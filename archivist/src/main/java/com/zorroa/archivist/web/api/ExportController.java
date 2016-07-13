package com.zorroa.archivist.web.api;

import com.zorroa.archivist.domain.ExportSpec;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;

/**
 * Created by chambers on 7/11/16.
 */
public class ExportController {

    @RequestMapping(value="/api/v1/exports", method= RequestMethod.POST)
    public Object create(@RequestBody ExportSpec spec) throws IOException {
        return null;
    }
}
