package com.zorroa.archivist.sdk.util;

import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.sdk.domain.AssetType;

import javax.imageio.ImageIO;
import java.util.Arrays;

/**
 * Created by chambers on 10/30/15.
 *
 */
public class IngestUtils {

    /**
     * This is a lame way to do this, but it works for now.
     */
    public static final ImmutableSet<String> SUPPORTED_IMG_FORMATS = ImmutableSet.<String>builder()
            .addAll(Arrays.asList(ImageIO.getReaderFormatNames())).build();

    public static final ImmutableSet<String> SUPPORTED_DOC_FORMATS = ImmutableSet.<String>builder()
            .add("pdf").build();

    public static AssetType determineAssetType(String ext) {
        if (SUPPORTED_IMG_FORMATS.contains(ext)) {
            return AssetType.Image;
        } else if (SUPPORTED_DOC_FORMATS.contains(ext)) {
            return AssetType.Document;
        } else {
            return AssetType.Unknown;
        }
    }

}
