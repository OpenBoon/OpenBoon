package com.zorroa.archivist.processors;

import java.awt.Dimension;
import java.awt.geom.Dimension2D;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.zorroa.archivist.domain.AssetBuilder;

/**
 *
 * Attempts to extra metadata from all types of supported files.
 *
 * @author chambers
 *
 */
public class AssetMetadataProcessor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AssetMetadataProcessor.class);

    public AssetMetadataProcessor() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void process(AssetBuilder asset) {
        if (isImageType(asset)) {
            extractDimensions(asset);
            extractExifData(asset);
        }

    }

    public void extractDimensions(AssetBuilder asset) {
        try {
            Dimension size = imageService.getImageDimensions(asset.getFile());
            asset.put("source", "width", size.width);
            asset.put("source", "height", size.height);
        } catch (IOException e) {
            logger.warn("Unable to determine image dimensions: {}", asset, e);
        }
    }

    public void extractExifData(AssetBuilder asset) {

         try {
             Metadata metadata = ImageMetadataReader.readMetadata(asset.getFile());
             ExifSubIFDDirectory d = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
             if (d != null) {
                 asset.put("exif", "compression", d.getString(ExifSubIFDDirectory.TAG_COMPRESSION));
                 asset.put("exif", "aperture", d.getString(ExifSubIFDDirectory.TAG_APERTURE));
             }
             else {
                 logger.warn("Exif metdata not found: {}", asset.getAbsolutePath());
             }

             /*
             for (Directory dir: metadata.getDirectories()) {
                 for (Tag tag: dir.getTags()) {
                     logger.info("{}", tag);
                 }
             }
             */
         } catch (Exception e) {
             logger.error("Failed to process EXIF data:", e);
         }
    }

    public boolean isImageType(AssetBuilder asset) {
        return imageService.getSupportedFormats().contains(asset.getExtension());
    }


}
