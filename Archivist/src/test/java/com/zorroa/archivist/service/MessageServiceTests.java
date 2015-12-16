package com.zorroa.archivist.service;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.TestMessagingClient;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.MessagingService;
import com.zorroa.archivist.sdk.service.UserService;
import com.zorroa.archivist.security.SecurityUtils;
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
    UserService userService;

    @Autowired
    RoomService roomService;

    @Autowired
    MessagingService messagingService;

    @Autowired
    SessionRegistry sessionRegistry;

    @Value("${archivist.events.port}")
    private int port;

    TestMessagingClient client;
    Room room;

    @Before
    public void init() throws Exception {
        client = new TestMessagingClient(port);
        room = roomService.create(new RoomBuilder("test"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        sessionRegistry.registerNewSession(request.getSession().getId(), SecurityUtils.getUser());

        roomService.join(room, userService.getSession(SecurityUtils.getSessionId()));
        client.sendSession(SecurityUtils.getSessionId());
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
        Session session = userService.getSession(SecurityUtils.getSessionId());
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
