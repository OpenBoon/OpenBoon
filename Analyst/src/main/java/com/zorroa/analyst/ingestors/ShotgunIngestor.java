package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.Argument;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.shotgun.Shotgun;

/**
 * Created by chambers on 3/24/16.
 */
public class ShotgunIngestor extends IngestProcessor {

    private Shotgun shotgun;

    @Argument
    private String server = "https://zorroa.shotgunstudio.com";

    @Argument
    private String script = "Archivist";

    @Argument
    private String key = "9b79ae646eab8304a5a9133c7d470871a800644f9fd36aa026c996d39b78ea70";

    @Override
    public void init() {
        shotgun = new Shotgun(server, script, key);
    }

    @Override
    public void process(AssetBuilder assetBuilder) {










    }
}
