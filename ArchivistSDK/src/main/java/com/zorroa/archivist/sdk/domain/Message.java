package com.zorroa.archivist.sdk.domain;

import com.zorroa.archivist.sdk.util.Json;

public class Message {

    private String type;
    private String payload;

    public Message(MessageType type, String payload) {
        this.type = type.toString();
        this.payload = payload;
    }

    public Message(MessageType type, Object payload) {
        this.type = type.toString();
        this.payload = Json.serializeToString(payload);
    }

    public Message(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public Message(String type, Object payload) {
        this.type = type;
        this.payload = Json.serializeToString(payload);
    }

    public Message() { }

    public String getType() {
        return type;
    }

    public Message setType(MessageType type) {
        this.type = type.toString();
        return this;
    }

    public Message setType(String type) {
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

    public String serialize(String endMessage) {
        StringBuilder sb = new StringBuilder(payload.length() + 64);
        sb.append(type.toString());
        sb.append("\t");
        sb.append(payload.replace('\n', ' '));
        if (endMessage != null) {
            sb.append(endMessage);
        }
        return sb.toString();
    }

}
