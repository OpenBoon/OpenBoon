package com.zorroa.archivist.processors;

import java.awt.Dimension;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.iptc.IptcDirectory;
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

    public AssetMetadataProcessor() { }

    @Override
    public void process(AssetBuilder asset) {
        if (isImageType(asset)) {
            /*
             * Depending on how configurable we want this to be, we might end up having
             * to split these into separate classes.
             */
            extractDimensions(asset);
            extractImageData(asset);
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

    /**
     * Handles pulling metadata out of the image itself, either by
     * EXIF, EXR header, DPX header, etc.  Currently only supports
     * EXIF.
     *
     * @param asset
     */
    public void extractImageData(AssetBuilder asset) {

        /*
         * Attempt to process EXIF data.
         */
        try {
            /*
             * Reuse the metadata object.
             */
            Metadata metadata = ImageMetadataReader.readMetadata(asset.getFile());
            extractExifData(asset, metadata);
            extractIptcData(asset, metadata);
        }
        catch (Exception e) {
            logger.error("Failed to load EXIF data, unexpected " + e, e);
        }
    }

    /*
     * Eventually refactor these metadata processors to be configuration based.
     */

    private void extractExifData(AssetBuilder asset, Metadata metadata) {

         ExifSubIFDDirectory d = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
         if (d != null) {
             asset.put("exif", "compression", d.getString(ExifSubIFDDirectory.TAG_COMPRESSION));
             asset.put("exif", "aperture", d.getString(ExifSubIFDDirectory.TAG_APERTURE));
         }
         else {
             logger.warn("Exif metdata not found: {}", asset.getAbsolutePath());
         }

         /*
          * Example of how to just dump all data.
          *
         for (Directory dir: metadata.getDirectories()) {
             for (Tag tag: dir.getTags()) {
                 logger.info("{}", tag);
             }
         }
         */
    }

    private void extractIptcData(AssetBuilder asset, Metadata metadata) {
        IptcDirectory i = metadata.getFirstDirectoryOfType(IptcDirectory.class);
        if (i != null) {
            asset.put("iptc", "keywords", i.getString(IptcDirectory.TAG_KEYWORDS));
        }
        else {
            logger.warn("Iptc metdata not found: {}", asset.getAbsolutePath());
        }
    }

    public boolean isImageType(AssetBuilder asset) {
        return imageService.getSupportedFormats().contains(asset.getExtension());
    }
}
