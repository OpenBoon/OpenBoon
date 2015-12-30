package com.zorroa.archivist.processors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Ingest;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;

public class AggregatorIngestor extends IngestProcessor {
    private int assetsSinceLastAgg = 0;
    private final int kAggCount = 128;
    private ArrayList<IngestProcessor> aggregators = new ArrayList<>();

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    protected Client client;

    @Value("${archivist.index.alias}")
    protected String alias;

    @Override
    public void init(Ingest ingest) {
        aggregators.clear();                            // Not sure this is needed?
        aggregators.add(new DateAggregator());
        aggregators.add(new RatingAggregator());
        aggregators.add(new IngestPathAggregator());
        AutowireCapableBeanFactory autowire = applicationContext.getAutowireCapableBeanFactory();
        for (IngestProcessor aggregator : aggregators) {
            autowire.autowireBean(aggregator);
            aggregator.init(ingest);
        }
    }

    @Override
    public void process(AssetBuilder assetBuilder) {
        if (++assetsSinceLastAgg > kAggCount) {
            assetsSinceLastAgg = 0;
            aggregate();
        }
    }

    @Override
    public void teardown() {
        if (assetsSinceLastAgg > 0) {
            aggregate();
        }
    }

    private void aggregate() {
        refreshIndex(1);    // Wait for ES to settle after ingestion
        for (IngestProcessor agg: aggregators) {
            agg.process(null);
        }
    }

    private void refreshIndex(long sleep) {
        try {
            Thread.sleep(sleep/2);
        } catch (InterruptedException e) {
        }
        client.admin().indices().prepareRefresh(alias).get();
        try {
            Thread.sleep(sleep/2);
        } catch (InterruptedException e) {
        }
    }

}
