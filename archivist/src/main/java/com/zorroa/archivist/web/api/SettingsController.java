package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.Setting;
import com.zorroa.archivist.domain.SettingsFilter;
import com.zorroa.archivist.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 5/31/17.
 */

@RestController
public class SettingsController {

    @Autowired
    SettingsService settingsService;

    @RequestMapping(value="/api/v1/settings", method= RequestMethod.GET)
    public List<Setting> getAll(@RequestBody(required = false) SettingsFilter filter) {
        if (filter == null) {
            filter = new SettingsFilter();
        }
        return settingsService.getAll(filter);
    }

    @RequestMapping(value="/api/v1/settings/{name:.+}", method= RequestMethod.GET)
    public Setting get(@PathVariable String name) {
        return settingsService.get(name);
    }

    @PreAuthorize("hasAuthority('group::developer') || hasAuthority('group::administrator')")
    @RequestMapping(value="/api/v1/settings", method= RequestMethod.PUT)
    public Object set(@RequestBody Map<String, String> settings) {
        int count = settingsService.setAll(settings);
        return HttpUtils.status("settings", "update", count == settings.size());
    }
}
