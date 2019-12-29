package com.zorroa.zmlp.sdk;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Utils {

    /**
     * Return true if a String value is a valid JSON
     *
     * @param json JSON in string Format
     * @return True if is a Valid JSON
     */
    public static boolean isValidJSON(final String json) {
        boolean valid = false;
        try {
            final JsonParser parser = new ObjectMapper().getJsonFactory()
                    .createJsonParser(json);
            while (parser.nextToken() != null) {
            }
            valid = true;
        } catch (JsonParseException jpe) {
            jpe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return valid;
    }

    /**
     * Return true if the given value is a valid UUID
     *
     * @param uuidString a string which might be a UUID.
     * @return True if is a valid UUID
     */
    public static Boolean isValidUUI(String uuidString) {

        try {
            UUID uuid = UUID.fromString(uuidString);
            return true;
            //do something
        } catch (IllegalArgumentException exception) {
            //handle the case where string is not valid UUID
            return false;
        }

    }

    /**
     * Retrive a String text from a file.
     *
     * @param filePath File Path
     * @return String text of the given File URL
     * @throws IOException File does not exists
     */
    public static String readTextFromFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    private static final OkHttpClient HTTP_CLIENT_INSTANCE = new OkHttpClient();
    public static final MediaType JSON = MediaType.parse("application/json");

    /**
     * Execute an HTTP request based on the args
     *
     * @param httpMethod Any HttpMethod name in string format
     * @param urlParam   Endpoint URL
     * @param header     Requests Header
     * @param bodyParams Requests Body
     * @return String response for the request
     * @throws IOException HTTP Fail
     */

    public static void updateEnvVariables(String name, String val) throws ReflectiveOperationException {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).put(name, val);
    }

}
