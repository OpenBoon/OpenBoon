package com.zorroa.archivist.sdk.domain;

import java.util.Map;

public class ExportOptions {

    private Images images = new Images();

    public ExportOptions() { }

    public Images getImages() {
        return images;
    }

    public void setImages(Images images) {
        this.images = images;
    }

    public class Images {

        /**
         * Strip off all existing image attributes.
         */
        private boolean stripMetdata = false;

        /**
         * The text for the copy right field, if any.
         */
        private String copyrightText;

        /**
         * A map of new arbitrary attributes to set.
         */
        private Map<String, String> attrs;

        /**
         * Image scale.  100 or <=0 is to leave as is.
         */
        private double scale = 100.0;

        /**
         * The quality of the image.  Supported values are between 0.0 and 1.0
         */
        private double quality = 1.0;

        /**
         * The format of the images, null to keep existing format.
         */
        private String format;

        public boolean isStripMetdata() {
            return stripMetdata;
        }

        public void setStripMetdata(boolean stripMetdata) {
            this.stripMetdata = stripMetdata;
        }

        public String getCopyrightText() {
            return copyrightText;
        }

        public void setCopyrightText(String copyrightText) {
            this.copyrightText = copyrightText;
        }

        public Map<String, String> getAttrs() {
            return attrs;
        }

        public void setAttrs(Map<String, String> attrs) {
            this.attrs = attrs;
        }

        public double getScale() {
            return scale;
        }

        public void setScale(double scale) {
            this.scale = scale;
        }

        public double getQuality() {
            return quality;
        }

        public void setQuality(double quality) {
            this.quality = quality;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

}
