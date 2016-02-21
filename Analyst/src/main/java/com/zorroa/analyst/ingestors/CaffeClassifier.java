/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.analyst.ingestors;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.caffe;
import org.bytedeco.javacpp.opencv_core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Arrays;

import static org.bytedeco.javacpp.caffe.*;
import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_core.MatVector;
import static org.bytedeco.javacpp.opencv_core.Scalar;
import static org.bytedeco.javacpp.opencv_core.Size;
import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_core.CV_32FC3;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_GRAY2BGR;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGRA2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGRA2BGR;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

/**
 *
 * Simplified interface to the Caffe native library
 *
 */
public class CaffeClassifier {
    final FloatNet network;
    final int kInputLayerChannels;
    final Size kInputLayerGeometry;
    final Mat kModelMean;
    final List<List<String>> kSynsetLabels;

    private static final Logger logger = LoggerFactory.getLogger(CaffeClassifier.class);

    public CaffeClassifier(String deployPath, String modelPath, String binaryProtoPath, String wordPath) throws IOException {
        Caffe.set_mode(Caffe.CPU);
        network = new FloatNet(deployPath, TEST);
        network.CopyTrainedLayersFrom(modelPath);
        assert network.num_inputs() == 1;
        assert network.num_outputs() == 1;
        FloatBlob inputLayer = network.input_blobs().get(0);
        kInputLayerChannels = inputLayer.channels();
        assert kInputLayerChannels == 3 || kInputLayerChannels == 1;
        kInputLayerGeometry = new Size(inputLayer.width(), inputLayer.height());
        kModelMean = loadModelMean(binaryProtoPath);
        kSynsetLabels = loadSynsetLabels(wordPath);
    }

    private Mat loadModelMean(String meanPath) {
        BlobProto blobProto = new BlobProto();
        if (!caffe.ReadProtoFromBinaryFile(meanPath, blobProto)) {
            return null;
        }

        // Convert from BlobProto to FloatBlob
        FloatBlob meanBlob = new FloatBlob();
        meanBlob.FromProto(blobProto);
        assert meanBlob.channels() == kInputLayerChannels;

        // The format of the kModelMean file is planar 32-bit float BGR or grayscale
        opencv_core.MatVector channels = new opencv_core.MatVector(kInputLayerChannels);
        FloatPointer data = meanBlob.mutable_cpu_data();
        for (int i = 0; i < kInputLayerChannels; ++i) {
            // Extract an individual channel
            Mat channel = new Mat(meanBlob.height(), meanBlob.width(), CV_32FC1, data);
            channels.put(i, channel);
            data.position((i + 1) * meanBlob.width() * meanBlob.height());
        }

        // Merge the separate channels into a single image
        Mat tmpMean = new Mat();
        opencv_core.merge(channels, tmpMean);

        // Compute the global kModelMean pixel value and create a kModelMean image filled with this value
        Scalar channelMean = opencv_core.mean(tmpMean);
        return new Mat(kInputLayerGeometry, tmpMean.type(), channelMean);
    }

    private List<List<String>> loadSynsetLabels(String wordPath) throws IOException {
        // Load Labels
        List<String> synsets = Files.readLines(new File(wordPath), Charsets.UTF_8);
        FloatBlob outputLayer = network.output_blobs().get(0);
        assert synsets.size() == outputLayer.channels();

        // Split synset kSynsetLabels into arrays of keywords, removing the synset node id
        List<List<String>> synsetLabels = new ArrayList<>();
        for (String node : synsets) {
            // First slice off the synset node id, the first space-separated word
            String commaKeywords = node.substring(node.indexOf(" ") + 1);

            // Separate the remaining words assuming a comma+space
            // Theoretically, we should NOT need to strip leading and trailing whitespace
            List<String> keywords = Arrays.asList(commaKeywords.split(", "));
            synsetLabels.add(keywords);
        }

        return synsetLabels;
    }

