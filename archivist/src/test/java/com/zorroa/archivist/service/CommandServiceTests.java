package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Command;
import com.zorroa.archivist.domain.CommandSpec;
import com.zorroa.archivist.domain.CommandType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 7/12/17.
 */
public class CommandServiceTests extends AbstractTest {

    @Autowired
    CommandService commandService;

    Command command;

    @Before
    public void init() {
        CommandSpec spec = new CommandSpec();
        spec.setType(CommandType.Sleep);
        spec.setArgs(new Object[] { 1000L });
        command = commandService.submit(spec);
    }

    @Test
    public void testCancelRunningCommand() {
        assertTrue(commandService.setRunningCommand(command));
        assertFalse(commandService.setRunningCommand(command));
        assertTrue(commandService.cancelRunningCommand(command));
        assertFalse(commandService.cancelRunningCommand(command));
    }
}
