package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.filesystem.ObjectFile;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ImageSchema;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * An temporary ingestor for handling EXR,DPX, and CIN.  Eventually these
 * will be handled by the ImageIngesteor.  To keep it simple, we don't
 * worry about any attributes.
 */
public class HdrImageIngestor extends IngestProcessor {

    private static final List<String> formats = ImmutableList.of("exr", "dpx", "cin");

    public HdrImageIngestor() {
        supportedFormats.addAll(formats);
    }

    @Override
    public void process(AssetBuilder asset) {
        try {
            ObjectFile  tmpFile = objectFileSystem.prepare("tmp", asset.getId(), "jpg");
            ProcessBuilder pb =
                    new ProcessBuilder("oiiotool",
                            asset.getAbsolutePath(),
                            "-o", tmpFile.getFile().getAbsolutePath());

            int result = pb.inheritIO().start().waitFor();
            if (result != 0) {
                throw new RuntimeException("Failed to execute oiiotool, retval: " + result);
            }

            BufferedImage image = ImageIO.read(tmpFile.getFile());
            asset.setImage(image);

            ImageSchema schema = new ImageSchema();
            schema.setHeight(image.getHeight());
            schema.setWidth(image.getWidth());
            asset.setAttr("image", schema);

        } catch (Exception e) {
            throw new UnrecoverableIngestProcessorException(
                    "Unable to read HDR image  from "
                            + asset.getAbsolutePath() + "," + e.getMessage(), e, getClass());
        }
    }

}
