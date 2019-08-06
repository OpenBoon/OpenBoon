package com.zorroa.archivist.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.common.io.BaseEncoding
import com.zorroa.archivist.repository.UserDao
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import redis.clients.jedis.JedisPool
import java.nio.ByteBuffer
import java.util.Calendar
import java.util.Date
import java.util.UUID

/**
 * A basic interface for dealing with an external JWT token store
 * which utilizes the HMAC style JWT tokens.
 */
interface TokenStore {

    /**
     * Update the expiration time with a future timestamp.
     */
    fun incrementSessionExpirationTime(sessionId: String)

    /**
     * Remove the given session Id from the token store.
     */
    fun removeSession(sessionId: String)

    /**
     * Create a new JWT token and register a session with the token store.
     */
    fun createSessionToken(userId: UUID): String

    /**
     * Create a does not expire API token for service/service communication.
     */
    fun createAPIToken(userId: UUID): String
}

/**
 * A TokenStore implementation that talks to a local Redis Server.
 */
@Service
class RedisTokenStore @Autowired constructor(
    private val jedisPool: JedisPool,
    private val userDao: UserDao,
    private val meterRegistry: MeterRegistry
) : TokenStore {

    /**
     * The maximum amount of time a JWT token is valid for.
     */
    @Value("\${archivist.security.jwt.token-expire-time-hours}")
    var tokenExpireTime: Int = 24

    /**
     * Automatic in activity logout time.  If there are no requests for this
     * amount of time, the user is automatically logged out.
     */
    @Value("\${archivist.security.jwt.session-inactivity-time-seconds}")
    var sessionExpireTime: Int = 1200

    override fun incrementSessionExpirationTime(sessionId: String) {
        jedisPool.resource.use {
            if (it.expire(sessionId, sessionExpireTime) == 0L) {
                meterRegistry.counter("zorroa.token-store", "action", "invalid-token").increment()
                throw BadCredentialsException("Invalid token")
            }
        }
    }

    override fun removeSession(sessionId: String) {
        jedisPool.resource.use {
            try {
                it.del(sessionId)
                meterRegistry.counter("zorroa.token-store", "action", "remove-token").increment()
            } catch (e: Exception) {
                meterRegistry.counter("zorroa.token-store", "action", "invalid-token").increment()
                logger.warn("Failed to remove sessionId: {}, already gone.", sessionId)
            }
        }
    }

    override fun createSessionToken(userId: UUID): String {
        val sessionId = "token:" + base64UUID(UUID.randomUUID())
        val token = generateUserToken(
            userId, userDao.getHmacKey(userId), sessionId = sessionId, expireTimeHours = tokenExpireTime
        )

        jedisPool.resource.use {
            if (it.exists(sessionId)) {
                meterRegistry.counter("zorroa.token-store", "action", "token-collision").increment()
                throw IllegalStateException("Invalid token")
            }

            it.set(sessionId, userId.toString())
            it.expire(sessionId, sessionExpireTime)
        }
        meterRegistry.counter("zorroa.token-store", "action", "create-token").increment()
        return token
    }

    override fun createAPIToken(userId: UUID): String {
        return generateUserToken(userId, userDao.getHmacKey(userId))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RedisTokenStore::class.java)
    }
}

fun base64UUID(uuid: UUID): String {
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.putLong(uuid.mostSignificantBits)
    bb.putLong(uuid.leastSignificantBits)
    return StringUtils.trimTrailingCharacter(BaseEncoding.base64Url().encode(bb.array()), '=')
}

/**
 * Generate a token with a users private key.
 *
 * @param userId The UUID of the user.
 * @param signingKey The users signing key.
 * @param sessionId An optional session Id.
 * @param expireTimeHours An optional expire time in days.
 */
fun generateUserToken(
    userId: UUID,
    signingKey: String,
    sessionId: String? = null,
    expireTimeHours: Int? = null
): String {
    val algo = Algorithm.HMAC256(signingKey)
    val spec = JWT.create().withIssuer("zorroa")
        .withClaim("userId", userId.toString())
    // Setup the session if its not null or empty.
    if (!sessionId.isNullOrEmpty()) {
        spec.withClaim("sessionId", sessionId)
    }
    if (expireTimeHours != null) {
        val c = Calendar.getInstance()
        c.time = Date()
        c.add(Calendar.HOUR, expireTimeHours)
        spec.withExpiresAt(c.time)
    }
    return spec.sign(algo)
}
