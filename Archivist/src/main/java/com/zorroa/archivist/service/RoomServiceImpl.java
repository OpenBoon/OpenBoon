package com.zorroa.archivist.service;

import com.google.common.collect.Sets;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.Message;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;
import com.zorroa.archivist.event.EventServerHandler;
import com.zorroa.archivist.repository.RoomDao;
import org.elasticsearch.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    EventServerHandler eventServerHandler;

    private Map<String, Long> mapSessionToRoom = Maps.newHashMap();

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
    public void setActiveRoom(String sessionId, Room room) {
        logger.info("Setting active room {} for session {}", room, sessionId);
        mapSessionToRoom.put(sessionId, room.getId());
    }

    @Override
    public Room getActiveRoom(String sessionId) {
        /*
         * Handle the case where the session is not in a room.
         */
        Long roomId = mapSessionToRoom.get(sessionId);
        if (roomId == null) {
            return null;
        }
        return roomDao.get(roomId);
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

        Set<String> sessions = Sets.newHashSet();
        for (Map.Entry<String, Long> entry: mapSessionToRoom.entrySet()) {
            if (entry.getValue().equals(room.getId())) {
                sessions.add(entry.getKey());
            }
        }
        eventServerHandler.send(sessions, message.toString());
    }

    @Override
    public List<Room> getAll() {
        return roomDao.getAll();
    }
}
