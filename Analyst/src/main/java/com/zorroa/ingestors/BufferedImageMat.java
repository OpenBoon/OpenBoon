/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.ingestors;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class BufferedImageMat {

    private static OpenCVLoader openCVLoader = new OpenCVLoader();

    // Convert a BufferedImage to an OpenCV Mat
    static public Mat convertBufferedImageToMat(BufferedImage image) {
        int curCVtype = CvType.CV_8UC4;
        boolean supportedType = true;

        switch (image.getType()) {
            case BufferedImage.TYPE_3BYTE_BGR:
                curCVtype = CvType.CV_8UC3;
                break;
            case BufferedImage.TYPE_BYTE_GRAY:
            case BufferedImage.TYPE_BYTE_BINARY:
                curCVtype = CvType.CV_8UC1;
                break;
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_INT_RGB:
                curCVtype = CvType.CV_32SC3;
                break;
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                curCVtype = CvType.CV_32SC4;
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
                curCVtype = CvType.CV_16UC1;
                break;
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                curCVtype = CvType.CV_8UC4;
                break;
            default:
                // BufferedImage.TYPE_BYTE_INDEXED;
                // BufferedImage.TYPE_CUSTOM;
                System.out.println("Unsupported format:" + image.getType());
                supportedType = false;
        }

        // Convert to Mat
        Mat mat = new Mat(image.getHeight(), image.getWidth(), curCVtype);
        if (supportedType) {
            // Insert pixel buffer directly
            byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, pixels);
        } else {
            // Convert to RGB first
            int height = image.getHeight();
            int width = image.getWidth();
            int[] pixels = image.getRGB(0, 0, width - 1, height - 1, null, 0, width);

            // Convert ints to bytes
            ByteBuffer byteBuffer = ByteBuffer.allocate(pixels.length * 4);
            IntBuffer intBuffer = byteBuffer.asIntBuffer();
            intBuffer.put(pixels);

            byte[] pixelBytes = byteBuffer.array();

            mat.put(0, 0, pixelBytes);

            // Reorder the channels for Opencv BGRA format from
            // BufferedImage ARGB format
            Mat imgMix = mat.clone();
            ArrayList<Mat> imgSrc = new ArrayList<Mat>();
            imgSrc.add(imgMix);

            ArrayList<Mat> imgDest = new ArrayList<Mat>();
            imgDest.add(mat);

            int[] fromTo = { 0, 3, 1, 2, 2, 1, 3, 0 }; //Each pair is a channel swap
            Core.mixChannels(imgSrc, imgDest, new MatOfInt(fromTo));
        }

        return mat;
    }

    // Convert an OpenCV Mat to a BufferedImage
    // A guideline for a proper implementation, which should invert the type computations above
    // TODO: Only handles 3 channel matrices!
    static public BufferedImage convertMatToBufferedImage(Mat mat) {
        byte[] data = new byte[mat.rows() * mat.cols() * (int)(mat.elemSize())];
        mat.get(0, 0, data);
        if (mat.channels() == 3) {
            for (int i = 0; i < data.length; i += 3) {
                byte temp = data[i];
                data[i] = data[i + 2];
                data[i + 2] = temp;
            }
        }
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_3BYTE_BGR);
        image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
        return image;
    }
}
