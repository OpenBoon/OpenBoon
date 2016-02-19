/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.analyst.ingestors;

import java.util.List;

/**
 *
 * Returned value from Caffe classification.
 * keyword is a string for one or more words.
 * confidence is a float in [0, 1], currently normalized for an image.
 *
 */
public class CaffeKeyword {
    List<String> keywords;
    float confidence;

    CaffeKeyword() {}

    CaffeKeyword(List<String> keywords, float confidence) {
        this.keywords = keywords;
        this.confidence = confidence;
    }

    public String toString() {
        return String.format("<CaffeKeyword '%s' confidence='%f'>", keywords, confidence);
    }
}
