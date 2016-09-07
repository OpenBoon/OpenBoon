package com.zorroa.archivist.domain;

/**
 * Created by chambers on 4/8/16.
 */
public class Note {

    private String id;
    private User user;
    private int userId;
    private String text;
    private String asset;
    private long timeCreated;

    public String getId() {
        return id;
    }

    public Note setId(String id) {
        this.id = id;
        return this;
    }

    public String getText() {
        return text;
    }

    public Note setText(String text) {
        this.text = text;
        return this;
    }

    public User getUser() {
        return user;
    }

    public Note setUser(User user) {
        this.user = user;
        return this;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public Note setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Note setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public String getAsset() {
        return asset;
    }

    public Note setAsset(String asset) {
        this.asset = asset;
        return this;
    }
}
