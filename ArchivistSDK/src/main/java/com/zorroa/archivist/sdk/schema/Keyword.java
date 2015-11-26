package com.zorroa.archivist.sdk.schema;

/**
 * Created by chambers on 11/24/15.
 */

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Keyword {

    int confidence() default 5;

}
