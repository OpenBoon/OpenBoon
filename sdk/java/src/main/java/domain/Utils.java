package domain;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

public class Utils {

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

    public static Boolean isValidUUI(String uuidString) {

        /*
    Return true if the given value is a valid UUID.

    Args:
        val (str): a string which might be a UUID.

    Returns:
        bool: True if UUID

         */

        try {
            UUID uuid = UUID.fromString(uuidString);
            return true;
            //do something
        } catch (IllegalArgumentException exception) {
            //handle the case where string is not valid UUID
            return false;
        }

    }

    public static String readTextFromFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    public static String executeHttpRequest(String httpMethod, String urlParam, Map<String, String> header, Map body) throws IOException {
        StringBuilder response = new StringBuilder();

        URL url = new URL(urlParam);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod(httpMethod.toUpperCase());

        // Request Header Setup
        header.entrySet().forEach((entry) -> conn.setRequestProperty(entry.getKey(), entry.getValue()));

        // Request body Setup
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
        //System.out.println("Output from Server .... \n");
        while ((output = br.readLine()) != null) {
            //System.out.println(output);
            response.append(output);
        }
        conn.disconnect();


        return response.toString();
    }
}
