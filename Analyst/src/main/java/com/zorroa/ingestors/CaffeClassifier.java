/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.ingestors;

import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 *
 * Java wrapper class for Caffe native C library.
 *
 */
public class CaffeClassifier {
    private static final Logger logger = LoggerFactory.getLogger(CaffeIngestor.class);

    private static final native long createCaffeClassifier(String deployPath, String modelPath, String meanPath, String wordPath);
    private static final native CaffeKeyword[] classify(long caffeClassifier, long imageMat);
    private static final native void destroyCaffeClassifier(long classifier);

    private long nativeCaffeClassifier;

    public CaffeClassifier(String deployPath, String modelPath, String binaryProtoPath, String wordPath) {
        nativeCaffeClassifier = createCaffeClassifier(deployPath, modelPath, binaryProtoPath, wordPath);
    }

    public CaffeKeyword[] classify(Mat image) {
        CaffeKeyword[] keywords = classify(nativeCaffeClassifier, image.getNativeObjAddr());
        keywords = deduplicateKeywords(keywords);
        return keywords;
    }

    public void destroy() {
        if (nativeCaffeClassifier != 0) {
            destroyCaffeClassifier(nativeCaffeClassifier);
        }
    }

    // Combine duplicate entries, adding together their classification relevance
    private CaffeKeyword[] deduplicateKeywords(CaffeKeyword[] keywords) {
        HashMap<String, Float> existing = new HashMap<String, Float>();
        for (CaffeKeyword keyword : keywords) {
            if (existing.containsKey(keyword.keyword)) {
                float confidence = existing.get(keyword.keyword);
                confidence += keyword.confidence;
                existing.put(keyword.keyword, confidence);
            } else {
                existing.put(keyword.keyword, keyword.confidence);
            }
        }
        if (existing.size() < keywords.length) {
            CaffeKeyword[] dedup = new CaffeKeyword[existing.size()];
            int j = 0;
            for (String key : existing.keySet()) {
                CaffeKeyword keyword = new CaffeKeyword(key, existing.get(key));
                dedup[j++] = keyword;
            }
            return dedup;
        }
        return  keywords;
    }
}
