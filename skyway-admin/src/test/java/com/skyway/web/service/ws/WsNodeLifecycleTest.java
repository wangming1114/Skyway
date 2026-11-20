package com.skyway.web.service.ws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.VlessWsTls;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.resource.mapper.ProxyNodeMapper;
import com.skyway.resource.service.*;
import com.skyway.resource.service.impl.ProxyNodeServiceImpl;
import com.skyway.web.controller.resource.WsNodeController;
import com.skyway.web.service.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.schmizz.sshj.SSHClient;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

public class WsNodeLifecycleTest {
    private WsStore store;
    private WsNodeService service;
    private JdbcTemplate db;
    private final CloudflareDns dns = mock(CloudflareDns.class);
    private final WsRemote remote = mock(WsRemote.class);
    private final WsCertificates certs = mock(WsCertificates.class);
    private final IVpsInstanceService instances = mock(IVpsInstanceService.class);
    private final VpsPortAvailabilityService ports = mock(VpsPortAvailabilityService.class);
    private final IProxyNodeRateLimitService limits = mock(IProxyNodeRateLimitService.class);
    private final IProxyNodeTrafficService traffic = mock(IProxyNodeTrafficService.class);
    private VpsInstance vps;

    @BeforeEach
    void start() throws Exception {
        // Exercise the same subclass proxy used for the application's Spring repository.
        ProxyFactory repository = new ProxyFactory(memoryStore());
        repository.setProxyTargetClass(true);
        store = (WsStore) repository.getProxy();
        db = store.jdbc();
        vps = new VpsInstance();
        vps.setIp("192.0.2.10");
        when(instances.getById(7L)).thenReturn(vps);
        when(dns.root()).thenReturn("example.test");
        when(dns.zone()).thenReturn("example-zone");
        when(dns.apply(any(), anyString(), anyBoolean()))
                .thenAnswer(call -> ((WsStore.Domain) call.getArgument(0)).id);
        when(remote.connect(7L)).thenReturn(mock(SSHClient.class));
        AtomicInteger port = new AtomicInteger(12000);
        when(ports.resolveAutoPortForCreate(eq(7L), isNull()))
                .thenAnswer(call -> port.incrementAndGet());
        when(certs.ensure(any(), any())).thenReturn(Instant.now().plusSeconds(86400));
        service = worker();
    }

    static WsStore memoryStore() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL(
                "jdbc:h2:mem:"
                        + WsStore.uuid()
                        + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        JdbcTemplate db = new JdbcTemplate(source);
        db.execute(
                "CREATE TABLE res_ws_operation(id varchar PRIMARY KEY, sequence_no bigint auto_increment unique,"
                        + "instance_id bigint,node_id bigint,actor varchar,kind varchar,request_json varchar,context_json varchar,state varchar,"
                        + "stage varchar,attempts int default 0,next_run datetime,created_at datetime,reserved_port int,unique(instance_id,reserved_port))");
        db.execute(
                "CREATE TABLE res_ws_domain(id varchar PRIMARY KEY, node_key varchar,instance_id bigint,operation_id varchar unique,"
                        + "hostname varchar unique,zone_id varchar,record_id varchar,origin varchar,proxied boolean,state varchar,renew_at datetime)");
        db.execute(
                "CREATE TABLE res_proxy_node(id bigint auto_increment PRIMARY KEY,instance_id bigint,customer_id bigint,node_name varchar,"
                        + "node_type varchar,address varchar,port int,listen_port int,ws_key varchar unique,subscription_token varchar unique,"
                        + "url varchar,config_json varchar,domain_policy_json varchar,expire_time datetime,custom_id varchar,status varchar,"
                        + "remark varchar,create_by varchar,create_time datetime,update_by varchar,update_time datetime)");
        db.execute(
                "CREATE TABLE res_proxy_node_traffic(node_id bigint,rx_delta bigint,tx_delta bigint)");
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setMapperLocations(new ClassPathResource("mapper/resource/ProxyNodeMapper.xml"));
        ProxyNodeServiceImpl nodes = new ProxyNodeServiceImpl();
        ReflectionTestUtils.setField(
                nodes,
                "proxyNodeMapper",
                new SqlSessionTemplate(factory.getObject()).getMapper(ProxyNodeMapper.class));
        return new MemoryStore(
                db, new TransactionTemplate(new DataSourceTransactionManager(source)), nodes);
    }

