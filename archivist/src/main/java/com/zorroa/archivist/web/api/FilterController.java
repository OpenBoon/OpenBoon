package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.Filter;
import com.zorroa.archivist.domain.FilterSpec;
import com.zorroa.archivist.service.FilterService;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Created by chambers on 4/25/17.
 */
@PreAuthorize("hasAuthority('group::developer') || hasAuthority('group::administrator')")
@RestController
public class FilterController {

    @Autowired
    FilterService filterService;

    @RequestMapping(value = "/api/v1/filters", method = RequestMethod.POST)
    public Filter create(@Valid @RequestBody FilterSpec spec, BindingResult valid) {
        if (valid.hasErrors()) {
            throw new ArchivistWriteException(HttpUtils.getBindingErrorString(valid));
        }
        Filter filter = filterService.create(spec);
        return filter;
    }

    @RequestMapping(value = "/api/v1/filters", method = RequestMethod.GET)
    public List<Filter> getAll() {
        return filterService.getAll();
    }

    @RequestMapping(value = "/api/v1/filters/{id}", method = RequestMethod.GET)
    public Filter get(@PathVariable int id) {
        return filterService.get(id);
    }

    /**
     * This just disables the filter.
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/api/v1/filters/{id}", method = RequestMethod.DELETE)
    public Object delete(@PathVariable int id) {
        Filter filter = filterService.get(id);
        boolean result = filterService.setEnabled(filter, false);
        return HttpUtils.deleted("filters", id, result);
    }
}
