package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Command;
import com.zorroa.archivist.domain.CommandSpec;

/**
 * Created by chambers on 4/21/17.
 */
public interface CommandService {

    Command submit(CommandSpec spec);

    Command get(int id);

    Command refresh(Command cmd);

    Command run(Command cmd);
}
