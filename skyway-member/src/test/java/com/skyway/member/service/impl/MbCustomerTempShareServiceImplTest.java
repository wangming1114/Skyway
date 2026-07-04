package com.skyway.member.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.skyway.member.domain.MbCustomer;
import com.skyway.member.domain.MbCustomerTempShare;
import com.skyway.member.mapper.MbCustomerMapper;
import com.skyway.member.mapper.MbCustomerTempShareMapper;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.service.IProxyNodeService;

@ExtendWith(MockitoExtension.class)
public class MbCustomerTempShareServiceImplTest {

    @Mock
    private MbCustomerTempShareMapper tempShareMapper;

    @Mock
    private MbCustomerMapper customerMapper;

    @Mock
    private IProxyNodeService proxyNodeService;

    @InjectMocks
    private MbCustomerTempShareServiceImpl service;

    @Test
    public void createEncryptsPasswordAndGeneratesTokenForExistingCustomer() {
        MbCustomer customer = new MbCustomer();
        customer.setId(8L);
        when(customerMapper.selectById(8L)).thenReturn(customer);
        when(tempShareMapper.insert(any(MbCustomerTempShare.class))).thenReturn(1);

        Date expireTime = tomorrow();
        MbCustomerTempShare created = service.create(8L, "plain-pass", expireTime, "admin");

        assertEquals(8L, created.getCustomerId());
        assertEquals(expireTime, created.getExpireTime());
        assertEquals("0", created.getStatus());
        assertEquals("admin", created.getCreateBy());
        assertTrue(created.getToken().length() >= 32);
        assertFalse("plain-pass".equals(created.getAccessPassword()));
        assertTrue(new BCryptPasswordEncoder().matches("plain-pass", created.getAccessPassword()));
        verify(tempShareMapper).insert(any(MbCustomerTempShare.class));
    }

    @Test
    public void createRejectsMissingCustomer() {
        when(customerMapper.selectById(404L)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(404L, "plain-pass", tomorrow(), "admin"));

        assertEquals("客户不存在", ex.getMessage());
    }

    @Test
    public void unlockRejectsWrongPassword() {
        MbCustomerTempShare share = activeShare();
        share.setAccessPassword(new BCryptPasswordEncoder().encode("right-pass"));
        when(tempShareMapper.selectByToken("token-1")).thenReturn(share);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.unlock("token-1", "wrong-pass"));

        assertEquals("访问密码错误", ex.getMessage());
    }

    @Test
    public void unlockRejectsExpiredShare() {
        MbCustomerTempShare share = activeShare();
        share.setExpireTime(yesterday());
        when(tempShareMapper.selectByToken("token-1")).thenReturn(share);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.unlock("token-1", "right-pass"));

        assertEquals("临时访问已过期", ex.getMessage());
    }

    @Test
    public void unlockReturnsReadonlyCustomerNodes() {
        MbCustomerTempShare share = activeShare();
        share.setAccessPassword(new BCryptPasswordEncoder().encode("right-pass"));
        ProxyNode queryResult = new ProxyNode();
        queryResult.setId(10L);
        queryResult.setCustomerId(8L);
        queryResult.setStatus("0");
        queryResult.setUrl("vless://example");
        queryResult.setConfigJson("{\"secret\":\"value\"}");
        queryResult.setRemark("internal note");
        when(tempShareMapper.selectByToken("token-1")).thenReturn(share);
        when(proxyNodeService.selectList(any(ProxyNode.class))).thenReturn(Arrays.asList(queryResult));

        List<ProxyNode> nodes = service.unlock("token-1", "right-pass");

        assertEquals(1, nodes.size());
        assertEquals("vless://example", nodes.get(0).getUrl());
        assertEquals(null, nodes.get(0).getConfigJson());
        assertEquals(null, nodes.get(0).getRemark());
        ArgumentCaptor<ProxyNode> queryCaptor = ArgumentCaptor.forClass(ProxyNode.class);
        verify(proxyNodeService).selectList(queryCaptor.capture());
        assertEquals(8L, queryCaptor.getValue().getCustomerId());
        assertEquals("unexpired", queryCaptor.getValue().getExpireStatus());
        assertEquals("0", queryCaptor.getValue().getStatus());
    }

    private MbCustomerTempShare activeShare() {
        MbCustomerTempShare share = new MbCustomerTempShare();
        share.setId(1L);
        share.setCustomerId(8L);
        share.setToken("token-1");
        share.setStatus("0");
        share.setExpireTime(tomorrow());
        return share;
    }

    private Date tomorrow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    private Date yesterday() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return calendar.getTime();
    }
}
