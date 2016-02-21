package com.zorroa.archivist.sdk.schema;

/**
 * Created by chambers on 11/24/15.
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Keyword {

    double confidence() default KeywordsSchema.CONFIDENCE_MAX;

}
