/*
 * Copyright (c) 2016 by Zorroa
 */

package com.zorroa.archivist.sdk.processor;

/**
 * Created by wex on 2/19/16.
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Argument {

    String name() default "";

}
