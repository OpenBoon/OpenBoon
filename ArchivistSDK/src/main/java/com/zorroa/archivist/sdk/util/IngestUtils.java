package com.zorroa.archivist.sdk.util;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.sdk.domain.AssetType;

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
            .put("jpg", AssetType.Image)
            .put("jpeg", AssetType.Image)
            .put("png", AssetType.Image)
            .put("gif", AssetType.Image)
            .put("bmp", AssetType.Image)

            .build();


    public static AssetType determineAssetType(String ext) {
        return SUPPORTED_FORMATS.getOrDefault(ext.toLowerCase(), AssetType.Unknown);
    }

    public static boolean isSupportedFormat(String ext) {
        return SUPPORTED_FORMATS.containsKey(ext.toLowerCase());
    }
}
