package com.zorroa.archivist.sdk.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.domain.AssetType;

import javax.imageio.ImageIO;
import java.util.Map;

/**
 * Created by chambers on 10/30/15.
 *
 */
public class IngestUtils {

    public static final Map<String, AssetType> SUPPORTED_FORMATS = ImmutableMap.<String, AssetType>builder()
            /*
             * Image
             */

            .putAll(getImageFormats())
            .build();

    private static Map<String, AssetType> getImageFormats() {
        Map<String, AssetType> result  = Maps.newHashMap();
        for (String format: ImageIO.getReaderFormatNames()) {
            result.put(format.toLowerCase(), AssetType.Image);
        }
        return result;
    }

    public static AssetType determineAssetType(String ext) {
        return SUPPORTED_FORMATS.getOrDefault(ext.toLowerCase(), AssetType.Unknown);
    }

    public static boolean isSupportedFormat(String ext) {
        return SUPPORTED_FORMATS.containsKey(ext.toLowerCase());
    }


}
