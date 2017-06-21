package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.Taxonomy;
import com.zorroa.archivist.domain.TaxonomySpec;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 6/17/17.
 */
public interface TaxonomyService {
    Taxonomy createTaxonomy(TaxonomySpec spec);

    Taxonomy getTaxonomy(int id);

    Taxonomy getTaxonomy(Folder folder);

    void runAllAsync();

    void runAll();

    void tagTaxonomyAsync(Taxonomy tax, Folder start, boolean force);

    Map<String, Long> tagTaxonomy(Taxonomy tax, Folder start, boolean force);

    void untagTaxonomy(Taxonomy tax, List<Integer> folders, long updatedTime);
}
