package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.Taxonomy;
import com.zorroa.archivist.domain.TaxonomySpec;
import com.zorroa.archivist.service.FolderService;
import com.zorroa.archivist.service.TaxonomyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by chambers on 6/20/17.
 */
@RestController
public class TaxonomyController {

    @Autowired
    TaxonomyService taxonomyService;

    @Autowired
    FolderService folderService;

    @ResponseBody
    @RequestMapping(value="/api/v1/taxonomy", method = RequestMethod.POST)
    public Object create(@RequestBody TaxonomySpec tspec) {
        return taxonomyService.createTaxonomy(tspec);
    }

    @ResponseBody
    @RequestMapping(value="/api/v1/taxonomy/{id}", method = RequestMethod.GET)
    public Taxonomy get(@PathVariable int id) {
        return taxonomyService.getTaxonomy(id);
    }

    @ResponseBody
    @RequestMapping(value="/api/v1/taxonomy/_folder/{id}", method = RequestMethod.GET)
    public Taxonomy getByFolder(@PathVariable int id) {
        Folder folder = folderService.get(id);
        return taxonomyService.getTaxonomy(folder);
    }

    @ResponseBody
    @RequestMapping(value="/api/v1/taxonomy/{id}", method = RequestMethod.DELETE)
    public Object delete(@PathVariable int id) {
        Taxonomy tax = taxonomyService.getTaxonomy(id);
        return HttpUtils.deleted("taxonomy", id, taxonomyService.deleteTaxonomy(tax));
    }
}
