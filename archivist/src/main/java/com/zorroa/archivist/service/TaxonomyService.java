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

    boolean delete(Taxonomy tax, boolean untag);

    Taxonomy create(TaxonomySpec spec);

    Taxonomy get(int id);

    Taxonomy get(Folder folder);

    void runAllAsync();

    void runAll();

    void tagTaxonomyAsync(Taxonomy tax, Folder start, boolean force);

    Map<String, Long> tagTaxonomy(Taxonomy tax, Folder start, boolean force);

    void untagTaxonomyAsync(Taxonomy tax, long updatedTime);

    void untagTaxonomyAsync(Taxonomy tax);

    void untagTaxonomyFoldersAsync(Taxonomy tax, List<Folder> folders);

    void untagTaxonomyFoldersAsync(Taxonomy tax, Folder folder, List<String> assets);

    void untagTaxonomyFolders(Taxonomy tax, Folder folder, List<String> assets);

    void untagTaxonomyFolders(Taxonomy tax, List<Folder> folders);

    Map<String, Long> untagTaxonomy(Taxonomy tax);

    Map<String, Long> untagTaxonomy(Taxonomy tax, long updatedTime);
}
