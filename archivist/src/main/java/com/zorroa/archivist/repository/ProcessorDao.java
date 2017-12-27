package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Plugin;
import com.zorroa.archivist.domain.Processor;
import com.zorroa.archivist.domain.ProcessorFilter;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.processor.ProcessorSpec;

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

    PagedList<Processor> getAll(Pager page);

    boolean update(int id, Processor spec);

    boolean delete(int id);

    boolean deleteAll(Plugin plugin);

    long count();

    ProcessorRef getRef(String name);
}
