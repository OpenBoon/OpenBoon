package com.zorroa.archivist.domain;

import java.time.*;
import java.util.List;

/**
 * Created by chambers on 9/5/15.
 */
public class IngestSchedule {

    private int id;
    private String name;
    private String runAtTime;
    private List<DayOfWeek> days;
    private List<Long> ingestIds;

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

    public String getRunAtTime() {
        return runAtTime;
    }

    public void setRunAtTime(String runAtTime) {
        LocalTime.parse(runAtTime);
        this.runAtTime = runAtTime;
    }

    public List<DayOfWeek> getDays() {
        return days;
    }

    public void setDays(List<DayOfWeek> days) {
        this.days = days;
    }

    public List<Long> getIngestIds() {
        return ingestIds;
    }

    public void setIngestIds(List<Long> ingestIds) {
        this.ingestIds = ingestIds;
    }

    /**
     * Determines the next run time.
     *
     * @param schedule
     * @return
     */
    public static long determineNextRunTime(IngestSchedule schedule) {
        return determineNextRunTime(schedule.getDays(), LocalTime.parse(schedule.getRunAtTime()));
    }

    /**
     * Determines the next run time.
     *
     * @param days
     * @param runAtTime
     * @return
     */
    public static long determineNextRunTime(List<DayOfWeek> days, LocalTime runAtTime) {

        if (days.isEmpty()) {
            return -1;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.with(runAtTime);

        if (nextRun.isAfter(now)) {
            nextRun = nextRun.minusDays(1);
        }

        for(;;) {
            nextRun = nextRun.plusDays(1);
            if (days.contains(nextRun.getDayOfWeek())) {
                ZonedDateTime zdt = nextRun.atZone(ZoneId.systemDefault());
                return zdt.toInstant().toEpochMilli();
            }
        }
    }
}
