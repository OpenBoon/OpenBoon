package com.zorroa.analyst.domain;

import com.google.common.collect.Lists;
import com.zorroa.sdk.processor.Argument;
import com.zorroa.sdk.processor.DisplayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Scans a given class for @Argument annotations and aggregates the information
 * into a List<DisplayProperties>.
 */
public class ArgumentScanner {

    private static final Logger logger = LoggerFactory.getLogger(ArgumentScanner.class);

    public List<DisplayProperties> scan(Class<?> klass) {
        List<DisplayProperties> fields = Lists.newArrayList();
        for (Field childField: klass.getDeclaredFields()) {
            Annotation annotation = childField.getAnnotation(Argument.class);
            if (annotation == null) {
                continue;
            }

            DisplayProperties d = new DisplayProperties(childField, (Argument) annotation);
            fields.add(d);
            walkField(childField, annotation, d);
        }
        return fields;
    }

    private void walkField(Field field, Annotation annotation, DisplayProperties d) {

        Class type = field.getType();
        if (type.isPrimitive() || type.isArray() || type.isAssignableFrom(String.class)) {
            return;
        }

        for (Field childField: type.getDeclaredFields()) {
            Annotation childAnnotation = childField.getAnnotation(Argument.class);
            if (childAnnotation == null) {
                continue;
            }
            if (childField.getType().equals(type)) {
                continue;
            }

            DisplayProperties childDisplay = new DisplayProperties(childField, (Argument) childAnnotation);
            d.addToChildren(childDisplay);
            walkField(childField, childAnnotation, childDisplay);
        }
    }
}
