package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.geocode.GeoName;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.Argument;
import com.zorroa.archivist.sdk.schema.KeywordsSchema;
import com.zorroa.archivist.sdk.util.GeoUtils;

import java.awt.geom.Point2D;

/**
 * Created by wex on 3/7/16.
 */
public class ReverseGeocodeIngestor extends IngestProcessor {

    @Argument
    String assetField = "source.location";

    @Argument
    String namespace = "place";

    @Argument
    boolean isKeyword = true;

    @Override
    public void process(AssetBuilder asset) {
        Point2D.Double pt = asset.getAttr(assetField);
        if (pt == null) {
            return;
        }
        GeoName place = GeoUtils.nearestPlace(pt.x, pt.y);
        if (place == null) {
            return;
        }
        logger.info("Nearest town is " + place.name + " in " + place.country);
        if (place.name != null) {
            asset.setAttr(namespace, "name", place.name);
            if (isKeyword) {
                asset.addKeywords(KeywordsSchema.CONFIDENCE_MAX, true, place.name);
            }
        }
        if (place.country != null) {
            asset.setAttr(namespace, "country", place.country);
            if (isKeyword) {
                asset.addKeywords(KeywordsSchema.CONFIDENCE_MAX, true, place.country);
            }
        }
    }
}
