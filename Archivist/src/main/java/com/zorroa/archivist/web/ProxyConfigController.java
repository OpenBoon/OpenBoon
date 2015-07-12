package com.zorroa.archivist.web;

import java.util.List;

import com.zorroa.archivist.domain.ProxyConfigUpdateBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.service.ImageService;

@RestController
public class ProxyConfigController {

    @Autowired
    ImageService imageService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/proxy-configs", method=RequestMethod.GET)
    public List<ProxyConfig> getAll() {
        return imageService.getProxyConfigs();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/proxy-configs/{id}", method=RequestMethod.GET)
    public ProxyConfig get(@PathVariable String id) {
        return imageService.getProxyConfig(id);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/proxy-configs/{id}", method=RequestMethod.PUT)
    public ProxyConfig update(@RequestBody ProxyConfigUpdateBuilder builder, @PathVariable String id) {
        ProxyConfig cfg = imageService.getProxyConfig(Integer.parseInt(id));
        imageService.updateProxyConfig(cfg, builder);
        return imageService.getProxyConfig(cfg.getId());
    }
}