    static class MemoryStore extends WsStore {
        MemoryStore(JdbcTemplate db, TransactionTemplate tx, ProxyNodeServiceImpl nodes) {
            super(db, tx, nodes);
        }

        @Override
        synchronized void withInstanceLock(long instance, Work work) {
            work.run();
        }
    }

    private WsNodeService worker() {
        return new WsNodeService(
                store,
                dns,
                remote,
                certs,
                instances,
                ports,
                new VpsInstanceOperationCoordinator(),
                new ProxyDomainWhitelistService(),
                traffic,
                limits);
    }

    @AfterEach
    void close() {
        service.close();
        db.execute("SHUTDOWN");
    }

    private WsStore.Operation await(String id, String state) throws InterruptedException {
        for (int i = 0; i < 240; i++) {
            WsStore.Operation op = store.operation(id);
            if (state.equals(op.state)) return op;
            if ("FAILED".equals(op.state)) fail(op.stage);
            Thread.sleep(20);
        }
        WsStore.Operation op = store.operation(id);
        throw new AssertionError(op.state + ": " + op.stage);
    }

    private String submit(String kind, Long node, JSONObject req) {
        return service.submit(kind, 7, node, req, "admin").getString("operationId");
    }

    private ProxyNode complete(String kind, Long node, JSONObject req) throws InterruptedException {
        return store.nodeService().getById(await(submit(kind, node, req), "DONE").node);
    }

    @Test
    void retriesAndDomainChangesKeepOneNodeAndStableSubscriptionUntilTransferred()
            throws Exception {
        JSONObject req =
                JSONObject.of(
                        "customerId", 1L, "operationId", WsStore.uuid(), "cloudflareProxied", true);
        ProxyNode a = complete("CREATE", null, req);
        ProxyNode duplicate = complete("CREATE", null, req);
        assertEquals(a.getId(), duplicate.getId());
        assertEquals(1, store.nodeService().selectList(new ProxyNode()).size());
        assertEquals(443, a.getPort().intValue());
        assertFalse(JSON.parseObject(a.getConfigJson()).getBooleanValue("cloudflareProxied"));
        String token = a.getSubscriptionToken();
        String identity = JSON.parseObject(a.getConfigJson()).getString("id");
        WsNodeController share = new WsNodeController(service);
        assertTrue(share.subscription(token).getBody().contains(a.getAddress()));
        db.update("INSERT INTO res_proxy_node_traffic VALUES (?,30,20)", a.getId());

        ProxyNode b =
                complete("CREATE", null, JSONObject.of("customerId", 1, "subdomain", "second"));
        assertNotEquals(a.getListenPort(), b.getListenPort());
        assertNotEquals(a.getInboundTag(), b.getInboundTag());
        assertEquals(a.getPort(), b.getPort());
        ProxyNode edited =
                complete(
                        "EDIT",
                        a.getId(),
                        JSONObject.of(
                                "subdomain", "renamed", "port", 8443, "cloudflareProxied", true));
        assertEquals(a.getSubscriptionPath(), edited.getSubscriptionPath());
        assertEquals(1, store.domains(a.getWsKey()).size());
        assertEquals(identity, JSON.parseObject(edited.getConfigJson()).getString("id"));
        assertEquals(a.getListenPort(), edited.getListenPort());
        assertEquals(50, edited.getTotalTrafficBytes().longValue());
        assertTrue(share.subscription(token).getBody().contains("renamed.example.test"));
        assertTrue(share.subscription(token).getBody().contains("port: 8443"));
        verify(dns).remove(argThat(d -> d.host.equals(a.getAddress())));

        complete("EDIT", a.getId(), JSONObject.of("status", "1"));
        assertEquals(404, share.subscription(token).getStatusCodeValue());
        assertEquals(1, store.domains(a.getWsKey()).size());
        complete("EDIT", a.getId(), JSONObject.of("status", "0"));
        assertEquals(200, share.subscription(token).getStatusCodeValue());
        ProxyNode transferred = complete("EDIT", a.getId(), JSONObject.of("customerId", 2));
        assertEquals(404, share.subscription(token).getStatusCodeValue());
        assertEquals(
                200, share.subscription(transferred.getSubscriptionToken()).getStatusCodeValue());
        verifyNoInteractions(limits);

        when(remote.connect(7L)).thenThrow(new IOException("offline"));
        doThrow(new IOException("DNS timeout"))
                .doNothing()
                .when(dns)
                .remove(argThat(d -> d.host.equals(transferred.getAddress())));
        assertThrows(IllegalStateException.class, () -> service.delete(transferred, true, "admin"));
        assertNotNull(store.domains(a.getWsKey()).get(0).record);
        assertEquals("DELETING", store.domains(a.getWsKey()).get(0).state);
        assertTrue(store.nodeService().getById(a.getId()).getCleanupStage().contains("DNS"));
        assertEquals(
                404, share.subscription(transferred.getSubscriptionToken()).getStatusCodeValue());
        service.close();
        service = worker();
        db.update("UPDATE res_ws_operation SET next_run=NOW() WHERE state='RETRY'");
        service.recover();
        await(WsNodeService.stableId("FORCE:" + a.getWsKey()), "DONE");
        assertTrue(store.domains(a.getWsKey()).isEmpty());
        assertEquals(200, share.subscription(b.getSubscriptionToken()).getStatusCodeValue());
        service.forceVps(7, "admin");
        assertTrue(store.domains(b.getWsKey()).isEmpty());
        verify(instances).deleteById(7L);
    }

