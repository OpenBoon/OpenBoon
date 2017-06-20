package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.Taxonomy;
import com.zorroa.archivist.domain.TaxonomySpec;

/**
 * Created by chambers on 6/17/17.
 */
public interface TaxonomyDao extends GenericDao<Taxonomy, TaxonomySpec> {

    Taxonomy get(Folder folder);
}
