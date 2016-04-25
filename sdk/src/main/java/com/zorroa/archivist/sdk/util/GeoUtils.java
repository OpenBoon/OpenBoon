package com.zorroa.archivist.sdk.util;

import com.zorroa.archivist.sdk.geocode.GeoName;
import com.zorroa.archivist.sdk.geocode.ReverseGeoCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.zip.ZipInputStream;

/**
 * Created by chambers on 1/25/16.
 */
public class GeoUtils {

    private static final Logger logger = LoggerFactory.getLogger(GeoUtils.class);

    private static volatile ReverseGeoCode reverseGeoCode = null;

    public static GeoName nearestPlace(double lat, double lon) {
        if (reverseGeoCode == null) {
            try {
                loadGeoCodes();
            } catch (IOException e) {
                logger.warn("Failed to load reverse geo code table");
            }
        }
        if (reverseGeoCode == null) {
            return null;
        }
        else {
            return reverseGeoCode.nearestPlace(lat, lon);
        }
    }

    private static synchronized void loadGeoCodes() throws IOException {
        if (reverseGeoCode != null) {
            return;
        }
        reverseGeoCode = new ReverseGeoCode(
                new ZipInputStream(GeoUtils.class.getClassLoader().getResourceAsStream("cities5000.zip")), true);
    }
}
