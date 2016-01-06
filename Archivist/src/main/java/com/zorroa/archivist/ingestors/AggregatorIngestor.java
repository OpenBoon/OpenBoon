package com.zorroa.archivist.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Ingest;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.concurrent.atomic.LongAdder;

public class AggregatorIngestor extends IngestProcessor {
    /**
     * Using LongAdder here, its not safe to have multiple
     * threads incrementing a primitive integer.
     */
    private final LongAdder assetsSinceLastAgg = new LongAdder();

    /**
     * The number of assets that must be processed before aggregation occurs.
     */
    private final int kAggCount = 128;

    /**
     * The aggregators to run.
     */
    private final ArrayList<IngestProcessor> aggregators = new ArrayList<>();

    @Autowired
    ApplicationContext applicationContext;

    @Override
    public void init(Ingest ingest) {
        aggregators.add(new DateAggregator());
        aggregators.add(new IngestPathAggregator());
        AutowireCapableBeanFactory autowire = applicationContext.getAutowireCapableBeanFactory();
        for (IngestProcessor aggregator : aggregators) {
            autowire.autowireBean(aggregator);
            aggregator.init(ingest);
        }
    }

    @Override
    public void process(AssetBuilder assetBuilder) {
        assetsSinceLastAgg.increment();
        if (assetsSinceLastAgg.longValue() > kAggCount) {
            /*
             * Synchronize around the aggregators and double check
             * the condition to avoid double execution.
             */
            synchronized(aggregators) {
                if (assetsSinceLastAgg.longValue() > kAggCount) {
                    aggregate();
                    assetsSinceLastAgg.reset();
                }
            }
        }
    }

    @Override
    public void teardown() {
        if (assetsSinceLastAgg.longValue() > 0) {
            aggregate();
        }
    }

    private void aggregate() {
        /*
         * Refresh is run from the ingest executor, so the data should be available.
         */
        for (IngestProcessor agg: aggregators) {
            agg.process(null);
        }
    }
}
