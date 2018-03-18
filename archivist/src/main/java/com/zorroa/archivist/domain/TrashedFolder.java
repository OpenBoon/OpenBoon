package com.zorroa.archivist.domain;

import com.zorroa.sdk.search.AssetSearch;

import java.util.Map;
import java.util.UUID;

/**
 * Created by chambers on 12/2/16.
 */
public class TrashedFolder {

    private UUID id;
    private String opId;
    private UUID folderId;
    private UUID parentId;
    private String name;
    private UserBase user;
    private UserBase userDeleted;
    private long timeCreated;
    private long timeModified;
    private long timeDeleted;
    private boolean recursive;
    private Acl acl;
    private AssetSearch search;
    private Map<String, Object> attrs;

    public UserBase getUserDeleted() {
        return userDeleted;
    }

    public TrashedFolder setUserDeleted(UserBase userDeleted) {
        this.userDeleted = userDeleted;
        return this;
    }

    public UUID getFolderId() {
        return folderId;
    }

    public TrashedFolder setFolderId(UUID folderId) {
        this.folderId = folderId;
        return this;
    }

    public UUID getId() {
        return id;
    }

    public TrashedFolder setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getOpId() {
        return opId;
    }

    public TrashedFolder setOpId(String opId) {
        this.opId = opId;
        return this;
    }

    public UUID getParentId() {
        return parentId;
    }

    public TrashedFolder setParentId(UUID parentId) {
        this.parentId = parentId;
        return this;
    }

    public String getName() {
        return name;
    }

    public TrashedFolder setName(String name) {
        this.name = name;
        return this;
    }

    public UserBase getUser() {
        return user;
    }

    public TrashedFolder setUser(UserBase user) {
        this.user = user;
        return this;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public TrashedFolder setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
        return this;
    }

    public long getTimeModified() {
        return timeModified;
    }

    public TrashedFolder setTimeModified(long timeModified) {
        this.timeModified = timeModified;
        return this;
    }

    public long getTimeDeleted() {
        return timeDeleted;
    }

    public TrashedFolder setTimeDeleted(long timeDeleted) {
        this.timeDeleted = timeDeleted;
        return this;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public TrashedFolder setRecursive(boolean recursive) {
        this.recursive = recursive;
        return this;
    }

    public Acl getAcl() {
        return acl;
    }

    public TrashedFolder setAcl(Acl acl) {
        this.acl = acl;
        return this;
    }

    public AssetSearch getSearch() {
        return search;
    }

    public TrashedFolder setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public TrashedFolder setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
        return this;
    }
}
