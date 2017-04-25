package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Command;
import com.zorroa.archivist.domain.CommandSpec;

import java.util.List;

/**
 * Created by chambers on 4/21/17.
 */
public interface CommandDao extends GenericDao<Command, CommandSpec> {

    List<Command> getPendingByUser();

    Command getNext();

    boolean start(Command cmd);

    boolean stop(Command cmd, String msg);

    boolean updateProgress(Command cmd, long total, long incrementSuccess, long incrementError);
}
