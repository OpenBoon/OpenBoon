package com.zorroa.archivist.sdk.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;

/**
 * Exported Output
 */
public class ExportOutput {

    private int id;
    private String name;
    private int exportId;
    private String createdBy;
    private long createdTime;

    private ProcessorFactory<ExportProcessor> factory;

    public ProcessorFactory<ExportProcessor> getFactory() {
        return factory;
    }

    public void setFactory(ProcessorFactory<ExportProcessor> factory) {
        this.factory = factory;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("<Export id='%d' type='%s'>", getId(), factory.getKlassName());
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
