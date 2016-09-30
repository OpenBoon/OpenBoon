package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.LogAction;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpecV;
import com.zorroa.archivist.repository.PipelineDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.sdk.domain.Message;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.ProcessorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    MessagingService message;

    @Autowired
    TransactionEventManager event;

    @Autowired
    LogService logService;

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
            message.broadcast(new Message("PIPELINE_CREATE",
                    ImmutableMap.of("id", p.getId())));
            if (SecurityUtils.getAuthentication() != null) {
                logService.log(LogSpec.build(LogAction.Create, p));
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
                message.broadcast(new Message("PIPELINE_UPDATE",
                        ImmutableMap.of("id", id)));
                logService.log(LogSpec.build(LogAction.Update, "pipeline", id));
            });
        }
        return result;
    }

    @Override
    public boolean delete(int id) {
        boolean result = pipelineDao.delete(id);
        if (result) {
            event.afterCommit(() -> {
                message.broadcast(new Message("PIPELINE_DELETE",
                        ImmutableMap.of("id", id)));
                logService.log(LogSpec.build(LogAction.Delete, "pipeline", id));
            });
        }
        return result;
    }

    @Override
    public  List<ProcessorRef> getProcessors(Object pipelineId, List<ProcessorRef> custom) {
        List<ProcessorRef> result = Lists.newArrayList();

        if (JdbcUtils.isValid(custom)) {
            for (ProcessorRef ref: custom) {
                result.add(pluginService.getProcessorRef(ref));
            }
        }
        else if (pipelineId != null) {
            if (pipelineId instanceof Number) {
                Number pid = (Number) pipelineId;
                for (ProcessorRef ref : get(pid.intValue()).getProcessors()) {
                    result.add(pluginService.getProcessorRef(ref));
                }
            }
            else {
                for (ProcessorRef ref: get((String)pipelineId).getProcessors()) {
                    result.add(pluginService.getProcessorRef(ref));
                }
            }
        }
        return result;
    }
}
