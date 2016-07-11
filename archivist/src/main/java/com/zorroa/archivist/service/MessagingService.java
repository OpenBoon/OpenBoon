package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.User;
import com.zorroa.sdk.domain.Message;
import com.zorroa.sdk.domain.Room;
import com.zorroa.sdk.domain.Session;

/**
 * MessagingService is used to send Asynchronous messages to clients
 * connected to the event port.
 */
public interface MessagingService {

    /**
     * Send a message to the active sessions room.
     *
     * @param message
     */
    void sendToActiveRoom(Message message);

    /**
     * Send a message to a given room.
     *
     * @param room
     * @param message
     */
    void sendToRoom(Room room, Message message);

    /**
     * Send a message to the active session.
     *
     * @param message
     */
    void sendToActiveSession(Message message);

    /**
     * Send a message to the given session.
     *
     * @param session
     * @param message
     */

    void sendToSession(Session session, Message message);

    /**
     * Send a message to all sessions of the active user.
     *
     * @param message
     */
    void sendToActiveUser(Message message);

    /**
     * Send a message to all sessions for a given user.
     *
     * @param user
     * @param message
     */
    void sendToUser(User user, Message message);

    /**
     * Send a message to everyone.
     *
     * @param message
     */
    void broadcast(Message message);
}
