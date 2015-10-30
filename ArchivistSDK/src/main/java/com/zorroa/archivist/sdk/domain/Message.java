package com.zorroa.archivist.sdk.domain;

public class Message {

    private MessageType type;
    private String payload;

    public Message(MessageType type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public Message() { }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String toString() {
        return String.format("%s\t%s", type.toString(), payload.replace('\n', ' '));
    }


}
