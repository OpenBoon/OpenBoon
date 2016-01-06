package com.zorroa.archivist.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.VideoSchema;
import com.zorroa.archivist.sdk.service.EventLogService;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.image.BufferedImage;

/**
 * Created by chambers on 1/2/16.
 */
public class VideoIngestor extends IngestProcessor {

    @Autowired
    EventLogService eventLogService;

    public VideoIngestor() {
        supportedFormats.add("mov");
        supportedFormats.add("avi");
        supportedFormats.add("mp4");
    }

    @Override
    public void process(AssetBuilder assetBuilder) {
        extractMetadata(assetBuilder);
        extractImage(assetBuilder);
    }

    public void extractMetadata(AssetBuilder assetBuilder) {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();

        try {
            parser.parse(assetBuilder.getInputStream(), handler, metadata);
            VideoSchema video = new VideoSchema();
            video.setHeight(Integer.parseInt(metadata.get("tiff:ImageLength")));
            video.setWidth(Integer.parseInt(metadata.get("tiff:ImageWidth")));
            video.setAudioSampleRate(Integer.parseInt(metadata.get("xmpDM:audioSampleRate")));
            video.setDuration(Double.parseDouble(metadata.get("xmpDM:duration")));
            assetBuilder.addSchema("video", video);

        } catch (Exception e) {
            logger.warn("{}", e);
            eventLogService.log("Failed to extra metadata from video: {}", e, assetBuilder.getFilename());
        }
    }

    public void extractImage(AssetBuilder assetBuilder) {

        Java2DFrameConverter converter = new Java2DFrameConverter();

        try {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(assetBuilder.getFile());
            grabber.start();

            /*
             * This metadata seems to get only get populated if we start playback.
             */
            VideoSchema video = assetBuilder.getSchema("video", VideoSchema.class);
            video.setFrames(grabber.getLengthInFrames());
            video.setAspectRatio(grabber.getAspectRatio());
            video.setAudioChannels(grabber.getAudioChannels());
            video.setFormat(grabber.getFormat());
            video.setFrameRate(grabber.getFrameRate());
            video.setSampleRate(grabber.getSampleRate());

            Frame frame = grabber.grabImage();
            BufferedImage image = converter.convert(frame);
            assetBuilder.setImage(image);
            grabber.stop();
        } catch (Exception e) {
            logger.warn("{}", e);
            eventLogService.log("Failed to capture image from video: {}", e, assetBuilder.getFilename());
        }
    }
}
