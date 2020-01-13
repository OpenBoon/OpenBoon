package com.zorroa.zmlp.sdk.app;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;

public abstract class AbstractAppTests {

    protected MockWebServer webServer;

    @Before
    public void preSetup() throws IOException {
        webServer = new MockWebServer();
        webServer.start();
    }

    @After
    public void preTeardown() throws IOException {
        webServer.shutdown();
    }
}
