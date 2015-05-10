package com.zorroa.archivist;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Some basic JSON utilities.
 *
 * @author chambers
 *
 */
public class Json {

    public final static ObjectMapper Mapper = new ObjectMapper();

    public static byte[] serialize(Object object) {
        try {
            return Json.Mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                    "Failed to serialize object, unexpected: " + e, e);
        }
    }

    public static <T> T deserialize(byte[] data, Class<T> valueType) {
        try {
            return Json.Mapper.readValue(data, valueType);
        } catch (IOException e) {
             throw new org.springframework.dao.DataIntegrityViolationException(
                     "Failed to unserialize object, unexpected " + e, e);
        }
    }
}
