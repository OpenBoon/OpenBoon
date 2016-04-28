package com.zorroa.archivist.service;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.TestMessagingClient;
import com.zorroa.archivist.sdk.config.ApplicationProperties;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 12/14/15.
 */
public class MessageServiceTests extends ArchivistApplicationTests {

    @Autowired
    SessionRegistry sessionRegistry;

    @Autowired
    ApplicationProperties applicationProperties;

    @Value("${archivist.events.port}")
    private int port;

    TestMessagingClient client;
    Room room;

    @Before
    public void init() throws Exception {

        SslContext sslContext = null;
        if (applicationProperties.getBoolean("archivist.events.ssl")) {
            sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        }

        client = new TestMessagingClient(port, sslContext);
        room = roomService.create(new RoomBuilder("test"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        sessionRegistry.registerNewSession(request.getSession().getId(), SecurityUtils.getUser());

        roomService.join(room, userService.getSession(SecurityUtils.getCookieId()));
        client.sendSession(SecurityUtils.getCookieId());
        Thread.sleep(100);
        assertEquals("OK", client.pop());
    }

    @After
    public void shutdown() {
        client.shutdown();
    }

    @Test
    public void broadcast() throws Exception {
        messagingService.broadcast(new Message("TEST", "foo"));
        assertEquals("TEST\tfoo", client.pop());
    }

    @Test
    public void testSendToActiveRoom() throws Exception {
        messagingService.sendToActiveRoom(new Message("TEST", "foo"));
        assertEquals("TEST\tfoo", client.pop());
    }

    @Test
    public void testSendToActiveRoomWithNoRoom() throws Exception {
        messagingService.sendToActiveRoom(new Message("TEST", "foo"));
        assertEquals("TEST\tfoo", client.pop());
    }


    @Test
    public void testSendToRoom() throws Exception {
        messagingService.sendToRoom(room, new Message("TEST", "foo"));
        assertEquals("TEST\tfoo", client.pop());
    }

    @Test
    public void testSendToActiveSession() throws Exception {
        messagingService.sendToActiveSession(new Message("TEST", "foo"));
        assertEquals("TEST\tfoo", client.pop());
    }

    @Test
    public void testSendSession() throws Exception {
        Session session = userService.getSession(SecurityUtils.getCookieId());
        messagingService.sendToSession(session, new Message("TEST", "foo"));
        assertEquals("TEST\tfoo", client.pop());
    }

    @Test
    public void testSendToUser() throws Exception {
        User user = SecurityUtils.getUser();
        messagingService.sendToUser(user, new Message("TEST", "foo"));
        assertEquals("TEST\tfoo", client.pop());
    }

    @Test
    public void testSendToAciveUser() throws Exception {
        messagingService.sendToActiveUser(new Message("TEST", "foo"));
        assertEquals("TEST\tfoo", client.pop());
    }
}
