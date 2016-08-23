package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Plugin;
import com.zorroa.archivist.domain.Processor;
import com.zorroa.archivist.domain.ProcessorFilter;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.processor.ProcessorRef;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 6/28/16.
 */
public interface PluginService {

    Plugin installPlugin(MultipartFile file);

    Plugin installPlugin(Path zipFilePath);

    PagedList<Plugin> getAllPlugins(Paging page);
    List<Plugin> getAllPlugins();
    Plugin getPlugin(String name);
    Plugin getPlugin(int id);

    List<Processor> getAllProcessors(Plugin plugin);
    List<Processor> getAllProcessors();

    Processor getProcessor(int id);

    ProcessorRef getProcessorRef(String name, Map<String, Object> args);

    ProcessorRef getProcessorRef(ProcessorRef ref);

    List<Processor> getAllProcessors(ProcessorFilter filter);

    ProcessorRef getProcessorRef(String name);

    Processor getProcessor(String name);

    void registerAllPlugins();
}
