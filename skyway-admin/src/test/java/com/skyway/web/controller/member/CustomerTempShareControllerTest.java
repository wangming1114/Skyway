package com.skyway.web.controller.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.common.core.domain.entity.SysUser;
import com.skyway.common.core.domain.model.LoginUser;
import com.skyway.member.domain.MbCustomerTempShare;
import com.skyway.member.service.IMbCustomerTempShareService;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.web.domain.member.CustomerTempShareCreateBody;
import com.skyway.web.domain.member.CustomerTempShareUnlockBody;

@ExtendWith(MockitoExtension.class)
public class CustomerTempShareControllerTest {

    @Mock
    private IMbCustomerTempShareService tempShareService;

    @InjectMocks
    private CustomerTempShareController controller;

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void createReturnsGeneratedShare() {
        loginAs("admin");
        MbCustomerTempShare share = new MbCustomerTempShare();
        share.setId(1L);
        share.setCustomerId(8L);
        share.setToken("share-token");
        share.setExpireTime(tomorrow());
        when(tempShareService.create(any(), any(), any(), any())).thenReturn(share);
        CustomerTempShareCreateBody body = new CustomerTempShareCreateBody();
        body.setAccessPassword("pass");
        body.setExpireTime(tomorrow());

        AjaxResult result = controller.create(8L, body);
        MbCustomerTempShare data = (MbCustomerTempShare) result.get("data");

        assertEquals(200, result.get("code"));
        assertEquals("share-token", data.getToken());
        assertEquals(8L, data.getCustomerId());
    }

    @Test
    public void unlockReturnsReadonlyNodes() {
        ProxyNode node = new ProxyNode();
        node.setId(10L);
        node.setUrl("vless://example");
        when(tempShareService.unlock("share-token", "pass")).thenReturn(Arrays.asList(node));
        CustomerTempShareUnlockBody body = new CustomerTempShareUnlockBody();
        body.setAccessPassword("pass");

        AjaxResult result = controller.unlock("share-token", body);

        assertEquals(200, result.get("code"));
        assertEquals(Arrays.asList(node), result.get("data"));
    }

    @Test
    public void unlockReturnsErrorWhenPasswordInvalid() {
        when(tempShareService.unlock("share-token", "bad")).thenThrow(new IllegalArgumentException("访问密码错误"));
        CustomerTempShareUnlockBody body = new CustomerTempShareUnlockBody();
        body.setAccessPassword("bad");

        AjaxResult result = controller.unlock("share-token", body);

        assertEquals(500, result.get("code"));
        assertEquals("访问密码错误", result.get("msg"));
    }

    private Date tomorrow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    private void loginAs(String username) {
        SysUser user = new SysUser();
        user.setUserName(username);
        LoginUser loginUser = new LoginUser(1L, 1L, user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null));
    }
}
