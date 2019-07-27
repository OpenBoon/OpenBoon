package com.zorroa.archivist.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.zorroa.common.clients.RestClient
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.nio.file.Path
import java.security.cert.CertificateFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Date
import java.util.UUID

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
    fun provisionUser(claims: Map<String, String>)
}

/**
 * The validated JWT claims and the validator instance.
 */
class ValidatedJwt(
    val validator: JwtValidator,
    val claims: Map<String, String>
) {
    fun provisionUser() {
        validator.provisionUser(claims)
    }
}

class MasterJwtValidator constructor(
    private val validators: List<JwtValidator>
) {
    fun validate(token: String): ValidatedJwt {
        for (validator in validators) {
            try {
                val claims = validator.validate(token)
                return ValidatedJwt(validator, claims)
            } catch (e: Exception) {

            }
        }
        throw JwtValidatorException("Failed to validate JWT token")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MasterJwtValidator::class.java)
    }
}

class LocalUserJwtValidator constructor(val tokenStore: TokenStore) : JwtValidator {

    init {
        logger.info("Initializing User/Hmac JwtValidator")
    }

    override fun validate(token: String): Map<String, String> {
        try {
            val jwt = JWT.decode(token)
            val userId = UUID.fromString(jwt.claims.getValue("userId").asString())

            // Validate signing
            val hmacKey = tokenStore.getSigningKey(userId)
            val alg = Algorithm.HMAC256(hmacKey)
            alg.verify(jwt)

            // Check expire if the token has a session ID.
            if (jwt.claims.containsKey("sessionId")) {
                tokenStore.incrementSessionExpirationTime(jwt.claims.getValue("sessionId").asString())
            }

            return jwt.claims.map {
                it.key to it.value.asString()
            }.toMap()

        } catch (e: JWTVerificationException) {
            throw JwtValidatorException("Failed to validate token", e)
        }
    }

    override fun provisionUser(claims: Map<String, String>) {
        // User already has to be provisioned for this validator to work
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LocalUserJwtValidator::class.java)
    }
}

/**
 * Validates a token with google credentials
 */
class GcpJwtValidator : JwtValidator {

    private val credentials: GoogleCredential
    private val client = RestClient("https://www.googleapis.com")
    private val publickKey: RSAPublicKey

    init {
        logger.info("Initializing GCP JwtValidator")
    }

    constructor(credentials: GoogleCredential) {
        this.credentials = credentials
        val user = credentials.serviceAccountId
        val keys = client.get("/robot/v1/metadata/x509/$user", Json.STRING_MAP)

        val cfactory = CertificateFactory.getInstance("X.509")
        val cert = cfactory.generateCertificate(
            keys.getValue(credentials.serviceAccountPrivateKeyId).byteInputStream()
        )
        this.publickKey = cert.publicKey as RSAPublicKey
    }

    constructor(path: String?) : this(GoogleCredential.fromStream(FileInputStream(path)))

    constructor(path: Path) : this(GoogleCredential.fromStream(FileInputStream(path.toFile())))

    constructor() : this(System.getenv()["GOOGLE_APPLICATION_CREDENTIALS"])

    override fun validate(token: String): Map<String, String> {
        try {
            val jwt = JWT.decode(token)
            val alg = when (jwt.algorithm) {
                "RS256" -> Algorithm.RSA256(
                    publickKey,
                    credentials.serviceAccountPrivateKey as RSAPrivateKey
                )
                else -> Algorithm.HMAC256(credentials.serviceAccountPrivateKey.encoded)
            }
            alg.verify(jwt)
            val expDate: Date? = jwt.expiresAt

            if (expDate != null && (System.currentTimeMillis() > expDate.time)) {
                throw JwtValidatorException("Token has expired")
            }

            val result = mutableMapOf<String, String>()
            if (logger.isDebugEnabled) {
                logger.debug("JWT Claims: {}", Json.prettyString(jwt.claims))
            }

            jwt.claims.forEach { (k, v) ->

                if (v.asString() != null) {
                    result[k] = v.asString()
                }
            }
            return result
        } catch (e: JWTVerificationException) {
            throw JwtValidatorException("Failed to validate token", e)
        }
    }

    override fun provisionUser(claims: Map<String, String>) {
        // User already has to be provisioned for this validator to work
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcpJwtValidator::class.java)
    }
}
