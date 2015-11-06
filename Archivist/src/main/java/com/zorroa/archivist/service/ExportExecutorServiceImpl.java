package com.zorroa.archivist.service;

import com.zorroa.archivist.processors.export.AssetSearchGraphIterator;
import com.zorroa.archivist.sdk.domain.AssetSearchBuilder;
import com.zorroa.archivist.sdk.domain.Export;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Created by chambers on 11/2/15.
 */
@Configuration
public class ExportExecutorServiceImpl implements ExportExecutorService {


    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ExportGraph getExportGraph() {

        AssetSearchBuilder search = new AssetSearchBuilder();
        search.setQuery("beer");

        Export export = new Export();
        export.setId(1);

        ExportGraph graph = new ExportGraph(export, new AssetSearchGraphIterator(search));
        return graph;
    }

}
