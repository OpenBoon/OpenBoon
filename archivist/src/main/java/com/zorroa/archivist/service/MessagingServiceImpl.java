package com.zorroa.archivist.service;

import com.zorroa.archivist.event.EventServerHandler;
import com.zorroa.archivist.repository.SessionDao;
import com.zorroa.archivist.sdk.domain.Message;
import com.zorroa.archivist.sdk.domain.Room;
import com.zorroa.archivist.sdk.domain.Session;
import com.zorroa.archivist.sdk.domain.User;
import com.zorroa.archivist.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by chambers on 12/14/15.
 */
@Component
public class MessagingServiceImpl implements MessagingService {

    private static final Logger logger = LoggerFactory.getLogger(MessagingServiceImpl.class);

    @Autowired
    RoomService roomService;

    @Autowired
    UserService userService;

    @Autowired
    EventServerHandler eventServerHandler;

    @Autowired
    SessionDao sessionDao;

    @Override
    public void broadcast(Message message) {
        eventServerHandler.broadcast(message);
    }

    @Override
    public void sendToActiveRoom(Message message) {
        sendToRoom(roomService.getActiveRoom(), message);
    }

    @Override
    public void sendToRoom(Room room, Message message) {
        /*
         * If the user has no room, then just send to their session.
         */
        if (room == null) {
            sendToActiveSession(message);
            return;
        }

        if (message.getPayload() == null) {
            logger.warn("The current session {} has a null message payload", SecurityUtils.getCookieId());
            return;
        }

        eventServerHandler.send(sessionDao.getAll(room), message);
    }

    @Override
    public void sendToActiveSession(Message message) {
        sendToSession(userService.getActiveSession(), message);
    }

    @Override
    public void sendToSession(Session session, Message message) {
        /*
         * If the room is null just log it and move on.
         */
        if (session == null) {
            logger.warn("The current session {} is null.", SecurityUtils.getCookieId());
            return;
        }

        if (message.getPayload() == null) {
            logger.warn("Not sending null message payload to {}", SecurityUtils.getCookieId());
            return;
        }

        logger.debug("Sending: {} to active room", message);
        eventServerHandler.send(session, message);
    }

    @Override
    public void sendToActiveUser(Message message) {

        User user = SecurityUtils.getUser();
        if (user == null) {
            return;
        }

        sendToUser(user, message);
    }

    @Override
    public void sendToUser(User user, Message message) {
        List<Session> sessions = sessionDao.getAll(user);
        eventServerHandler.send(sessions, message);
    }
}
