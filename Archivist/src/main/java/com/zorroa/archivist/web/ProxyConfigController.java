package com.zorroa.archivist.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.service.ImageService;

@RestController
public class ProxyConfigController {

    @Autowired
    ImageService imageService;

    public ProxyConfigController() {
        // TODO Auto-generated constructor stub
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/proxy-configs", method=RequestMethod.GET)
    public List<ProxyConfig> getAll() {
        return imageService.getProxyConfigs();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/proxy-configs/{id}", method=RequestMethod.GET)
    public ProxyConfig get(@PathVariable String id) {
        return imageService.getProxyConfig(id);
    }
}
