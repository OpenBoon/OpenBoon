package com.zorroa.archivist.sdk.domain;

import com.zorroa.archivist.sdk.util.Json;

public class Message {

    private MessageType type;
    private String payload;

    public Message(MessageType type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = Json.serializeToString(payload);
    }

    public Message() { }

    public MessageType getType() {
        return type;
    }

    public Message setType(MessageType type) {
        this.type = type;
        return this;
    }

    public String getPayload() {
        return payload;
    }

    public Message setPayload(String payload) {
        this.payload = payload;
        return this;
    }

    public Message setPayload(Object payload) {
        this.payload = Json.serializeToString(payload);
        return this;
    }

    public String toString() {
        return String.format("%s\t%s", type.toString(), payload.replace('\n', ' '));
    }


}
