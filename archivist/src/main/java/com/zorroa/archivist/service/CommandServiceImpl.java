package com.zorroa.archivist.service;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.domain.Acl;
import com.zorroa.archivist.domain.Command;
import com.zorroa.archivist.domain.CommandSpec;
import com.zorroa.archivist.repository.CommandDao;
import com.zorroa.sdk.client.exception.ArchivistException;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

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
    public Command run(Command cmd) {
        String failureMessage = null;
        boolean started = false;
        try {
            started = commandDao.start(cmd);
            if (started) {
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
                    default:
                        logger.warn("Unknown command type: {}", cmd.getType());
                }
            }
        } catch (Exception e) {
            failureMessage = e.getMessage();
            logger.warn("Failed to execute command {}, unexpected: ", cmd, e);

        } finally {
            if (started) {
                commandDao.stop(cmd, failureMessage);
            }
        }
        return commandDao.refresh(cmd);
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
        return Scheduler.newFixedRateSchedule(30, 1, TimeUnit.SECONDS);
    }
}
