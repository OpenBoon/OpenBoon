package com.zorroa.archivist.processors;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.zorroa.archivist.domain.AssetBuilder;


public class AssetMetadataProcessor extends IngestProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AssetMetadataProcessor.class);


    public AssetMetadataProcessor() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void process(AssetBuilder builder, File file) {

        try {

            Metadata metadata = ImageMetadataReader.readMetadata(file);
            ExifSubIFDDirectory d = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            builder.put("source", "width", d.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH));
            builder.put("source", "height", d.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT));
            builder.put("source", "path", file.getAbsolutePath());

            // Dump what is available.
            for (Directory dir: metadata.getDirectories()) {
                for (Tag tag: dir.getTags()) {
                    logger.info("{}", tag);
                }
            }

        } catch (ImageProcessingException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MetadataException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
