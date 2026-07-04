package com.skyway.web.controller.member;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.skyway.common.annotation.Anonymous;
import com.skyway.common.annotation.Log;
import com.skyway.common.core.controller.BaseController;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.common.enums.BusinessType;
import com.skyway.member.service.IMbCustomerTempShareService;
import com.skyway.web.domain.member.CustomerTempShareCreateBody;
import com.skyway.web.domain.member.CustomerTempShareUnlockBody;

/**
 * 客户订阅临时访问
 */
@RestController
public class CustomerTempShareController extends BaseController {

    @Autowired
    private IMbCustomerTempShareService tempShareService;

    @PreAuthorize("@ss.hasPermi('member:customer:query')")
    @GetMapping("/member/customer/{customerId}/temp-shares")
    public AjaxResult list(@PathVariable Long customerId) {
        return success(tempShareService.listByCustomerId(customerId));
    }

    @PreAuthorize("@ss.hasPermi('member:customer:query')")
    @Log(title = "客户临时访问", businessType = BusinessType.INSERT)
    @PostMapping("/member/customer/{customerId}/temp-shares")
    public AjaxResult create(@PathVariable Long customerId, @RequestBody CustomerTempShareCreateBody body) {
        try {
            return success(tempShareService.create(customerId, body.getAccessPassword(), body.getExpireTime(), getUsername()));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('member:customer:query')")
    @Log(title = "客户临时访问", businessType = BusinessType.UPDATE)
    @DeleteMapping("/member/customer/temp-shares/{id}")
    public AjaxResult revoke(@PathVariable Long id) {
        try {
            return toAjax(tempShareService.revoke(id, getUsername()));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Anonymous
    @PostMapping("/share/customer/{token}/unlock")
    public AjaxResult unlock(@PathVariable String token, @RequestBody CustomerTempShareUnlockBody body) {
        try {
            return success(tempShareService.unlock(token, body.getAccessPassword()));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }
}
