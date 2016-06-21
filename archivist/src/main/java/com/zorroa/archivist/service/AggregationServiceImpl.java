package com.zorroa.archivist.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zorroa.archivist.security.BackgroundTaskAuthentication;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.Aggregator;
import com.zorroa.sdk.processor.ProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by chambers on 6/21/16.
 */
@Service
public class AggregationServiceImpl implements AggregationService {

    private static final Logger logger = LoggerFactory.getLogger(AggregationServiceImpl.class);

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    IngestService ingestService;

    @Autowired
    AssetDao assetDao;

    @Autowired
    MessagingService messagingService;

    @Autowired
    UserService userService;

    private final LoadingCache<Ingest, List<Aggregator>> cache = CacheBuilder.newBuilder()
            .build(new CacheLoader<Ingest, List<Aggregator>>() {
                public List<Aggregator> load(Ingest ingest) throws Exception {
                    IngestPipeline pipeline = ingestService.getIngestPipeline(ingest.getPipelineId());
                    List<Aggregator> result = Lists.newArrayList();
                    for (ProcessorFactory<Aggregator> factory : pipeline.getAggregators()) {
                        Aggregator agg = (Aggregator) applicationContext.getBean(factory.getKlassName());
                        agg.init(ingest);
                        result.add(agg);
                    }
                    return result;
                }
            });

    private final Set<Integer> aggregating = Sets.newConcurrentHashSet();

    private final ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(1);

    @Override
    public void invalidate(Ingest ingest) {
        cache.invalidate(ingest);
    }

    @Override
    public void aggregate(Ingest ingest) {
        if (aggregating.add(ingest.getId())) {
            threadPoolExecutor.schedule(() -> {
                try {
                    User user = userService.get(ingest.getUserCreated());
                    SecurityContextHolder.getContext().setAuthentication(
                            authenticationManager.authenticate(new BackgroundTaskAuthentication(user)));
                    assetDao.refresh();
                    cache.get(ingest).forEach(Aggregator::aggregate);
                    messagingService.broadcast(new Message(MessageType.INGEST_AGGREGATE, ingest));
                } catch (ExecutionException e) {
                    logger.warn("aggregation failed: {}", e.getMessage(), e);
                }
                finally {
                    aggregating.remove(ingest.getId());
                }
            }, 5, TimeUnit.SECONDS);
        }
    }
}
