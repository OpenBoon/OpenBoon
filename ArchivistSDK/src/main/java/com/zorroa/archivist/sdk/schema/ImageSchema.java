package com.zorroa.archivist.sdk.schema;

/**
 * Image Schema contains options that only pertain to Image assets.
 */
public class ImageSchema extends ExtendableSchema<String, Object> {

    private Integer width;
    private Integer height;

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }
}
