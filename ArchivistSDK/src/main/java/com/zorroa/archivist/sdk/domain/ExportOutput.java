package com.zorroa.archivist.sdk.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.util.FileUtils;

import java.io.File;

/**
 * Exported Output
 */
public class ExportOutput implements Id {

    private int id;
    private String name;
    private int exportId;
    private int userCreated;
    private long timeCreated;
    private String path;
    private String mimeType;
    private String fileExtention;
    private boolean offline;
    private long timeOnline;
    private long timeOffline;
    private long fileSize;

    private ProcessorFactory<ExportProcessor> factory;

    public ProcessorFactory<ExportProcessor> getFactory() {
        return factory;
    }

    @JsonIgnore
    public String getFileName() {
        return FileUtils.filename(path);
    }

    @JsonIgnore
    public String getDirName() {
        return FileUtils.dirname(path);
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setFactory(ProcessorFactory<ExportProcessor> factory) {
        this.factory = factory;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public int getUserCreated() {
        return userCreated;
    }

    public void setUserCreated(int userCreated) {
        this.userCreated = userCreated;
    }

    public int getExportId() {
        return exportId;
    }

    public void setExportId(int exportId) {
        this.exportId = exportId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setFileExtention(String ext) {
        this.fileExtention = ext;
    }

    public String getFileExtention() {
        return fileExtention;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public boolean pathExists() {
        if (path == null) {
            return false;
        }
        return new File(path).exists();
    }

    public boolean isOffline() {
        return offline;
    }

    public ExportOutput setOffline(boolean offline) {
        this.offline = offline;
        return this;
    }

    public long getTimeOnline() {
        return timeOnline;
    }

    public ExportOutput setTimeOnline(long timeOnline) {
        this.timeOnline = timeOnline;
        return this;
    }

    public long getTimeOffline() {
        return timeOffline;
    }

    public ExportOutput setTimeOffline(long timeOffline) {
        this.timeOffline = timeOffline;
        return this;
    }

    @Override
    public String toString() {
        return String.format("<ExportOutput id='%d' export='%d' path='%s' type='%s'>",
                getId(), getExportId(), getPath(), factory.getKlassName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExportOutput export = (ExportOutput) o;
        return id == export.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
