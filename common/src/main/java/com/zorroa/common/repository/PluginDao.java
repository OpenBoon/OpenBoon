package com.zorroa.common.repository;

import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.plugins.Plugin;

import java.util.List;

/**
 * Created by chambers on 6/29/16.
 */
public interface PluginDao {
    List<Plugin> getAll();

    PagedList<List<Plugin>> getAll(Paging paging);

    Plugin get(String id);
}
