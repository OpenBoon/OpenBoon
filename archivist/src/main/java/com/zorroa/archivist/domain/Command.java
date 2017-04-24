package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;

import java.util.List;
import java.util.Objects;

/**
 * Command object.  Note: progress field is dynamic.
 */
public class Command {

    private int id;
    private UserBase user;
    private CommandType type;
    private List<Object> args;
    private JobState state;

    private long totalCount = 0;
    private long successCount = 0;
    private long errorCount = 0;
    private String message;

    public int getId() {
        return id;
    }

    public Command setId(int id) {
        this.id = id;
        return this;
    }

    public UserBase getUser() {
        return user;
    }

    public Command setUser(UserBase user) {
        this.user = user;
        return this;
    }

    public CommandType getType() {
        return type;
    }

    public Command setType(CommandType type) {
        this.type = type;
        return this;
    }

    public List<Object> getArgs() {
        return args;
    }

    public Command setArgs(List<Object> args) {
        this.args = args;
        return this;
    }

    public JobState getState() {
        return state;
    }

    public Command setState(JobState state) {
        this.state = state;
        return this;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public Command setTotalCount(long totalCount) {
        this.totalCount = totalCount;
        return this;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public Command setSuccessCount(long successCount) {
        this.successCount = successCount;
        return this;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public Command setErrorCount(long errorCount) {
        this.errorCount = errorCount;
        return this;
    }

    public double getProgress() {
        double completed = successCount + errorCount;
        if (totalCount == 0 || completed == 0) {
            return 0;
        }
        return completed / totalCount;
    }

    public String getMessage() {
        return message;
    }

    public Command setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("type", type)
                .add("state", state)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Command command = (Command) o;
        return getId() == command.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
