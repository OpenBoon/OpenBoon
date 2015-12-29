package com.zorroa.archivist.sdk.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zorroa.archivist.sdk.exception.MalformedDataException;

import java.io.IOException;

/**
 * Some basic JSON utilities.
 *
 * @author chambers
 *
 */
public class Json {

    public final static ObjectMapper Mapper = new ObjectMapper();
    static {
        Mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        Mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        Mapper.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, true);
        Mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String serializeToString(Object object) {
        try {
            return Json.Mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new MalformedDataException(
                    "Failed to serialize object, unexpected: " + e, e);
        }
    }

    public static byte[] serialize(Object object) {
        try {
            return Json.Mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new MalformedDataException(
                    "Failed to serialize object, unexpected: " + e, e);
        }
    }

    public static String serializeToString(Object object, String onNull) {
        if (object == null) {
            return onNull;
        }
        try {
            return Json.Mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new MalformedDataException(
                    "Failed to serialize object, unexpected: " + e, e);
        }
    }

    public static byte[] serialize(Object object, String onNull) {
        if (object == null) {
            return onNull.getBytes();
        }
        try {
            return Json.Mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new MalformedDataException(
                    "Failed to serialize object, unexpected: " + e, e);
        }
    }

    public static <T> T deserialize(byte[] data, Class<T> valueType) {
        try {
            return Json.Mapper.readValue(data, valueType);
        } catch (IOException e) {
            throw new MalformedDataException(
                    "Failed to unserialize object, unexpected " + e, e);
        }
    }

    public static <T> T deserialize(byte[] data, TypeReference<T> valueType) {
        try {
            return Json.Mapper.readValue(data, valueType);
        } catch (IOException e) {
            throw new MalformedDataException(
                    "Failed to unserialize object, unexpected " + e, e);
        }
    }

    public static <T> T deserialize(String data, Class<T> valueType) {
        try {
            return Json.Mapper.readValue(data, valueType);
        } catch (IOException e) {
            throw new MalformedDataException(
                    "Failed to unserialize object, unexpected " + e, e);
        }
    }

    public static <T> T deserialize(String data, TypeReference<T> valueType) {
        try {
            return Json.Mapper.readValue(data, valueType);
        } catch (IOException e) {
            throw new MalformedDataException(
                    "Failed to unserialize object, unexpected " + e, e);
        }
    }
}
