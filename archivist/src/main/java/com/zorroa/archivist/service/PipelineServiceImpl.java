package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.PipelineDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.ProcessorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * The ingest service is responsible for processing IngestRequest objects which
 * results in the creation of assets using the assetService.
 *
 * @author chambers
 *
 */
@Service
@Transactional
public class PipelineServiceImpl implements PipelineService {

    private static final Logger logger = LoggerFactory.getLogger(PipelineServiceImpl.class);

    @Autowired
    PluginService pluginService;

    @Autowired
    PipelineDao pipelineDao;

    @Autowired
    TransactionEventManager event;

    @Autowired
    EventLogService logService;

    @Override
    public Pipeline create(PipelineSpecV spec) {
        /**
         * Each processor needs to be validated.
         */
        spec.setProcessors(validateProcessors(spec.getType(), spec.getProcessors()));

        Pipeline p = pipelineDao.create(spec);
        event.afterCommit(()-> {
            if (SecurityUtils.getAuthentication() != null) {
                logService.logAsync(UserLogSpec.build(LogAction.Create, p));
            }
        });
        return p;
    }

    @Override
    public Pipeline get(int id) {
        return pipelineDao.get(id);
    }

    @Override
    public Pipeline get(String name) {
        return pipelineDao.get(name);
    }

    @Override
    public Pipeline getStandard(PipelineType type) {
        return pipelineDao.getStandard(type);
    }

    @Override
    public boolean exists(String name) {
        return pipelineDao.exists(name);
    }

    @Override
    public List<Pipeline> getAll() {
        return pipelineDao.getAll();
    }

    @Override
    public PagedList<Pipeline> getAll(Pager page) {
        return pipelineDao.getAll(page);
    }

    @Override
    public boolean update(int id, Pipeline spec) {
        Pipeline pl = pipelineDao.get(id);

        /**
         * TODO: recursively validate all processors.
         */
        List<ProcessorRef> validated = validateProcessors(pl.getType(), spec.getProcessors());
        spec.setProcessors(validated);

        boolean result = pipelineDao.update(id, spec);
        if (result) {
            event.afterCommit(() -> {
                logService.logAsync(UserLogSpec.build(LogAction.Update, "pipeline", id));
            });
        }
        return result;
    }

    @Override
    public boolean delete(int id) {
        boolean result = pipelineDao.delete(id);
        if (result) {
            event.afterCommit(() -> {
                logService.logAsync(UserLogSpec.build(LogAction.Delete, "pipeline", id));
            });
        }
        return result;
    }

    /**
     * The types of processors that can be found in each pipeline type.
     */
    private static final Map<PipelineType, Set<String>> ALLOWED_TYPES = ImmutableMap.of(
            PipelineType.Generate, ImmutableSet.of("Generate", "Common"),
            PipelineType.Import, ImmutableSet.of("Import", "Common"),
            PipelineType.Export, ImmutableSet.of("Export", "Common"),
            PipelineType.Batch, ImmutableSet.of("Batch", "Common"),
            PipelineType.Training, ImmutableSet.of("Training", "Common"));

    @Override
    public List<ProcessorRef> validateProcessors(PipelineType pipelineType, List<ProcessorRef> refs) {

        List<ProcessorRef> validated = Lists.newArrayList();

        for (ProcessorRef ref: refs) {
            ProcessorRef vref = pluginService.getProcessorRef(ref);
            if (!ALLOWED_TYPES.getOrDefault(pipelineType, ImmutableSet.of()).contains(vref.getType())) {
                throw new IllegalStateException("Cannot have processor type " +
                        vref.getType() + " in a " + pipelineType + " pipeline");
            }
            validated.add(vref);
            if (ref.getExecute() != null) {
                vref.setExecute(validateProcessors(PipelineType.Import, ref.getExecute()));
            }
        }
        return validated;
    }

    @Override
    public List<ProcessorRef> mungePipelines(PipelineType type, List<ProcessorRef> procs) {
        List<ProcessorRef> result = Lists.newArrayListWithCapacity(8);

        if (procs != null) {
            result.addAll(pluginService.getProcessorRefs(procs));
        }

        if (result.isEmpty()) {
            try {
                Pipeline p = getStandard(type);
                result.addAll(pluginService.getProcessorRefs(p.getId()));
            } catch (EmptyResultDataAccessException e) {
                // ignore the fact there is no standard.
            }
        }
        return result;
    }

    /**
     * Return true of the Object is a valid pipeline identifier, which is
     * a number > 0 or a string.
     *
     * @param value
     * @return
     */
    @Override
    public boolean isValidPipelineId(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Number) {
            return ((Integer)value) > 0;
        }

        if (value instanceof String) {
            return true;
        }

        return false;
    }
}
