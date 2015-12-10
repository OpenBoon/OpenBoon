package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.zorroa.archivist.event.EventServerHandler;
import com.zorroa.archivist.repository.RoomDao;
import com.zorroa.archivist.repository.SessionDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.exception.MalformedDataException;
import com.zorroa.archivist.sdk.service.RoomService;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {

    private static final Logger logger = LoggerFactory.getLogger(RoomServiceImpl.class);

    @Autowired
    RoomDao roomDao;

    @Autowired
    SessionDao sessionDao;

    @Autowired
    EventServerHandler eventServerHandler;

    @Autowired
    TransactionEventManager transactionEventManager;

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
    public Room getActiveRoom() {
        Session session = sessionDao.get(SecurityUtils.getSessionId());
        if (session != null) {
            return roomDao.get(session);
        }
        else {
            return null;
        }
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
    public void sendToActiveRoom(Message message) {
        sendToRoom(getActiveRoom(), message);
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

    @Override
    public boolean update(Room room, RoomUpdateBuilder updater) {
        return roomDao.update(room, updater);
    }

    @Override
    public boolean delete(Room room) {
        return roomDao.delete(room);
    }

    /**
     * The maximum size of a selection that can be propagated to a room.  This will
     * probably need to be tweaked/tested for very large selections, but 1024 gives
     * us roughly a 32k packet size.
     */
    private static final int SELECTION_MAX_SIZE = 1024;

    public int setSelection(@Nullable Room room, Set<String> selection) {
        if (room == null) {
            return -1;
        }
        if (selection.size() > SELECTION_MAX_SIZE) {
            throw new MalformedDataException(String.format(
                    "The selection is too large, maximum allowed size: '%d' ", SELECTION_MAX_SIZE));
        }
        int version = roomDao.setSelection(room, selection);
        transactionEventManager.afterCommit(()-> {
            sendToRoom(getActiveRoom(), new Message(MessageType.ROOM_SELECTION_UPDATE,
                    ImmutableMap.of("roomId", room.getId(), "version", version, "selection", selection)));
        });
        return version;
    }

    public int setSearch(@Nullable Room room, AssetSearchBuilder search) {
        if (room == null) {
            return -1;
        }

        int version = roomDao.setSearch(room, search);
        transactionEventManager.afterCommit(()-> {
            sendToRoom(getActiveRoom(), new Message(MessageType.ROOM_SEARCH_UPDATE,
                    ImmutableMap.of("roomId", room.getId(), "version", version, "search", search)));
        });
        return version;
    }

    public Set<String> getSelection(Room room) {
        return roomDao.getSelection(room);
    }

    public AssetSearchBuilder getSearch(Room room) {
        return roomDao.getSearch(room);
    }

    public SharedRoomState getSharedState(Room room) {
        return roomDao.getSharedState(room);
    }
}
