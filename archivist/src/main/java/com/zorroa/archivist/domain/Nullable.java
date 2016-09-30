package com.zorroa.archivist.domain;

/**
 * Created by chambers on 9/30/16.
 */
import java.lang.annotation.*;

/**
 * The presence of this annotation on a method parameter indicates that
 * {@code null} is an acceptable value for that parameter.  It should not be
 * used for parameters of primitive types.
 *
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
public @interface Nullable {
}
