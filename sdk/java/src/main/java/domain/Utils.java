package domain;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
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
}
