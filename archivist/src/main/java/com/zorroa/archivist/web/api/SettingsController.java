package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Created by chambers on 5/31/17.
 */
@PreAuthorize("hasAuthority('group::developer') || hasAuthority('group::administrator')")
@RestController
public class SettingsController {

    @Autowired
    SettingsService settingsService;

    @RequestMapping(value="/api/v1/settings", method= RequestMethod.GET)
    public Map<String, String> getAll() {
        return settingsService.getAll();
    }

    @RequestMapping(value="/api/v1/settings", method= RequestMethod.POST)
    public Object set(@RequestBody Map<String, Object> settings) {
        settingsService.setAll(settings);
        return HttpUtils.status("settings", "update", true);
    }
}
