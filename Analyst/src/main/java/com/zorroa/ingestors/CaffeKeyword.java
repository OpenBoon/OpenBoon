/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.ingestors;

/**
 *
 * Returned value from Caffe classification.
 * keyword is a string for one or more words.
 * confidence is a float in [0, 1], currently normalized for an image.
 *
 */
public class CaffeKeyword {
    String keyword;
    float confidence;

    CaffeKeyword() {}

    CaffeKeyword(String keyword, float confidence) {
        this.keyword = keyword;
        this.confidence = confidence;
    }

    public String toString() {
        return String.format("<CaffeKeyword '%s' confidence='%f'>", keyword, confidence);
    }
}
