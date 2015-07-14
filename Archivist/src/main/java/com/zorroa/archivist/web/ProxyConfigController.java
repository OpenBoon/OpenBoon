package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.domain.ProxyConfigBuilder;
import com.zorroa.archivist.domain.ProxyConfigUpdateBuilder;
import com.zorroa.archivist.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    @RequestMapping(value="/api/v1/proxy-configs", method=RequestMethod.POST)
    public ProxyConfig create(@RequestBody ProxyConfigBuilder builder) {
        return imageService.createProxyConfig(builder);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/proxy-configs/{id}", method=RequestMethod.PUT)
    public ProxyConfig update(@RequestBody ProxyConfigUpdateBuilder builder, @PathVariable String id) {
        ProxyConfig cfg = imageService.getProxyConfig(Integer.parseInt(id));
        imageService.updateProxyConfig(cfg, builder);
        return imageService.getProxyConfig(cfg.getId());
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/proxy-configs/{id}", method=RequestMethod.DELETE)
    public Map<String, Object> delete(@PathVariable Integer id) {
        ProxyConfig cfg = imageService.getProxyConfig(id);
        try {
            return ImmutableMap.<String, Object>builder()
                    .put("status", imageService.deleteProxyConfig(cfg))
                    .build();
        } catch (Exception e) {
            return ImmutableMap.<String, Object>builder()
                    .put("status", false)
                    .put("message", e.getMessage())
                    .build();
        }
    }
}
