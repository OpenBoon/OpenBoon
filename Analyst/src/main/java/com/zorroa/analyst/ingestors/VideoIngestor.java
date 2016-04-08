package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.VideoSchema;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;

/**
 * Created by chambers on 1/2/16.
 */
public class VideoIngestor extends IngestProcessor {

    public VideoIngestor() {
        supportedFormats.add("mov");
        supportedFormats.add("avi");
        supportedFormats.add("mp4");
        supportedFormats.add("m4v");
        supportedFormats.add("m2v");
    }

    @Override
    public void process(AssetBuilder assetBuilder) {

        if (assetBuilder.attrExists("video") && !assetBuilder.isChanged()) {
            logger.debug("'video' schema already exists, skipping: {}", assetBuilder);
            return;
        }

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
            assetBuilder.setAttr("video", video);

        } catch (Exception e) {
            throw new UnrecoverableIngestProcessorException(
                    "Unable to extract video metadata from " + assetBuilder.getAbsolutePath(), e, getClass());
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
            VideoSchema video = assetBuilder.getAttr("video", VideoSchema.class);
            video.setFrames(grabber.getLengthInFrames());
            video.setAspectRatio(grabber.getAspectRatio());
            video.setAudioChannels(grabber.getAudioChannels());
            video.setFormat(grabber.getFormat());
            video.setFrameRate(grabber.getFrameRate());
            video.setSampleRate(grabber.getSampleRate());
            video.setWidth(grabber.getImageWidth());
            video.setHeight(grabber.getImageHeight());
            video.setFrames(grabber.getLengthInFrames());
            video.setAspectRatio(grabber.getAspectRatio());
            
            grabber.setFrameNumber(grabber.getLengthInFrames() / 2);
            Frame frame = grabber.grabImage();

            BufferedImage image = converter.convert(frame);
            assetBuilder.setImage(image);
            grabber.stop();
        } catch (Exception e) {
            throw new UnrecoverableIngestProcessorException(
                    "Unable to extract video metadata from " + assetBuilder.getAbsolutePath(), e, getClass());
        }
    }
}
