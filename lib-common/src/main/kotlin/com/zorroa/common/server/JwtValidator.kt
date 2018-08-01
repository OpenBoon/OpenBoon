package com.zorroa.common.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.zorroa.common.clients.RestClient
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.security.cert.CertificateFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

class JwtValidatorException constructor(override val message :
                                      String, override val cause : Throwable?) : RuntimeException(message, cause) {

    constructor(message: String) : this(message, null)
}

object JwtSecurityConstants {
    const val TOKEN_PREFIX = "Bearer "
    const val HEADER_STRING = "Authorization"
}

interface JwtValidator {
    /**
     * The only method you have to implement
     */
    fun validate(token: String): Map<String,String>
}

class NoOpJwtValidator: JwtValidator {
    override fun validate(token: String) : Map<String,String> { return mapOf() }
}

/**
 * Validates a token with google credentials
 */
class GcpJwtValidator : JwtValidator {

    private val credentials : GoogleCredential
    private val client = RestClient("https://www.googleapis.com")
    private val publickKey : RSAPublicKey

    constructor(credentials: GoogleCredential) {
        this.credentials = credentials
        val user = credentials.serviceAccountId
        val keys = client.get("/robot/v1/metadata/x509/$user", Json.STRING_MAP)

        val cfactory = CertificateFactory.getInstance("X.509")
        val cert = cfactory.generateCertificate(
                keys.getValue(credentials.serviceAccountPrivateKeyId).byteInputStream())
        this.publickKey = cert.publicKey as RSAPublicKey

    }
    constructor(path: String?) : this(GoogleCredential.fromStream(FileInputStream(path)))

    constructor() : this(System.getenv()["GOOGLE_APPLICATION_CREDENTIALS"])

    override fun validate(token: String) : Map<String, String> {
        try {
            val jwt = JWT.decode(token)
            val alg = when(jwt.algorithm) {
                "RS256"-> Algorithm.RSA256(publickKey, credentials.serviceAccountPrivateKey as RSAPrivateKey)
                else -> Algorithm.HMAC256(credentials.serviceAccountPrivateKey.encoded)
            }
            alg.verify(jwt)
            val expDate: Date? = jwt.expiresAt

            if (expDate != null && (System.currentTimeMillis() > expDate.time)) {
                throw JwtValidatorException("Token has expired")
            }

            val result = mutableMapOf<String,String>()
            logger.info("Claims: {}", Json.prettyString(jwt.claims))

            jwt.claims.forEach { (k,v) ->
                if (v.asString() != null) {
                    result[k] = v.asString()
                }
            }
            logger.info("Claims Result: {}", Json.prettyString(jwt.claims))
            return result

        } catch(e: JWTVerificationException) {
            throw JwtValidatorException("Failed to validate token", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcpJwtValidator::class.java)
    }
}
