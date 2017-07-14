package com.zorroa.archivist.service;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.archivist.config.ArchivistConfiguration;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.CommandDao;
import com.zorroa.archivist.security.InternalAuthentication;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.client.exception.ArchivistException;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A dedicated system for running internal tasks which should be run in serial.
 */
@Service
public class CommandServiceImpl  extends AbstractScheduledService implements CommandService {

    private static final Logger logger = LoggerFactory.getLogger(CommandServiceImpl.class);

    @Autowired
    CommandDao commandDao;

    @Autowired
    AssetService assetService;

    @Autowired
    UserService userService;

    AtomicReference<Command> runningCommand = new AtomicReference<>();

    @PostConstruct
    public void init() {
        // Not started for unit tests
        if (!ArchivistConfiguration.unittest) {
            this.startAsync();
        }
    }

    @Override
    public Command submit(CommandSpec spec) {
        /**
         * Validate the args!
         */
        try {
            Class[] argTypes = spec.getType().getArgTypes();
            Object[] args = spec.getArgs();

            if (args == null || (argTypes.length != args.length)) {
                throw new ArchivistWriteException(
                        "Invalid command arguments, args do not match requirements.");
            }

            for (int i = 0; i<argTypes.length; i++) {
                Json.Mapper.convertValue(args[i], argTypes[i]);
            }
        } catch (ArchivistException e) {
            throw e;
        } catch (Exception e) {
            throw new ArchivistWriteException("Failed to submit command, invalid args " + e.getMessage());
        }

        return commandDao.create(spec);
    }

    @Override
    public Command get(int id) {
        return commandDao.get(id);
    }

    @Override
    public Command refresh(Command cmd) { return commandDao.get(cmd.getId());}

    @Override
    public boolean setRunningCommand(Command cmd) {
        if (runningCommand.get() == null) {
            runningCommand.set(cmd);
            return true;
        }
        return false;
    }

    @Override
    public boolean cancelRunningCommand(Command cmd) {
        Command currentCommand = runningCommand.get();
        if (currentCommand != null && currentCommand.getId() == cmd.getId()) {
            if (!currentCommand.getState().equals(JobState.Cancelled)) {
                currentCommand.setState(JobState.Cancelled);
                logger.info("{} canceled running {}", SecurityUtils.getUsername(), currentCommand);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean cancel(Command cmd) {
        if (commandDao.cancel(cmd, "Command canceled by " + SecurityUtils.getUsername())) {
            cancelRunningCommand(cmd);
            return true;
        }
        return false;
    }

    @Override
    public Command run(Command cmd) {
        String failureMessage = null;
        boolean started = false;

        try {

            /**
             * Switch thread to user who made the request.
             */
            User user = userService.get(cmd.getUser().getId());
            SecurityContextHolder.getContext().setAuthentication(new InternalAuthentication(user,
                    userService.getPermissions(user)));

            started = commandDao.start(cmd);
            if (started) {
                setRunningCommand(cmd);
                switch (cmd.getType()) {
                    case UpdateAssetPermissions:
                        Acl acl = Json.Mapper.convertValue(cmd.getArgs().get(1), Acl.class);
                        AssetSearch search = Json.Mapper.convertValue(cmd.getArgs().get(0), AssetSearch.class);
                        if (acl.isEmpty()) {
                            failureMessage = "ACL was empty, not permissions to apply";
                        }
                        else {
                            assetService.setPermissions(cmd, search, acl);
                        }
                        break;
                    case Sleep:
                        logger.info("Sleeping...");
                        Thread.sleep(Json.Mapper.convertValue(cmd.getArgs().get(0), Long.class));
                    default:
                        logger.warn("Unknown command type: {}", cmd.getType());
                }
            }
        } catch (Exception e) {
            failureMessage = e.getMessage();
            logger.warn("Failed to execute command {}, unexpected: ", cmd, e);

        } finally {
            SecurityContextHolder.clearContext();
            if (started) {
                runningCommand.set(null);
                commandDao.stop(cmd, failureMessage);
            }
        }
        return commandDao.refresh(cmd);
    }

    @Override
    public List<Command> getPendingByUser() {
        return commandDao.getPendingByUser();
    }

    @Override
    protected void runOneIteration() throws Exception {
        try {
            Command cmd = commandDao.getNext();
            if (cmd == null) {
                return;
            }
            run(cmd);
        } catch (Exception e) {
            logger.warn("Unexpected exception running commands, ", e);
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedDelaySchedule(20, 1, TimeUnit.SECONDS);
    }
}
