package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.Argument;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.util.FileUtils;
import com.zorroa.shotgun.SgRequest;
import com.zorroa.shotgun.Shotgun;

import java.util.Map;

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

        /*
         * Kinda just hard coding stuff in here for now.   This processes some assets but we'll also
         * need to process shots.
         */
        try {
            String name = FileUtils.basename(assetBuilder.getFilename()).split("_")[0];
            logger.debug("querying shotgun for: {}", name);
            Map<String, Object> item = shotgun.findOne(
                    new SgRequest("Asset").filter("code", "is", name)
                    .setFields("sg_asset_type", "id", "type", "code", "image", "sg_status_list", "shots"));
            assetBuilder.setAttr("shotgun", item);
        } catch (Exception e) {
            logger.warn("Cannot find shotgun record for: {}", assetBuilder);
        }
    }
}
