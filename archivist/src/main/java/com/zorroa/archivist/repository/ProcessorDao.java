package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Plugin;
import com.zorroa.archivist.domain.Processor;
import com.zorroa.archivist.domain.ProcessorFilter;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.plugins.ProcessorSpec;
import com.zorroa.sdk.processor.ProcessorRef;

import java.util.List;

/**
 * Created by chambers on 8/16/16.
 */
public interface ProcessorDao {
    boolean exists(String name);

    Processor create(Plugin plugin, ProcessorSpec spec);

    Processor get(String name);

    Processor get(int id);

    Processor refresh(Processor object);

    List<Processor> getAll();

    List<Processor> getAll(ProcessorFilter filter);

    List<Processor> getAll(Plugin plugin);

    PagedList<Processor> getAll(Paging page);

    boolean update(int id, Processor spec);

    boolean delete(int id);

    long count();

    ProcessorRef getRef(String name);
}
