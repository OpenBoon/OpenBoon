package com.zorroa.analyst.ingestors;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

import static org.bytedeco.javacpp.opencv_core.Mat;

/**
 * Created by wex on 2/20/16.
 */
public final class OpenCVUtils {
    private static final Logger logger = LoggerFactory.getLogger(OpenCVUtils.class);
    
    private OpenCVUtils() {}        // Disallow instantiation

    public static Mat convert(BufferedImage bufferedImage) {
        Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();
        Frame frame = java2DFrameConverter.convert(bufferedImage);
        OpenCVFrameConverter openCVFrameConverter = new OpenCVFrameConverter.ToMat();
        return openCVFrameConverter.convertToMat(frame);
    }
}
