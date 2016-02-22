package com.zorroa.archivist.service;

import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.schema.SourceSchema;
import com.zorroa.archivist.security.ExportOptionsService;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

@Component
public class ExportOptionsServiceImpl implements ExportOptionsService {

    private static final Logger logger = LoggerFactory.getLogger(ExportOptionsServiceImpl.class);


    @Override
    public ExportedAsset applyOptions(Export export, ExportOutput output, Asset asset) throws Exception {
        /*
         * Currently we only handle image data.
         */
        ExportedAsset result = new ExportedAsset(export, output, asset);
        String type = result.getAsset().getSource().getType();

        if (type != null && type.startsWith("image/")) {
            applyImageOptions(export, result);
        }
        // TODO: add other types.
        return result;
    }

    public void applyImageOptions(Export export, ExportedAsset asset) throws Exception {
        if (asset.getExportOutput().getPath() == null) {
            return;
        }

        ExportOptions.Images imgOpts = export.getOptions().getImages();
        if (imgOpts == null) {
            return;
        }

        SourceSchema source = asset.getAsset().getSchema("source", SourceSchema.class);
        String format =  imgOpts.getFormat() == null ? source.getExtension() : imgOpts.getFormat();
        BufferedImage inputImage = ImageIO.read(asset.getCurrentFile());
        BufferedImage outputImage;

        outputImage = Thumbnails.of(inputImage)
                .outputQuality(imgOpts.getQuality())
                .outputFormat(imgOpts.getFormat())
                .scale(imgOpts.getScale())
                .asBufferedImage();

        File outputFile = new File(asset.nextPath(source.getBasename(), format));
        ImageIO.write(outputImage, format, outputFile);
    }
}