    @Test
    void nameConflictAndCertificateFailureCleanOnlyTheirOwnAllocations() throws Exception {
        doThrow(new CloudflareDns.Conflict())
                .when(dns)
                .apply(
                        argThat(d -> "taken.example.test".equals(d.host)),
                        anyString(),
                        anyBoolean());
        String failed =
                submit("CREATE", null, JSONObject.of("customerId", 1, "subdomain", "taken"));
        assertTrue(await(failed, "FAILED").stage.contains("域名"));
        ProxyNode good = complete("CREATE", null, JSONObject.of("customerId", 1));
        String original = VlessWsTls.mihomo(good);
        when(certs.ensure(any(), argThat(d -> "bad.example.test".equals(d.host))))
                .thenThrow(new IllegalArgumentException("证书签发失败"));
        String rename = submit("EDIT", good.getId(), JSONObject.of("subdomain", "bad"));
        await(rename, "FAILED");
        assertEquals(
                original, VlessWsTls.mihomo(service.subscription(good.getSubscriptionToken())));
        assertEquals(1, store.domains(good.getWsKey()).size());
        verify(dns).remove(argThat(d -> "bad.example.test".equals(d.host)));
    }

    @Test
    void missingConfiguredSignerFailsBeforeCreatingDnsOrTouchingTheVps() throws Exception {
        when(certs.signer()).thenThrow(new IllegalArgumentException("管理端找不到指定的证书工具"));
        String id = submit("CREATE", null, JSONObject.of("customerId", 1, "subdomain", "fresh"));
        assertTrue(await(id, "FAILED").stage.contains("证书工具"));
        verify(dns, never()).apply(any(), anyString(), anyBoolean());
        verify(remote, never()).connect(anyLong());
        assertTrue(store.domains(id).isEmpty());
    }

    @Test
    void automaticNameConflictIsRetriedAndAddressMaintenanceKeepsBothRecords() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(
                        call -> {
                            if (attempts.getAndIncrement() == 0) throw new CloudflareDns.Conflict();
                            return ((WsStore.Domain) call.getArgument(0)).id;
                        })
                .when(dns)
                .apply(any(), anyString(), anyBoolean());
        ProxyNode a = complete("CREATE", null, JSONObject.of("customerId", 1));
        ProxyNode b = complete("CREATE", null, JSONObject.of("customerId", 2));
        assertEquals(3, attempts.get());
        String privatePort = submit("EDIT", b.getId(), JSONObject.of("port", a.getListenPort()));
        assertTrue(await(privatePort, "FAILED").stage.contains("预留"));
        complete("EDIT", a.getId(), JSONObject.of("cloudflareProxied", true));
        String badPort =
                submit("EDIT", a.getId(), JSONObject.of("port", 10001, "cloudflareProxied", true));
        assertTrue(await(badPort, "FAILED").stage.contains("橙云"));
        vps.setIp("2001:db8::5");
        service.syncAddress(7);
        for (String id :
                db.queryForList(
                        "SELECT id FROM res_ws_operation WHERE kind='MAINTAIN'", String.class))
            await(id, "DONE");
        for (ProxyNode n : Arrays.asList(a, b)) {
            WsStore.Domain binding = store.domains(n.getWsKey()).get(0);
            assertEquals(CloudflareDns.origin(vps.getIp()), binding.origin);
            assertEquals(n.getId().equals(a.getId()), binding.proxied);
            assertNotNull(binding.record);
        }
    }
}
