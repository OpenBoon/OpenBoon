package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.geocode.GeoName;
import com.zorroa.archivist.sdk.processor.Argument;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.LocationSchema;
import com.zorroa.archivist.sdk.util.GeoUtils;

/**
 * Looks at the a Location schema and attempts to fill in more data
 * using the GeoUtils.
 */
public class ReverseGeocodeIngestor extends IngestProcessor {

    @Argument
    String locationAttr = "location";

    @Argument
    boolean isKeyword = true;

    @Override
    public void process(AssetBuilder asset) {
        LocationSchema location = asset.getAttr(locationAttr, LocationSchema.class);
        if (location == null) {
            logger.debug("The location attr '{}' does not exist", locationAttr);
            return;
        }

        logger.debug("Looking up location for {},{}",location.getPoint()[1], location.getPoint()[0]);
        GeoName place = GeoUtils.nearestPlace(
                location.getPoint()[1],
                location.getPoint()[0]);

        if (place == null) {
            logger.debug("The name of the location {},{} was unable to be found.", location);
            return;
        }

        logger.debug("Nearest town is " + place.name + " in " + place.country);

        if (place.name != null) {
            location.setCity(place.name);
            if (isKeyword) {
                asset.addSuggestKeywords(locationAttr, place.name);
            }
        }
        if (place.country != null) {
            location.setCountry(place.country);
            if (isKeyword) {
                asset.addSuggestKeywords(locationAttr, place.country);
            }
        }
    }
}
