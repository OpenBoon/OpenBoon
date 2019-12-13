package domain;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
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

/*    public static String executeHttpRequest(String httpMethod, String urlParam, Map<String, String> header, Map body) throws IOException {
        StringBuilder response = new StringBuilder();

        URL url = new URL(urlParam);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod(httpMethod.toUpperCase());

        // Request Header Setup
        header.entrySet().forEach((entry) -> conn.setRequestProperty(entry.getKey(), entry.getValue()));

        // Request body Setup
        body = Optional.ofNullable(body).orElse(new HashMap());
        String input = new ObjectMapper().writeValueAsString(body);

        OutputStream os = conn.getOutputStream();
        os.write(input.getBytes());
        os.flush(); // request

        if (conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
            throw new RuntimeException("Failed : HTTP error code : "
                    + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));

        String output;
        while ((output = br.readLine()) != null) {
            response.append(output);
        }
        conn.disconnect();


        return response.toString();
    }*/


    private static final OkHttpClient HTTP_CLIENT_INSTANCE = new OkHttpClient();

    public static String executeHttpRequest(String httpMethod, String urlParam, Map<String, String> header, Map bodyParams) throws IOException {

        // json formatted data
        // Request body Setup
        bodyParams = Optional.ofNullable(bodyParams).orElse(new HashMap());
        String json = new ObjectMapper().writeValueAsString(bodyParams);

        // json request body
        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );

        Request.Builder builder = new Request.Builder();

        header.entrySet().forEach((entry) -> {
            builder.addHeader(entry.getKey(), entry.getValue());
        });

        Request request = builder
                .url(urlParam)
                .method(httpMethod, body)
                .build();

        try (Response response = Utils.HTTP_CLIENT_INSTANCE.newCall(request).execute()) {

            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            // Get response body
            return response.body().string();

        }

    }

}
