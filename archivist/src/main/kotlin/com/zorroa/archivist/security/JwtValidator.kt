package com.zorroa.archivist.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID
import javax.annotation.PostConstruct

class JwtValidatorException constructor(
    override val message: String,
    override val cause: Throwable?
) : RuntimeException(message, cause) {

    constructor(message: String) : this(message, null)
}

object JwtSecurityConstants {

    const val TOKEN_PREFIX = "Bearer "
    const val HEADER_STRING_REQ = "Authorization"
    const val HEADER_STRING_RSP = "X-Zorroa-Credential"

    /**
     * Used to override organization
     */
    const val ORGID_HEADER = "X-Zorroa-Organization"
}

interface JwtValidator {

    /**
     * The only method you have to implement
     */
    fun validate(token: String): Map<String, String>

    /**
     * Provision a user with the given claims.
     */
    fun provisionUser(claims: Map<String, String>): UserAuthed?
}

/**
 * The validated JWT claims and the validator instance.
 */
class ValidatedJwt(
    val validator: JwtValidator,
    val claims: Map<String, String>
) {
    fun provisionUser(): UserAuthed? {
        return validator.provisionUser(claims)
    }
}

@Component
class MasterJwtValidator {

     val validators: MutableList<JwtValidator> = mutableListOf()

    @Autowired
    var externalJwtValidator: ExternalJwtValidator? = null

    @Autowired
    lateinit var jwtValidator: JwtValidator

    @PostConstruct
    fun init() {
        validators.add(jwtValidator)
        externalJwtValidator?.let {
            validators.add(it)
        }
    }

    fun validate(token: String): ValidatedJwt {
        for (validator in validators) {
            try {
                val claims = validator.validate(token)
                return ValidatedJwt(validator, claims)
            } catch (e: Exception) {
                // ignore
            }
        }

        val req = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
        throw JwtValidatorException("Failed to validate JWT token, ${req.requestURI}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MasterJwtValidator::class.java)
    }
}

class LocalUserJwtValidator @Autowired constructor(val userService: UserService) : JwtValidator {

    init {
        logger.info("Initializing User/Hmac JwtValidator")
    }

    override fun validate(token: String): Map<String, String> {
        try {
            val jwt = JWT.decode(token)
            val userId = UUID.fromString(jwt.claims.getValue("userId").asString())

            // Validate signing
            val hmacKey = userService.getHmacKey(userId)
            val alg = Algorithm.HMAC256(hmacKey)
            alg.verify(jwt)

            return jwt.claims.map {
                it.key to it.value.asString()
            }.toMap()
        } catch (e: JWTVerificationException) {
            throw JwtValidatorException("Failed to validate token", e)
        }
    }

    override fun provisionUser(claims: Map<String, String>): UserAuthed? {
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LocalUserJwtValidator::class.java)
    }
}
