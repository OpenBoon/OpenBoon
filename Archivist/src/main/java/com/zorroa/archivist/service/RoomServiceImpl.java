package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.repository.RoomDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.exception.MalformedDataException;
import com.zorroa.archivist.tx.TransactionEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {

    private static final Logger logger = LoggerFactory.getLogger(RoomServiceImpl.class);

    @Autowired
    RoomDao roomDao;

    @Autowired
    UserService userService;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    MessagingService messagingService;

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
        Session session = userService.getActiveSession();
        if (session == null) {
            return null;
        }
        return roomDao.get(session);
    }

    @Override
    public boolean join(Room room) {
        return join(room, userService.getActiveSession());
    }

    @Override
    public boolean join(Room room, Session session) {
        if (session == null) {
            return false;
        }
        if (roomDao.isInRoom(room, session)) {
            return false;
        }
        boolean result = roomDao.join(room, session);
        if (result) {
            transactionEventManager.afterCommit(()->
                    messagingService.sendToRoom(room, new Message(MessageType.ROOM_USER_JOINED,
                            ImmutableMap.of("userId", session.getUserId()))));
        }
        return result;
    }

    @Override
    public boolean leave(Room room) {
        return leave(room, userService.getActiveSession());
    }

    @Override
    public boolean leave(Room room, Session session) {
        if (session == null || room == null) {
            return false;
        }
        boolean result = roomDao.leave(room, session);
        if (result) {
            transactionEventManager.afterCommit(()->
                    messagingService.sendToRoom(room, new Message(MessageType.ROOM_USER_LEFT,
                            ImmutableMap.of("userId", session.getUserId()))));
        }
        return result;
    }

    @Override
    public List<Room> getAll() {
        return roomDao.getAll();
    }

    @Override
    public boolean update(Room room, RoomUpdateBuilder updater) {
        boolean result = roomDao.update(room, updater);
        transactionEventManager.afterCommit(()-> {
            messagingService.sendToRoom(getActiveRoom(), new Message(MessageType.ROOM_UPDATED,
                    ImmutableMap.of("roomId", room.getId())));
        });
        return result;
    }

    @Override
    public boolean delete(Room room) {
        return roomDao.delete(room);
    }

    public int setSelection(Room room, Set<String> selection) {
        if (selection.size() > SELECTION_MAX_SIZE) {
            throw new MalformedDataException(String.format(
                    "The selection is too large, maximum allowed size: '%d', size: '%d'",
                        SELECTION_MAX_SIZE, selection.size()));
        }
        if (room != null) {
            int version = roomDao.setSelection(room, selection);
            transactionEventManager.afterCommit(() -> {
                messagingService.sendToRoom(room, new Message(MessageType.ROOM_SELECTION_UPDATE,
                        ImmutableMap.of("roomId", room.getId(), "version", version, "selection", selection)));
            });
            return version;
        }
        else {
            messagingService.sendToActiveRoom(new Message(MessageType.ROOM_SELECTION_UPDATE,
                    ImmutableMap.of("version", -1, "selection", selection)));
            return -1;
        }
    }

    public int setSearch(Room room, AssetSearch search) {
        if (room != null) {
            int version = roomDao.setSearch(room, search);
            transactionEventManager.afterCommit(() -> {
                messagingService.sendToRoom(room, new Message(MessageType.ROOM_SEARCH_UPDATE,
                        ImmutableMap.of("roomId", room.getId(), "version", version, "search", search)));
            });
            return version;
        }
        else {
            transactionEventManager.afterCommit(() -> {
                messagingService.sendToActiveRoom(new Message(MessageType.ROOM_SEARCH_UPDATE,
                        ImmutableMap.of("version", -1, "search", search)));
            });
            return -1;
        }
    }

    public Set<String> getSelection(Room room) {
        return roomDao.getSelection(room);
    }

    public AssetSearch getSearch(Room room) {
        return roomDao.getSearch(room);
    }

    public SharedRoomState getSharedState(Room room) {
        return roomDao.getSharedState(room);
    }
}
