package com.zorroa.archivist.sdk.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zorroa.archivist.sdk.exception.MalformedDataException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Some basic JSON utilities.
 *
 * @author chambers
 *
 */
public class Json {

    public static TypeReference<Map<String, Object>> GENERIC_MAP = new TypeReference<Map<String, Object>>() { };
    public static TypeReference<Set<Integer>> SET_OF_INTS = new TypeReference<Set<Integer>>() { };
    public static TypeReference<Set<String>> SET_OF_STRINGS = new TypeReference<Set<String>>() { };
    public static TypeReference<List<Integer>> LIST_OF_INTS = new TypeReference<List<Integer>>() { };
    public static TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<List<String>>() { };

    public final static ObjectMapper Mapper = new ObjectMapper();
    static {
        Mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        Mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        Mapper.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, true);
        Mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String prettyString(Object object) {
        try {
            return Mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new MalformedDataException(
                    "Failed to serialize object, unexpected: " + e, e);
        }
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
