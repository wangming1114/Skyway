package com.skyway.web.controller.resource;

import com.skyway.common.annotation.Anonymous;
import com.skyway.common.core.controller.BaseController;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.resource.domain.*;
import com.skyway.web.service.ws.WsNodeService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class WsNodeController extends BaseController {
    private final WsNodeService nodes;

    public WsNodeController(WsNodeService nodes) {
        this.nodes = nodes;
    }

    @PreAuthorize("@ss.hasPermi('resource:vps:add') or @ss.hasPermi('resource:vps:edit')")
    @GetMapping("/resource/vps/proxyNode/wsOperations/{id}")
    public AjaxResult status(@PathVariable String id) {
        return success(nodes.status(id, getUsername()));
    }

    @Anonymous
    @GetMapping("/share/subscription/{token}/clash")
    public ResponseEntity<String> subscription(@PathVariable String token) {
        ProxyNode node = nodes.subscription(token);
        if (node == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .cacheControl(CacheControl.noStore())
                    .build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("text/yaml;charset=UTF-8"))
                .body(VlessWsTls.mihomo(node));
    }
}
