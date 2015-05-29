package com.zorroa.archivist.processors;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;

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

    public AssetMetadataProcessor() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void process(AssetBuilder asset) {
        if (isImageType(asset)) {
            extractDimensions(asset);
            extractMetadata(asset);
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

    public void extractMetadata(AssetBuilder asset) {
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
             
             IptcDirectory i = metadata.getFirstDirectoryOfType(IptcDirectory.class);
             if (i != null) {
            	 // Convert the space-separated keywords into a string array
            	 String keywords = i.getString(IptcDirectory.TAG_KEYWORDS);
            	 List<String> keywordList = Arrays.asList(keywords.split(" "));
            	 // Remove any suffix, which typically contains a confidence term,
            	 // e.g. "vase:0.0375"
            	 for (int j = 0; j < keywordList.size(); j++) {
            		 String word = keywordList.get(j);
            		 int lastIndex = word.lastIndexOf(':');
            		 if (lastIndex > 0) {
            			 String prefix = word.substring(0, word.lastIndexOf(':'));
            			 keywordList.set(j, prefix);
            		 }
            	 }
        		 asset.put("iptc", "keywords", keywordList);
             }
             else {
                 logger.warn("Iptc metdata not found: {}", asset.getAbsolutePath());
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
