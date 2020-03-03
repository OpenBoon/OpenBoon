package com.zorroa.zmlp.client;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class UtilsTests {

    public static void updateEnvVariables(String name, String val) {
        try {
            Map<String, String> env = System.getenv();
            Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            ((Map<String, String>) field.get(env)).put(name, val);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to update test environment", e);
        }
    }

    public static  String getMockData(String name) {
        try {
            return new String(Files.readAllBytes(Paths.get("src/test/resources/" + name + ".json")));
        } catch (IOException e) {
            throw new RuntimeException("Failed to find mock data: " + name, e);
        }
    }

}
