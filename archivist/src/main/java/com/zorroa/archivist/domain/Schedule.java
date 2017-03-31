package com.zorroa.archivist.domain;

import java.util.Objects;

/**
 * Created by chambers on 7/9/16.
 */
public class Schedule {

    private final String minute = "0";
    private String hour = "*/12";
    private String day = "*";
    private String month = "*";
    private String weekday = "*";
    private String description = "";

    public Schedule() { }

    public Schedule(String tab) {
        String[] parts = tab.split(" " , 5);
        this.hour = parts[1];
        this.day = parts[2];
        this.month = parts[3];
        this.weekday = parts[4];
    }

    public String getHour() {
        return hour;
    }

    public Schedule setHour(String hour) {
        this.hour = hour;
        return this;
    }

    public String getDay() {
        return day;
    }

    public Schedule setDay(String day) {
        this.day = day;
        return this;
    }

    public String getMonth() {
        return month;
    }

    public Schedule setMonth(String month) {
        this.month = month;
        return this;
    }

    public String getWeekday() {
        return weekday;
    }

    public Schedule setWeekday(String weekday) {
        this.weekday = weekday;
        return this;
    }

    public String toString() {
        return String.join(" ", minute, hour, day, month, weekday);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schedule schedule = (Schedule) o;
        return Objects.equals(minute, schedule.minute) &&
                Objects.equals(getHour(), schedule.getHour()) &&
                Objects.equals(getDay(), schedule.getDay()) &&
                Objects.equals(getMonth(), schedule.getMonth()) &&
                Objects.equals(getWeekday(), schedule.getWeekday());
    }

    @Override
    public int hashCode() {
        return Objects.hash(minute, getHour(), getDay(), getMonth(), getWeekday());
    }
}
