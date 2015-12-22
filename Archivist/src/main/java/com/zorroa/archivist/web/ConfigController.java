package com.zorroa.archivist.web;

import com.zorroa.archivist.sdk.util.IngestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by chambers on 12/22/15.
 */
@RestController
public class ConfigController {

    @RequestMapping(value = "/api/v1/config/supported_formats", method = RequestMethod.GET)
    public Object supportedFormats() {
        return IngestUtils.SUPPORTED_FORMATS;
    }
}
