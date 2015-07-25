package com.zorroa.archivist.service;

import com.google.common.collect.Sets;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.Message;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;
import com.zorroa.archivist.domain.Session;
import com.zorroa.archivist.event.EventServerHandler;
import com.zorroa.archivist.repository.RoomDao;
import com.zorroa.archivist.repository.SessionDao;
import org.elasticsearch.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.session.SessionCreationEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoomServiceImpl implements RoomService {

    private static final Logger logger = LoggerFactory.getLogger(RoomServiceImpl.class);

    @Autowired
    RoomDao roomDao;

    @Autowired
    SessionDao sessionDao;

    @Autowired
    EventServerHandler eventServerHandler;

    @Override
    public Room get(long id) {
        return roomDao.get(id);
    }

    @Override
    public Room create(RoomBuilder bld) {
        Room room = roomDao.create(bld);
        return room;
    }

    @Override
    public Room getActiveRoom(Session session) {
        return roomDao.get(session);
    }

    @Override
    public boolean join(Room room, Session session) {
        if (logger.isDebugEnabled()) {
            logger.debug("Session {} is joining room {}", session, room);
        }
        return roomDao.join(room, session);
    }

    @Override
    public Room get(Session session) {
        /*
         * Handle the case where the session is not in a room.
         */
        return roomDao.get(session);
    }

    @Override
    public void sendToRoom(Room room, Message message) {
        /*
         * If the room is null just log it and move on.
         */
        if (room == null) {
            logger.warn("The current session {} is not in a room.", SecurityUtils.getSessionId());
            return;
        }

        if (message.getPayload() == null) {
            logger.warn("The current session {} has a null message payload", SecurityUtils.getSessionId());
            return;
        }

        logger.info("Sending: {} to active room", message.toString());

        Set<String> cookies = Sets.newHashSet();
        sessionDao.getAll(room).forEach(s -> cookies.add(s.getCookieId()));
        eventServerHandler.send(cookies, message.toString());
    }

    @Override
    public List<Room> getAll(Session session) {
        return roomDao.getAll(session);
    }
}
