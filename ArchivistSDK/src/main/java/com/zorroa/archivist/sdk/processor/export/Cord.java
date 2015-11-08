package com.zorroa.archivist.sdk.processor.export;

/**
 * A Cord is a wire leading from a @see "com.zorroa.archivist.sdk.processor.export.Port"
 * that can plug into a @see "com.zorroa.archivist.sdk.processor.export.Socket".
 *
 * Data leaving a port always travels through a Cord.
 */
public class Cord<T> {

    private Port<T> port;
    private Socket<T> socket;

    public Cord(Port<T> port) {
        this.port = port;
    }

    /**
     * Connect this Cord to the given Socket.
     *
     * @param socket
     */
    public void connect(Socket<T> socket) {
        if (socket == this.socket) {
            return;
        }
        this.socket = socket;
        socket.connect(this);
    }

    /**
     * Return the Socket this Cord is connected to.
     *
     * @return Socket
     */
    public Socket<T> getSocket() {
        return socket;
    }

    /**
     * Return the Port this Cord is leading from.
     *
     * @return
     */
    public Port<T> getPort() {
        return port;
    }

}
