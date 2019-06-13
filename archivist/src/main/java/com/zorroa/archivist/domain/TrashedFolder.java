package com.zorroa.archivist.domain;

import com.zorroa.archivist.search.AssetSearch;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;
import java.util.UUID;

/**
 * Created by chambers on 12/2/16.
 */
@ApiModel(value = "Trashed Folder", description = "Folder that has been soft-deleted.")
public class TrashedFolder {

    @ApiModelProperty("UUID of the Trashed Folder")
    private UUID id;

    @ApiModelProperty("UUID of the operation that trashed the Folder.")
    private String opId;

    @ApiModelProperty("UUID of the Folder that was trashed.")
    private UUID folderId;

    @ApiModelProperty("UUID of the Folder's parent Folder.")
    private UUID parentId;

    @ApiModelProperty("Name of this Trashed Folder.")
    private String name;

    @ApiModelProperty("User this Trashed Folder belongs to.")
    private UserBase user;

    @ApiModelProperty("User that trashed the Folder.")
    private UserBase userDeleted;

    @ApiModelProperty("Time this Trashed Folder was created.")
    private long timeCreated;

    @ApiModelProperty("Time this Trashed Folder was last modified.")
    private long timeModified;

    @ApiModelProperty("Time the Folder was trashed.")
    private long timeDeleted;

    @ApiModelProperty(hidden = true)
    private boolean recursive;

    @ApiModelProperty("ACL applied the Folder.")
    private Acl acl;

    @ApiModelProperty("Asset Search used to populate the Folder.")
    private AssetSearch search;

    @ApiModelProperty("Folder's attributes.")
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
