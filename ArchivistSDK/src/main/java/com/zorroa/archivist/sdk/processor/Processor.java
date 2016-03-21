package com.zorroa.archivist.sdk.processor;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.domain.ApplicationProperties;
import com.zorroa.archivist.sdk.util.Json;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;

/**
 * Created by chambers on 11/2/15.
 */
public class Processor {

    private Map<String, Object> args;
    protected ApplicationProperties applicationProperties;

    public Processor() {
        this.args = Maps.newHashMap();
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public <T> T getArg(String key) {
        return (T) this.args.get(key);
    }

    public void setArg(String key, Object value) {
        this.args.put(key, value);
    }

    public ApplicationProperties getApplicationProperties() {
        return applicationProperties;
    }

    public Processor setApplicationProperties(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        return this;
    }

    /**
     *  Assign annotated arguments using extracted JSON args
     */
    protected void setArguments() {
        // Loop over the fields, looking for annotated arguments, and assigning parsed JSON values
        for (Field field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Argument argument = field.getAnnotation(Argument.class);
            if (argument == null) {
                continue;
            }

            // Allow the annotation to optionally override the default field name
            String name = argument.name().equals("") ? field.getName() : argument.name();
            Object arg = getArg(name);
            if (arg == null) {
                continue;
            }

            // Use reflection to build a JavaType for the Jackson mapper,
            // handling collection, map and scalar fields
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setVisibility(objectMapper.getSerializationConfig()
                    .getDefaultVisibilityChecker()
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            JavaType javaType;
            if (Collection.class.isAssignableFrom(field.getType())) {
                ParameterizedType paramType = (ParameterizedType) field.getGenericType();
                Class<?> genericType = (Class<?>) paramType.getActualTypeArguments()[0];
                javaType = typeFactory.constructCollectionType(Collection.class, genericType);
            } else if (Map.class.isAssignableFrom(field.getType())) {
                ParameterizedType paramType = (ParameterizedType) field.getGenericType();
                Class<?> keyType = (Class<?>) paramType.getActualTypeArguments()[0];
                Class<?> valueType = (Class<?>) paramType.getActualTypeArguments()[1];
                javaType = typeFactory.constructMapType(Map.class, keyType, valueType);
            } else {
                javaType = typeFactory.constructType(field.getType());
            }

            // Convert the value from the JSON args and assign it this field
            Object value = Json.Mapper.convertValue(arg, javaType);
            try {
                field.set(this, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Ths function is called once at the end of the entire ingest/export process.  Its NOT called
     * on a per-asset basis.  The intent is that subclasses can override this, but its not
     * required.
     */
    public void teardown() { }
}
