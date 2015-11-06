package com.zorroa.archivist.service;

import com.zorroa.archivist.FileUtils;
import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ProcessorException;
import com.zorroa.archivist.sdk.processor.export.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by chambers on 11/4/15.
 */

public class ExportGraph {

    private static final Logger logger = LoggerFactory.getLogger(ExportGraph.class);

    @Autowired
    ApplicationContext applicationContext;

    @Value("${archivist.export.workspacePath}")
    String workspacePath;

    /**
     * Each graph has a single ExportInput processor which defined at
     * runtime and filled in by the export executor.  This serves as the
     * "root" node.
     */
    private AssetIterator graphIterator;

    private Export export;

    public ExportGraph(Export export, AssetIterator graphIterator) {
        this.graphIterator = graphIterator;
        this.export = export;
    }

    /**
     * Execute the graph from the given processor.
     *
     * @param processor
     */
    public void execute(ExportProcessor processor) {
        autowire(graphIterator);

        LinkedHashSet<ExportProcessor> visited = new LinkedHashSet<>();
        Queue<ExportProcessor> queue = new LinkedBlockingQueue<>();
        queue.add(processor);
        findExecutionPath(visited, queue);

        try {
            for (ExportProcessor p: visited) {
                p.init();
            }
        } catch (Exception e) {
            throw new ProcessorException(e.getMessage(), e);
        }

        for (Asset asset : graphIterator.getIterator()) {
            Iterator<ExportProcessor> iter = new LinkedList<>(visited).descendingIterator();
            while (iter.hasNext()) {
                ExportProcessor p = iter.next();
                p.getPorts(Port.Type.Output).forEach(port->port.reset());
                try {
                    p.execute(asset, export, makeOutputDirectory(asset, p));
                } catch (Exception e) {
                    logger.error("Failed to process asset {}", asset, e);
                    /*
                     * Maybe the asset is broken, so break from here and try
                     * the next asset.
                     */
                    break;
                }
            }
        }

        visited.forEach(p->p.teardown());
    }

    /**
     *
     * Do a breadth first walk of the connected Processors and return
     * the the path.
     *
     * @param visited
     * @param toVisit
     * @return
     */
    private LinkedHashSet<ExportProcessor> findExecutionPath(
            LinkedHashSet<ExportProcessor> visited, Queue<ExportProcessor> toVisit) {

        ExportProcessor processor = toVisit.poll();
        if (processor == null) {
            return visited;
        }

        if (!visited.contains(processor)) {
            visited.add(processor);
            autowire(processor);

            for (Port p : processor.getPorts(Port.Type.Input)) {
                for (Object o : p.getSockets()) {
                    Socket s = (Socket) o;
                    if (s.getCord() == null) {
                        continue;
                    }
                    toVisit.add(s.getCord().getPort().getParent());
                }
            }
        }

        findExecutionPath(visited, toVisit);
        return visited;
    }

    public String makeOutputDirectory(Asset asset, ExportProcessor processor) {
        String outputPath = String.format("%s/%d/%s/%s",
                workspacePath, export.getId(), asset.getId(), processor.getName());
        FileUtils.makedirs(outputPath);
        return outputPath;
    }

    private void autowire(Object p) {
        AutowireCapableBeanFactory autowire = applicationContext.getAutowireCapableBeanFactory();
        autowire.autowireBean(p);
    }
}
