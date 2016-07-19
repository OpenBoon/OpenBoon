package com.zorroa.common.repository;

import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.plugins.Module;

import java.util.List;

/**
 * Created by chambers on 6/29/16.
 */
public interface ModuleDao {

    List<Module> getAll(String plugin, String type);

    List<Module> getAll(String plugin);

    List<Module> getAll();

    PagedList<Module> getPaged(String plugin, Paging paging);

    Module get(String id);
}
