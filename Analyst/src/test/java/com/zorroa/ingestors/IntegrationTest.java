/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.ingestors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.sdk.domain.AssetSearch;
import com.zorroa.archivist.sdk.util.Json;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.*;
import java.net.*;
import java.util.Map;

public class IntegrationTest {
    static Process archivistProcess;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    @BeforeClass
    public static void authenticate() throws IOException, InterruptedException {
        // Initialize cookies
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

        // Set the authentication for all future requests
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        "admin", "admin".toCharArray());
            }
        });

        startArchivist();

        // Login
        String login = "http://localhost:8066/api/v1/login";
        HttpURLConnection httpConnection = (HttpURLConnection) new URL(login).openConnection();
        httpConnection.setRequestMethod("POST");
        int responseCode = httpConnection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            System.out.print("Cannot log in to Archivist");
        }

        ingest();
    }

    @AfterClass
    public static void terminateArchivist() throws IOException {
        if (archivistProcess != null) {
            archivistProcess.destroyForcibly();
        }
    }

    private static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    private static Boolean waitForHealthyArchivst(int maxAttempts) throws InterruptedException, IOException {
        Boolean isStarted = false;
        for (int attempts = 0; attempts < maxAttempts; ++attempts) {
            if (!isStarted && attempts > 0) {
                System.out.println("Waiting for Archivist to start...");
                Thread.sleep(3000);
            }
            try {
                String response = sendJson("health", "GET", null);
                Map<String, Object> json = Json.Mapper.readValue(response,
                        new TypeReference<Map<String, Object>>() {});
                String status = (String) json.get("status");
                isStarted = status.equals("UP");
            } catch (ConnectException e) {
                System.out.println("Archivist not yet healthy: " + e);
            }
        }
        return isStarted;
    }

    private static void startArchivist() throws IOException, InterruptedException {
        if (waitForHealthyArchivst(1)) {
            System.out.println(ANSI_RED + "EXPERT FEATURE: Testing Ingestors with already running archivist " + ANSI_RESET + " -- make sure to kill or clean the database before testing! If you are confused, kill your Archivist before running Ingestor tests.");
            return;
        }

        ProcessBuilder pb = new ProcessBuilder();
        String pwd = System.getProperty("user.dir");;
        File cwd = new File(pwd + "/Database");
        if (!cwd.exists()) {
            cwd.mkdir();
        } else {
            deleteDir(cwd);
            cwd.mkdir();
        }
        pb.directory(cwd);
        String archivistSrc = pwd + "/../Archivist/target/archivist.jar";
        File archivist = new File(archivistSrc);
        if (!archivist.exists()) {
            String archivistSys = "/Library/Application Support/Zorroa/java/archivist.jar";
            archivist = new File(archivistSys);
            if (!archivist.exists()) {
                System.err.println(ANSI_RED + "Cannot find archivist.jar in Archivist source (" + archivistSrc + ") or system-wide Zorroa directory (" + archivistSys + ")" + ANSI_RESET);
                return;
            }
        }
        Map<String, String> env = pb.environment();
        String cmd =  "/usr/bin/java -Djava.class.path=" + pwd + "/lib/face -Djava.library.path=" + pwd +"/target/jni:" + pwd + "/lib/face -jar \"" + archivist.getAbsolutePath() + "\"";
        System.out.println("Starting Archivist with command: " + cmd);
        pb.command("/bin/bash", "-c", cmd);
        env.put("ZORROA_OPENCV_MODEL_PATH", pwd + "/models");
        env.put("ZORROA_SITE_PATH", pwd + "/target");
        File log = new File("Database/archivist.log");
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        archivistProcess = pb.start();
        assert pb.redirectInput() == ProcessBuilder.Redirect.PIPE;
        assert pb.redirectOutput().file() == log;
        assert archivistProcess.getInputStream().read() == -1;

        Boolean isStarted = waitForHealthyArchivst(20);
        if (!isStarted) {
            System.out.println(ANSI_RED + "Timed out waiting for Archivist to start" + ANSI_RESET);
            terminateArchivist();
        } else {
            System.out.println("Archivist is healthy");
        }

    }

    private static void ingest() throws IOException, InterruptedException {
        String allProcessorPipeline = "{ \"name\" : \"standard\", \"processors\" : [ { \"klass\" : \"com.zorroa.archivist.processors.AssetMetadataProcessor\" }, { \"klass\" : \"com.zorroa.archivist.processors.ProxyProcessor\" }, { \"klass\" : \"com.zorroa.ingestors.FaceIngestor\" }, { \"klass\" : \"com.zorroa.ingestors.CaffeIngestor\" }, { \"klass\" : \"com.zorroa.ingestors.LogoIngestor\"}, { \"klass\" : \"com.zorroa.ingestors.RetrosheetIngestor\"} ] }";
        String response = sendJson("api/v1/pipelines/1", "PUT", allProcessorPipeline);
        System.out.println(response);
        String pwd = System.getProperty("user.dir");;
        String ingestDir = pwd + "/src/test/resources/images";
        response = sendJson("api/v1/ingests", "POST", "{ \"path\": \"" + ingestDir + "\", \"assetWorkerThreads\" : 1 }");
        System.out.println(response);
        response = sendJson("api/v1/ingests/1/_execute", "POST", null);
        System.out.println(response);
        Boolean isRunning = true;
        final int maxAttempts = 20;
        for (int attempts = 0; isRunning && attempts < maxAttempts; ++attempts) {
            response = sendJson("api/v1/ingests/1", "GET", null);
            Map<String, Object> json = Json.Mapper.readValue(response,
                    new TypeReference<Map<String, Object>>() {});
            String state = (String) json.get("state");
            isRunning = state.equals("Running");
            if (isRunning) {
                System.out.println("Waiting for ingest to complete...");
                Thread.sleep(3000);
            }
        }
        if (isRunning) {
            System.err.println(ANSI_RED + "Timed out waiting for ingest to complete" + ANSI_RESET);
        } else {
            System.out.println("Ingest completed.");
        }
    }

    private static String sendJson(String endpoint, String method, String body) throws IOException {
        String url = "http://localhost:8066/" + endpoint;
        String charset = "UTF-8";  // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()
        HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();
        httpConnection.setDoOutput(true);
        httpConnection.setRequestMethod(method);
        if (body != null) {
            httpConnection.setRequestProperty("Content-Type", "application/json");
            OutputStreamWriter writer = new OutputStreamWriter(httpConnection.getOutputStream());
            writer.write(body);
            writer.flush();
        }
        int responseCode = httpConnection.getResponseCode();
        String response = "";
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader br = new BufferedReader(new InputStreamReader((httpConnection.getInputStream())));
            String output;
            while ((output = br.readLine()) != null) {
                response += output;
            }
        }
        return response;
    }

    protected static Integer countQuery(String query) throws IOException {
        AssetSearch assetSearch = new AssetSearch().setQuery(query);
        String countResponse = sendJson("api/v2/assets/_count", "POST", Json.serializeToString(assetSearch));
        System.out.println(countResponse);
        Map<String, Object> json = Json.Mapper.readValue(countResponse,
                new TypeReference<Map<String, Object>>() {});
        return (Integer) json.get("count");
    }

}
