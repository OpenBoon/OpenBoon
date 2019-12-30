package com.zorroa.zmlp.sdk;

import java.lang.reflect.Field;
import java.util.Map;

public class TestUtils {

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
}