    public List<CaffeKeyword> classify(Mat image, int n, float threshold) {
        // FIXME: Can this block move into the ctor?
        FloatBlob inputLayer = network.input_blobs().get(0);
        inputLayer.Reshape(1, kInputLayerChannels, kInputLayerGeometry.height(), kInputLayerGeometry.width());
        network.Reshape();  // Forward dimension change to all layers

        MatVector inputChannels = new MatVector();
        wrapInputLayer(inputChannels);

        Mat sampleResizedFloat = formatImageToNetwork(image, inputChannels);
        Mat sampleNormalized = new Mat();
        opencv_core.subtract(sampleResizedFloat, kModelMean, sampleNormalized);

        // This operation will write the separate BGR planes directly to the
        // input layer of the network because it is wrapped by the cv::Mat
        // objects in inputChannels
        opencv_core.split(sampleNormalized, inputChannels.get(0));
        assert inputChannels.get(0).data().address() == network.input_blobs().get(0).cpu_data().address();

        network.ForwardPrefilled();

        List<CaffeKeyword> keywords = findTopKeywords(n, threshold);
        return keywords;
    }

    private void wrapInputLayer(MatVector inputChannels) {
        FloatBlob inputLayer = network.input_blobs().get(0);
        int width = inputLayer.width();
        int height = inputLayer.height();
        inputChannels.resize(inputLayer.channels());
        FloatPointer inputData = inputLayer.mutable_cpu_data();
        FloatPointer channelData = inputData;
        for (int i = 0; i < inputLayer.channels(); ++i) {
            Mat channel = new Mat(height, width, CV_32FC1, channelData);
            inputChannels.put(i, channel);

            // Offset to next channel by slicing the direct buffer (no copy or allocate)
            inputData.position((i + 1) * width * height);
            channelData = new FloatPointer(inputData.asBuffer().slice());
        }
    }

    private Mat formatImageToNetwork(Mat image, MatVector inputChannels) {
        Mat sample = new Mat();
        if (image.channels() == 3 && kInputLayerChannels == 1) {
            cvtColor(image, sample, CV_BGR2GRAY);
        } else if (image.channels() == 4 && kInputLayerChannels == 1) {
            cvtColor(image, sample, CV_BGRA2GRAY);
        } else if (image.channels() == 4 && kInputLayerChannels == 3) {
            cvtColor(image, sample, CV_BGRA2BGR);
        } else if (image.channels() == 1 && kInputLayerChannels == 3) {
            cvtColor(image, sample, CV_GRAY2BGR);
        } else {
            sample = image;
        }

        // FIXME: Thumbs are usually 256x256, but input layer is 227x227!
        Mat sampleResized = new Mat();
        if (sample.size() != kInputLayerGeometry) {
            resize(sample, sampleResized, kInputLayerGeometry);
        } else {
            sampleResized = sample;
        }

        Mat sampleFloat = new Mat();
        if (kInputLayerChannels == 3) {
            sampleResized.convertTo(sampleFloat, CV_32FC3);
        } else {
            sampleResized.convertTo(sampleFloat, CV_32FC1);
        }

        return sampleFloat;
    }

    private List<CaffeKeyword> findTopKeywords(int n, float threshold) {
        // Use a min-heap priority queue to find the top N entries
        class MaxComparator implements Comparator<CaffeKeyword> {
            public int compare(CaffeKeyword a, CaffeKeyword b) {
                return Float.compare(b.confidence, a.confidence);
            }
        }
        PriorityQueue<CaffeKeyword> queue = new PriorityQueue<>(5, new MaxComparator());
        FloatBlob outputLayer = network.output_blobs().get(0);
        FloatPointer outputData = outputLayer.cpu_data();

        for (int i = 0; i < outputLayer.channels(); ++i) {
            float confidence = outputData.get(i);
            if (confidence > threshold) {
                CaffeKeyword keyword = new CaffeKeyword(kSynsetLabels.get(i), threshold);
                queue.add(keyword);
            }
        }

        List<CaffeKeyword> keywords = new ArrayList<>();
        for (int i = 0; i < n; ++i) {
            try {
                CaffeKeyword keyword = queue.remove();
                keywords.add(keyword);
            } catch (NoSuchElementException e) {
                break;
            }
        }
        return keywords;
    }

    public void destroy() {
        // FIXME: Release big blobs of memory
    }
}
