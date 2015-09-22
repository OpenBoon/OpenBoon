package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Created by chambers on 9/5/15.
 */
public class IngestScheduleBuilder {

    private String name;

    /**
     * Time in hh:mm:ss format.
     */
    private String runAtTime;

    /**
     * The IDs of the ingests to put on this schedule.
     */
    private List<Long> ingestIds;

    /**
     * Bitwise 1=mon, 2=tues, 3=wed, etc
     */
    private List<DayOfWeek> days;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRunAtTime() {
        return runAtTime;
    }

    public void setRunAtTime(String runAtTime) {
        LocalTime.parse(runAtTime);
        this.runAtTime = runAtTime;
    }

    @JsonIgnore
    public LocalTime getRunAtTimeLocalTime() {
        return LocalTime.parse(runAtTime);
    }


    public List<Long> getIngestIds() {
        return ingestIds;
    }

    public void setIngestIds(List<Long> ingestIds) {
        this.ingestIds = ingestIds;
    }

    public List<DayOfWeek> getDays() {
        return days;
    }

    public void setDays(List<DayOfWeek> days) {
        this.days = days;
    }

    public void setAllDays() {
        days = Lists.newArrayList();
        for (DayOfWeek d: DayOfWeek.values()) {
            days.add(d);
        }
    }
}
