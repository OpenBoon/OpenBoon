package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.KeywordsSchema;
import com.zorroa.archivist.sdk.schema.VideoSchema;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
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

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(assetBuilder.getFile());

        try {
            grabber.start();
            extractMetadata(grabber, assetBuilder);
            extractImage(grabber, assetBuilder);
         } catch (Exception e) {
            throw new UnrecoverableIngestProcessorException(
                "Unable to extract video metadata from " + assetBuilder.getAbsolutePath(), e, getClass());
        }
        finally {
            try {
                grabber.stop();
            } catch (FrameGrabber.Exception e) {
                logger.warn("Failed to stop frame grabber, ", e);
            }
        }
    }

    public void extractMetadata(FFmpegFrameGrabber grabber, AssetBuilder assetBuilder) {

        VideoSchema video = new VideoSchema();
        assetBuilder.setAttr("video", video);
        video.setFrames(grabber.getLengthInFrames());
        video.setAspectRatio(grabber.getAspectRatio());
        video.setAudioChannels(grabber.getAudioChannels());
        video.setAudioSampleRate(grabber.getAudioBitrate());
        video.setFormat(grabber.getFormat());
        video.setFrameRate(grabber.getFrameRate());
        video.setSampleRate(grabber.getSampleRate());
        video.setWidth(grabber.getImageWidth());
        video.setHeight(grabber.getImageHeight());
        video.setFrames(grabber.getLengthInFrames());
        video.setAspectRatio(grabber.getAspectRatio());
        video.setDuration(grabber.getLengthInTime());
        video.setDescription(grabber.getMetadata("description"));
        video.setTitle(grabber.getMetadata("title"));
        video.setSynopsis(grabber.getMetadata("synopsis"));

        KeywordsSchema keywords = assetBuilder.getKeywords();
        keywords.addKeywords("video", video.getTitle(),
                video.getDescription(), video.getSynopsis());
    }

    public void extractImage(FFmpegFrameGrabber grabber, AssetBuilder assetBuilder) throws FrameGrabber.Exception {

        Java2DFrameConverter converter = new Java2DFrameConverter();
        grabber.setFrameNumber(grabber.getLengthInFrames() / 2);
        Frame frame = grabber.grabImage();

        BufferedImage image = converter.convert(frame);
        assetBuilder.setImage(image);
    }
}
