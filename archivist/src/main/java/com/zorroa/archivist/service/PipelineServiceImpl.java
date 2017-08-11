package com.zorroa.archivist.service;

import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.LogAction;
import com.zorroa.archivist.domain.UserLogSpec;
import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpecV;
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
        List<ProcessorRef> validated = Lists.newArrayList();
        for (ProcessorRef ref: spec.getProcessors()) {
            validated.add(pluginService.getProcessorRef(ref));
        }
        spec.setProcessors(validated);

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
    public Pipeline getStandard() {
        return pipelineDao.getStandard();
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
        List<ProcessorRef> validated = Lists.newArrayList();
        for (ProcessorRef ref: spec.getProcessors()) {
            validated.add(pluginService.getProcessorRef(ref));
        }
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

    @Override
    public  List<ProcessorRef> getProcessors(Object pipelineId, List<ProcessorRef> custom) {
        List<ProcessorRef> result = Lists.newArrayListWithCapacity(12);

        if (JdbcUtils.isValid(custom)) {
            for (ProcessorRef ref: custom) {
                result.add(pluginService.getProcessorRef(ref));
            }
        }
        else if (pipelineId != null) {
            if (pipelineId instanceof Number) {
                Number pid = (Number) pipelineId;
                result.addAll(pluginService.getProcessorRefs(pid.intValue()));
            }
            else {
                Pipeline p = get((String) pipelineId);
                result.addAll(pluginService.getProcessorRefs(p.getId()));
            }
        }
        else {
            try {
                Pipeline p = getStandard();
                result.addAll(pluginService.getProcessorRefs(p.getId()));
            } catch (EmptyResultDataAccessException ignore) {
                // If there1 is no standard, its just an empty pipeline.
            }
        }
        return result;
    }

    @Override
    public List<ProcessorRef> mungePipelines(List<Object> pipelineIds, List<ProcessorRef> processors) {
        List<ProcessorRef> result = Lists.newArrayListWithCapacity(16);
        int count = 0;

        boolean processorsAdded = false;
        if (pipelineIds != null) {
            for (Object id : pipelineIds) {
                Pipeline pipeline;
                ProcessorRef ref = pluginService.getProcessorRef(
                        "com.zorroa.core.processor.GroupProcessor");
                result.add(ref);

                if (id instanceof Number) {
                    pipeline = get((int) id);
                    ref.setExecute(pipeline.getProcessors());
                    count+=ref.getExecute().size();
                } else if (!processorsAdded && id.equals("#") && processors != null) {
                    ref.setExecute(pluginService.getProcessorRefs(processors));
                    count+=ref.getExecute().size();
                    processorsAdded = true;
                } else {
                    pipeline = get((String) id);
                    ref.setExecute(pipeline.getProcessors());
                    count+=ref.getExecute().size();
                }
            }
        }

        if (!processorsAdded && processors != null) {
            ProcessorRef ref = pluginService.getProcessorRef(
                    "com.zorroa.core.processor.GroupProcessor");
            ref.setExecute(pluginService.getProcessorRefs(processors));
            result.add(ref);
            count+=ref.getExecute().size();
        }

        if (count == 0) {
            logger.warn("No processors specified, defaulting to standard");
            try {
                Pipeline p = getStandard();
                result.addAll(pluginService.getProcessorRefs(p.getId()));
            } catch (EmptyResultDataAccessException ignore) {
                // If there1 is no standard, its just an empty pipeline.
            }
        }

        logger.info("munged {} processors", count);
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
