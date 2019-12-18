package domain;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
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

    /**
     * Execute an HTTP request based on the args
     *
     * @param httpMethod Any HttpMethod name in string format
     * @param urlParam   Endpoint URL
     * @param header     Requests Header
     * @param body       Requests Body
     * @return String response for the request
     * @throws IOException HTTP Fail
     */

    private static final OkHttpClient HTTP_CLIENT_INSTANCE = new OkHttpClient();
    public static final MediaType JSON = MediaType.parse("application/json");

    public static String executeHttpRequest(String httpMethod, String urlParam, Map<String, String> header, Map bodyParams) throws IOException {

        // json formatted data
        // Request body Setup
        bodyParams = Optional.ofNullable(bodyParams).orElse(new HashMap());
        String json = new ObjectMapper().writeValueAsString(bodyParams);

        // json request body
        RequestBody body = RequestBody.create(JSON, json);

        Request.Builder builder = new Request.Builder();

        header.entrySet().forEach((entry) -> {
            builder.addHeader(entry.getKey(), entry.getValue());
        });

        Request request = builder
                .url(urlParam)
                .method(httpMethod.toUpperCase(), body)
                .build();

        try (Response response = Utils.HTTP_CLIENT_INSTANCE.newCall(request).execute()) {

            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            // Get response body
            return response.body().string();

        }

    }

    public static void updateEnvVariables(String name, String val) throws ReflectiveOperationException {

        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).put(name, val);

    }

}
