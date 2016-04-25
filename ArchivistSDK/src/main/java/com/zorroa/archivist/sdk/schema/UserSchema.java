package com.zorroa.archivist.sdk.schema;

/**
 * User editable fields.
 */
public class UserSchema extends ExtendableSchema<String, Object> {

    private Integer rating = null;

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

}
