package com.zorroa.archivist.domain;

/**
 *   pk_command INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
 pk_user INT NOT NULL,
 time_created BIGINT NOT NULL,
 time_started BIGINT NOT NULL DEFAULT -1,
 time_stopped BIGINT NOT NULL DEFAULT -1,
 int_state TINYINT NOT NULL DEFAULT 0,
 str_error VARCHAR(255),
 str_type VARCHAR(32) NOT NULL,
 int_total_batch_count BIGINT NOT NULL DEFAULT 0,
 int_completed_batch_count BIGINT NOT NULL DEFAULT 0,
 json_args TEXT NOT NULL
 */
public class CommandSpec {

    private CommandType type;
    private Object[] args;

    public CommandSpec() { }

    public CommandType getType() {
        return type;
    }

    public CommandSpec setType(CommandType type) {
        this.type = type;
        return this;
    }

    public Object[] getArgs() {
        return args;
    }

    public CommandSpec setArgs(Object[] args) {
        this.args = args;
        return this;
    }
}
