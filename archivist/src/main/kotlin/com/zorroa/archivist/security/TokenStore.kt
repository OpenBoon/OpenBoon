package com.zorroa.archivist.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.google.common.io.BaseEncoding
import com.zorroa.archivist.repository.UserDao
import com.zorroa.archivist.service.DispatcherServiceImpl
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.util.UUID

/**
 * A basic interface for dealing with an external JWT token store
 * which utilizes the HMAC style JWT tokens.
 */
interface TokenStore {

    /**
     * Return the user's signing key as a string.
     */
    fun getSigningKey(userId: UUID): String

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
 *
 */
@Service
class LocalTokenStore @Autowired constructor(
    private val jedisPool: JedisPool,
    private val userDao: UserDao,
    private val meterRegistry: MeterRegistry) : TokenStore {

    @Value("\${archivist.security.jwt.zorroa.expire-time-seconds}")
    var expireTime : Int = 1200

    override fun getSigningKey(userId: UUID) : String {
        return userDao.getHmacKey(userId)
    }

    override fun incrementSessionExpirationTime(sessionId: String) {
        jedisPool.resource.use {
            if (it.expire(sessionId, expireTime) == 0L) {
                throw JWTVerificationException("Invalid token")
            }
        }
    }

    override fun removeSession(sessionId: String) {
        jedisPool.resource.use {
            try {
                it.del(sessionId)
                meterRegistry.counter("zorroa.local-token-store.remove-session").increment()
            } catch (e: Exception) {

            }
        }
    }

    override fun createSessionToken(userId: UUID) : String {
        val sessionId = encodeUUIDBase64(UUID.randomUUID())
        logger.info("---------CREATE SESSION: {}", sessionId)
        val token = generateUserToken(userId, sessionId, userDao.getHmacKey(userId))

        jedisPool.resource.use {
            if (it.exists(sessionId)) {
                throw IllegalStateException("Invalid token")
            }

            it.set(sessionId, "1")
            it.expire(sessionId, expireTime)
        }
        meterRegistry.counter("zorroa.local-token-store.create-session").increment()
        return token
    }

    override fun createAPIToken(userId: UUID) : String {
        return generateUserToken(userId, null, userDao.getHmacKey(userId))
    }

    fun getRedisClient() : Jedis {
        return jedisPool.resource
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LocalTokenStore::class.java)
    }
}

fun encodeUUIDBase64(uuid: UUID): String {
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.putLong(uuid.mostSignificantBits)
    bb.putLong(uuid.leastSignificantBits)
    return StringUtils.trimTrailingCharacter(BaseEncoding.base64Url().encode(bb.array()), '=')
}

fun generateUserToken(userId: UUID, sessionId: String?, signingKey: String): String {
    val algo = Algorithm.HMAC256(signingKey)
    val spec =  JWT.create().withIssuer("zorroa")
        .withClaim("userId", userId.toString())
    if (sessionId != null) {
        spec.withClaim("sessionId", sessionId)
    }
    return spec.sign(algo)
}

