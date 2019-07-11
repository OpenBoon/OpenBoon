package com.zorroa.archivist.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.zorroa.archivist.domain.IdGen
import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.sdk.security.AuthSource
import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.common.clients.RestClient
import com.zorroa.common.util.Json
import com.zorroa.common.util.readValueOrNull
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.nio.file.Path
import java.security.cert.CertificateFactory
import java.security.interfaces.RSAPublicKey
import java.util.UUID

class IrmJwtValidator constructor(
    val path: Path,
    val permMap: Map<String, String>,
    val userRegistryService: UserRegistryService
) : JwtValidator {

    private val credential: GoogleCredential = GoogleCredential.fromStream(FileInputStream(path.toFile()))
    private val client = RestClient("https://www.googleapis.com")
    private val publicKey: RSAPublicKey

    init {
        logger.info("Initializing IRM JwtValidator")
        val user = credential.serviceAccountId
        val keys = client.get("/robot/v1/metadata/x509/$user", Json.STRING_MAP)

        val certificateFactory = CertificateFactory.getInstance("X.509")
        val cert = certificateFactory.generateCertificate(
            keys.getValue(credential.serviceAccountPrivateKeyId).byteInputStream()
        )
        this.publicKey = cert.publicKey as RSAPublicKey
    }

    override fun validate(token: String): Map<String, String> {

        try {
            val jwt = JWT.decode(token)
            val verifier = JWT.require(Algorithm.RSA256(publicKey, null))
                .acceptLeeway(30) // because not everyone uses NTP
                .build()
            verifier.verify(token)

            val claims = jwt.claims.map {
                it.key to it.value.asString()
            }.toMap().toMutableMap()

            val unpackedInsightClaim: Map<String, Any> =
                Json.Mapper.readValueOrNull(jwt.getClaim("insightUser").asString())
                    ?: throw JwtValidatorException("No insightUser found in JWT claims")

            claims["permissions"] = mapPermissions(unpackedInsightClaim).joinToString()
            claims.putAll(mapInsightUserLoginInfo(unpackedInsightClaim))

            unpackedInsightClaim.map {
                claims.putIfAbsent(it.key, it.value.toString())
            }

            // Move the value of userId to username
            claims["username"] = claims.getValue("userId")

            claims["userId"] = if (userRegistryService.exists(claims.getValue("userId"), "Jwt")) {
                // Support any users that were already created with a JWT token
                val userAuthed = userRegistryService.getUser(claims.getValue("userId"))
                userAuthed.id.toString()
            } else {
                // Generate a unique userId from the username and company ID.
                // This allows JwtAuthenticationProvider to stay generic.
                IdGen.getId("${claims["username"]}::${claims["companyId"]}")
            }

            return claims
        } catch (e: Exception) {
            throw JwtValidatorException("Failed to validate token", e)
        }
    }

    private fun mapInsightUserLoginInfo(unpackedInsightClaim: Map<String, Any>): Map<out String, String> {
        val userLoginInfo = unpackedInsightClaim["userLoginInfo"] as Map<String, String>
        val map = mutableMapOf<String, String>()
        userLoginInfo["firstName"]?.let {
            map["first_name"] = it
        }
        userLoginInfo["lastName"]?.let {
            map["last_name"] = it
        }
        userLoginInfo["email"]?.let {
            map["mail"] = it
        }
        // Guess language from possible attributes
        userLoginInfo["locale"]?.let {
            map.putIfAbsent("user_locale", it)
        }
        userLoginInfo["language"]?.let {
            map.putIfAbsent("user_locale", it)
        }
        userLoginInfo["lang"]?.let {
            map.putIfAbsent("user_locale", it)
        }
        return map
    }

    private fun mapPermissions(insightUserClaims: Map<String, Any>): List<String> {
        val permissions = mutableListOf<String>()
        val authInfo = insightUserClaims["userAuthInfo"] as Map<String, Any>
        val aclEntries = authInfo["aclEntries"] as List<Map<String, String>>
        permissions.addAll(aclEntries?.mapNotNull { p ->
            p["permission"]
        })
        return permissions
    }

    override fun provisionUser(claims: Map<String, String>) {

        // These things have to exist, they cannot be null.
        val userId = claims.getValue("userId")
        val username = claims.getValue("username")
        val companyId = claims.getValue("companyId")

        val orgId = if (companyId == "0") {
            Organization.DEFAULT_ORG_ID.toString()
        } else {
            "company-$companyId"
        }

        var authorities = claims["permissions"]?.let { str ->
            str.split(",").mapNotNull { token -> permMap[token.trim()] }
        }

        val source = AuthSource("IRM", "Jwt", "Jwt", orgId,
            attrs = claims,
            groups = authorities,
            userId = UUID.fromString(userId)
        )
        userRegistryService.registerUser(username, source)
    }

    companion object {
        private val logger =
            LoggerFactory.getLogger(IrmJwtValidator::class.java)
    }
}
