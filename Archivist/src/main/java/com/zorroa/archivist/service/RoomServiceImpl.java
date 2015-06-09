package com.zorroa.archivist.service;

import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.Message;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;
import com.zorroa.archivist.event.EventServerHandler;
import com.zorroa.archivist.repository.RoomDao;

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
    public void setActiveRoom(Room room) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        mapSessionToRoom.put(sessionId, room.getId());
    }

    @Override
    public Room getActiveRoom() {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        return roomDao.get(mapSessionToRoom.get(sessionId));
    }

    @Override
    public void sendToActiveRoom(Message message) {
        logger.info("Sending: {} to active room", message.toString());
        Room room = getActiveRoom();

        Set<String> sessions = Sets.newHashSet();
        for (Map.Entry<String, Long> entry: mapSessionToRoom.entrySet()) {
            if (entry.getValue().equals(room.getId())) {
                sessions.add(entry.getKey());
            }
        }
        eventServerHandler.send(sessions, message.toString());
    }
}
