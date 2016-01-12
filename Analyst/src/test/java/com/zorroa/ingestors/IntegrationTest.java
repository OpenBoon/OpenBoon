/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.ingestors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.io.CharStreams;
import com.zorroa.archivist.sdk.domain.AssetSearch;
import com.zorroa.archivist.sdk.domain.IngestPipelineBuilder;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.util.Json;
import org.apache.commons.codec.Charsets;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.util.Map;

public class IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);

    static final String ANSI_RESET = "\u001B[0m";
    static final String ANSI_RED = "\u001B[31m";

    static Process archivistProcess;
    static HttpClient httpClient;
    static CookieStore cookieStore;

    @BeforeClass
    public static void authenticate() throws IOException, InterruptedException {
        httpClient = buildHttpsClient();
        startArchivist();
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
                logger.info("Waiting for Archivist to start...");
                Thread.sleep(3000);
            }
            try {
                String response = sendJson("health", "GET", null);
                Map<String, Object> json = Json.Mapper.readValue(response,
                        new TypeReference<Map<String, Object>>() {});
                String status = (String) json.get("status");
                isStarted = status.equals("UP");
                break;
            } catch (ConnectException e) {
                logger.info("Archivist not yet healthy: " + e);
            }
        }
        return isStarted;
    }

    private static void startArchivist() throws IOException, InterruptedException {
        if (waitForHealthyArchivst(1)) {
            logger.warn(ANSI_RED + "EXPERT FEATURE: Testing Ingestors with already running archivist " + ANSI_RESET + " -- make sure to kill or clean the database before testing! If you are confused, kill your Archivist before running Ingestor tests.");
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
                logger.error(ANSI_RED + "Cannot find archivist.jar in Archivist source (" + archivistSrc + ") or system-wide Zorroa directory (" + archivistSys + ")" + ANSI_RESET);
                return;
            }
        }
        Map<String, String> env = pb.environment();
        String cmd =  "/usr/bin/java  -Djava.class.path=" + pwd + "/lib/face -Djava.library.path=" + pwd +"/target/jni:" + pwd + "/lib/face -jar \"" + archivist.getAbsolutePath() + "\" --logging.file=/tmp/log";
        logger.info("Starting Archivist with command: {}", cmd);
        pb.command("/bin/sh", "-c", cmd);
        env.put("ZORROA_OPENCV_MODEL_PATH", pwd + "/models");
        env.put("ZORROA_SITE_PATH", pwd + "/target");

        archivistProcess = pb.start();

        Boolean isStarted = waitForHealthyArchivst(20);
        if (!isStarted) {
            logger.error(ANSI_RED + "Timed out waiting for Archivist to start" + ANSI_RESET);
            terminateArchivist();
        } else {
            logger.info("Archivist is healthy");
        }

    }

    static final String[] ALL_INGEST_PROCESSORS = {
            "com.zorroa.archivist.ingestors.ImageIngestor",
            "com.zorroa.archivist.ingestors.ProxyProcessor",
            "com.zorroa.vision.ingestors.FaceIngestor",
            "com.zorroa.vision.ingestors.CaffeIngestor",
            "com.zorroa.vision.ingestors.LogoIngestor",
            "com.zorroa.vision.ingestors.RetrosheetIngestor"
    };

    private static void ingest() throws IOException, InterruptedException {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("standard");
        for (String processor: ALL_INGEST_PROCESSORS) {
            builder.addToProcessors(new ProcessorFactory<IngestProcessor>(processor));
        }

        sendJson("api/v1/pipelines/1", "PUT", Json.serializeToString(builder));
        String pwd = System.getProperty("user.dir");;
        String ingestDir = pwd + "/src/test/resources/images";
        sendJson("api/v1/ingests", "POST", "{ \"path\": \"" + ingestDir + "\", \"assetWorkerThreads\" : 1 }");
        sendJson("api/v1/ingests/1/_execute", "POST", null);

        Boolean isRunning = true;
        final int maxAttempts = 20;
        for (int attempts = 0; isRunning && attempts < maxAttempts; ++attempts) {
            String response = sendJson("api/v1/ingests/1", "GET", null);
            Map<String, Object> json = Json.Mapper.readValue(response,
                    new TypeReference<Map<String, Object>>() {});
            String state = (String) json.get("state");
            isRunning = state.equals("Running");
            if (isRunning) {
                logger.info("Waiting for ingest to complete...");
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

        try {
            httpClient = buildHttpsClient();
        } catch (Exception e) {
            throw new IOException(e);
        }

        HttpRequestBase request;
        switch(method) {
            case "POST":
                request = new HttpPost("https://localhost:8066/" + endpoint);
                break;
            case "GET":
                request = new HttpGet("https://localhost:8066/" + endpoint);
                break;
            case "PUT":
                request = new HttpPut("https://localhost:8066/" + endpoint);
                break;
            default:
                throw new IOException("invalid request type:" + method);
        }

        request.addHeader("Content-type", "application/json");
        if (body != null) {
            if (!body.isEmpty() && !method.equals("GET")) {
                HttpEntity entity = new ByteArrayEntity(body.getBytes("UTF-8"));
                ((HttpEntityEnclosingRequestBase)request).setEntity(entity);
            }
        }

        logger.info("req: {} {}", request.getMethod(), request.getURI());
        HttpResponse response = httpClient.execute(request);
        logger.info("res code: {} {}", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
        String content = CharStreams.toString(new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8));
        if (content != null) {
            logger.info("rsp content: {}", content);
        }
        return content;
    }

    protected static Integer countQuery(String query) throws IOException {
        AssetSearch assetSearch = new AssetSearch().setQuery(query);
        String countResponse = sendJson("api/v2/assets/_count", "POST", Json.serializeToString(assetSearch));
        logger.info("count response: {}", countResponse);
        Map<String, Object> json = Json.Mapper.readValue(countResponse,
                new TypeReference<Map<String, Object>>() {});
        return (Integer) json.get("count");
    }

    public static HttpClient buildHttpsClient() {

        try {
            cookieStore = new BasicCookieStore();
            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", "admin");
            provider.setCredentials(AuthScope.ANY, credentials);

            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                    .build();

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslContext,
                    new AllowAllHostnameVerifier());
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sslsf)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .setDefaultCredentialsProvider(provider)
                    .setDefaultCookieStore(cookieStore)
                    .build();

            return httpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
