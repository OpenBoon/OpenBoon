package com.zorroa.archivist.service

import com.google.common.util.concurrent.AbstractScheduledService
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.CommandDao
import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.archivist.security.InternalAuthentication
import com.zorroa.archivist.security.getUsername
import com.zorroa.sdk.client.exception.ArchivistException
import com.zorroa.sdk.client.exception.ArchivistWriteException
import com.zorroa.sdk.search.AssetSearch
import com.zorroa.sdk.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PostConstruct

interface CommandService {

    fun getPendingByUser(): List<Command>

    fun submit(spec: CommandSpec): Command

    fun get(id: UUID): Command

    fun refresh(cmd: Command): Command

    fun setRunningCommand(cmd: Command): Boolean

    fun cancelRunningCommand(cmd: Command): Boolean

    fun cancel(cmd: Command): Boolean

    fun run(cmd: Command): Command
}

/**
 * A dedicated system for running internal tasks which should be run in serial.
 */
@Service
class CommandServiceImpl @Autowired constructor (
        private val commandDao: CommandDao
): AbstractScheduledService(), CommandService {

    @Autowired
    private lateinit var assetService: AssetService

    @Autowired
    private lateinit var userRegistryService: UserRegistryService

    internal var runningCommand = AtomicReference<Command>()

    @PostConstruct
    fun init() {
        // Not started for unit tests
        if (!ArchivistConfiguration.Companion.unittest) {
            this.startAsync()
        }
    }

    override fun submit(spec: CommandSpec): Command {
        /**
         * Validate the args!
         */
        try {
            val argTypes = spec.type.argTypes
            val args = spec.args

            if (args == null || argTypes.size != args.size) {
                throw ArchivistWriteException(
                        "Invalid command arguments, args do not match requirements.")
            }

            for (i in argTypes.indices) {
                Json.Mapper.convertValue(args[i], argTypes[i])
            }
        } catch (e: ArchivistException) {
            throw e
        } catch (e: Exception) {
            throw ArchivistWriteException("Failed to submit command, invalid args " + e.message)
        }

        return commandDao.create(spec)
    }

    override operator fun get(id: UUID): Command {
        return commandDao.get(id)
    }

    override fun refresh(cmd: Command): Command {
        return commandDao.get(cmd.id)
    }

    override fun setRunningCommand(cmd: Command): Boolean {
        if (runningCommand.get() == null) {
            logger.info("Setting running command: {}", cmd);
            runningCommand.set(cmd)
            return true
        }
        logger.warn("Command was already running: {}", runningCommand.get())
        return false
    }

    override fun cancelRunningCommand(cmd: Command): Boolean {
        val currentCommand = runningCommand.get()

        if (currentCommand != null && currentCommand.id == cmd.id) {
            if (currentCommand.state != JobState.Cancelled) {
                currentCommand.state = JobState.Cancelled
                logger.info("{} canceled running {}", getUsername(), currentCommand)
                if (ArchivistConfiguration.Companion.unittest) {
                    runningCommand.set(null)
                }
                return true
            }
            logger.warn("The command $cmd was not the running command")
            return false
        }
        logger.warn("The command $cmd was not the running command")
        return false
    }

    override fun cancel(cmd: Command): Boolean {
        if (commandDao.cancel(cmd, "Command canceled by " + getUsername())) {
            cancelRunningCommand(cmd)
            return true
        }
        return false
    }

    override fun run(cmd: Command): Command {
        var failureMessage: String? = null
        var started = false

        try {

            /**
             * Switch thread to user who made the request.
             */
            val user = userRegistryService.getUser(cmd.user.username)
            SecurityContextHolder.getContext().authentication = InternalAuthentication(user)

            started = commandDao.start(cmd)
            if (started) {
                setRunningCommand(cmd)
                when (cmd.type) {
                    CommandType.UpdateAssetPermissions -> {
                        val acl = Json.Mapper.convertValue(cmd.args[1], Acl::class.java)
                        val search = Json.Mapper.convertValue(cmd.args[0], AssetSearch::class.java)
                        if (acl.isEmpty()) {
                            failureMessage = "ACL was empty, not permissions to apply"
                        } else {
                            assetService.setPermissions(cmd, search, acl)
                        }
                    }
                    CommandType.Sleep -> {
                        logger.info("Sleeping...")
                        Thread.sleep(Json.Mapper.convertValue(cmd.args[0], Long::class.java))
                        logger.warn("Unknown command type: {}", cmd.type)
                    }
                    else -> logger.warn("Unknown command type: {}", cmd.type)
                }
            }
        } catch (e: Exception) {
            failureMessage = e.message
            logger.warn("Failed to execute command {}, unexpected: ", cmd, e)

        } finally {
            SecurityContextHolder.clearContext()
            if (started) {
                runningCommand.set(null)
                commandDao.stop(cmd, failureMessage)
            }
        }
        return commandDao.refresh(cmd)
    }

    override fun getPendingByUser(): List<Command> {
        return commandDao.getPendingByUser()
    }

    @Throws(Exception::class)
    override fun runOneIteration() {
        try {
            val cmd = commandDao.getNext() ?: return
            run(cmd)
        } catch (e: Exception) {
            logger.warn("Unexpected exception running commands, ", e)
        }

    }

    override fun scheduler(): AbstractScheduledService.Scheduler {
        return AbstractScheduledService.Scheduler.newFixedDelaySchedule(60, 1, TimeUnit.SECONDS)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommandServiceImpl::class.java)
    }
}
