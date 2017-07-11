package com.zorroa.archivist.web.api;

import com.zorroa.archivist.domain.SharedLink;
import com.zorroa.archivist.domain.SharedLinkSpec;
import com.zorroa.archivist.service.SharedLinkService;
import com.zorroa.archivist.web.InvalidObjectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Created by chambers on 7/7/17.
 */

@RestController
public class SharedLinkController {

    @Autowired
    SharedLinkService sharedLinkService;

    @RequestMapping(value="/api/v1/shared_link", method= RequestMethod.POST)
    public SharedLink create(@Valid @RequestBody SharedLinkSpec spec, BindingResult valid) {
        if (valid.hasErrors()) {
            throw new InvalidObjectException("Failed to create shared link", valid);
        }
        return sharedLinkService.create(spec);
    }

    @RequestMapping(value="/api/v1/shared_link/{id}", method= RequestMethod.GET)
    public SharedLink create(@PathVariable int id) {
        return sharedLinkService.get(id);
    }
}
