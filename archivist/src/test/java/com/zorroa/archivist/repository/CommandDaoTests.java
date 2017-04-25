package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by chambers on 4/21/17.
 */
public class CommandDaoTests extends AbstractTest {

    @Autowired
    CommandDao commandDao;

    Command command;

    @Before
    public void init() {
        CommandSpec spec = new CommandSpec();
        spec.setType(CommandType.UpdateAssetPermissions);
        spec.setArgs(new Object[] {
                new AssetSearch().setQuery("foo"),
                new Acl().addEntry(userService.getPermission("group::manager"), Access.Read)
        });
        command = commandDao.create(spec);
    }

    @Test
    public void testCreate() {
        assertEquals(JobState.Waiting, command.getState());
        assertEquals(CommandType.UpdateAssetPermissions, command.getType());

        List<Object> args = command.getArgs();
        AssetSearch search = Json.Mapper.convertValue(args.get(0), AssetSearch.class);
        Acl acl = Json.Mapper.convertValue(args.get(1), Acl.class);

        assertEquals("foo", search.getQuery());
        assertEquals(1, acl.size());
        assertTrue(acl.hasAccess(userService.getPermission("group::manager").getId(),
                Access.Read));
    }

    @Test
    public void testGet() {
        Command command1 = commandDao.get(command.getId());
        assertEquals(command, command1);
    }

    @Test
    public void testGetNext() {
        Command command1 = commandDao.getNext();
        assertEquals(command, command1);
    }

    @Test
    public void testStart() {
        assertTrue(commandDao.start(command));
        assertFalse(commandDao.start(command));
        assertNull(commandDao.getNext());
    }

    @Test
    public void teststop() {
        assertTrue(commandDao.start(command));
        assertTrue(commandDao.stop(command, null));
        assertFalse(commandDao.stop(command, null));
        assertNull(commandDao.getNext());
    }

    @Test
    public void testGetPendingByUser() {
        List<Command> all = commandDao.getPendingByUser();
        assertEquals(1, all.size());

        // Create a new one
        CommandSpec spec = new CommandSpec();
        spec.setType(CommandType.UpdateAssetPermissions);
        spec.setArgs(new Object[] {
                new AssetSearch().setQuery("foo"),
                new Acl().addEntry(userService.getPermission("group::manager"), Access.Read)
        });
        Command cmd = commandDao.create(spec);

        all = commandDao.getPendingByUser();
        assertEquals(2, all.size());

        // Now our new command should be first in the list.
        assertTrue(commandDao.start(cmd));
        all = commandDao.getPendingByUser();
        assertEquals(cmd.getId(), all.get(0).getId());

    }

    @Test
    public void testUpdateProgress() {
        assertTrue(commandDao.updateProgress(command, 100, 10, 0));
        assertEquals(0.1, commandDao.refresh(command).getProgress(), 0.01);

        assertTrue(commandDao.updateProgress(command, 100, 0, 5));
        assertEquals(0.15, commandDao.refresh(command).getProgress(), 0.01);

        assertTrue(commandDao.updateProgress(command, 100, 5, 0));
        assertEquals(0.2, commandDao.refresh(command).getProgress(), 0.01);
    }
}
