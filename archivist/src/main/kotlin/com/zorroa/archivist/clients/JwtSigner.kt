package com.zorroa.common.clients

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.impl.PublicClaims
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import org.apache.http.HttpRequest
import java.io.FileInputStream
import java.nio.file.Path
import java.security.interfaces.RSAPrivateKey
import java.util.*

interface JwtSigner {

    fun sign(req: HttpRequest, host:String, claims: Map<String,String>?=null)
}

/**
 * A JWT Signer that uses google app creds.
 */
class GcpJwtSigner : JwtSigner {

    private val credential : GoogleCredential

    constructor(credential: GoogleCredential) {
        this.credential = credential
    }

    constructor(path: String) : this(GoogleCredential.fromStream(FileInputStream(path)))

    constructor(path: Path) : this(GoogleCredential.fromStream(FileInputStream(path.toFile())))

    constructor() {
        if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS") != null) {
            credential = GoogleCredential.getApplicationDefault()
        }
        else {
            throw IllegalStateException("Unable to determine path of google credentials")
        }
    }

    override fun sign(req: HttpRequest, host:String, claims: Map<String,String>?)  {
        val token =  getToken(host, claims)
        req.setHeader("Authorization", "Bearer $token")
    }

    private fun getToken(host:String, claims: Map<String,String>?=null) : String {
        val now = Date()
        var expiration = Calendar.getInstance()
        expiration.add(Calendar.HOUR_OF_DAY, 1)
        val builder = JWT.create()
                .withKeyId(credential.serviceAccountPrivateKeyId)
                .withIssuer("https://cloud.google.com/iap")
                .withClaim(PublicClaims.SUBJECT, credential.serviceAccountUser)
                .withAudience(host)
                .withIssuedAt(now)
                .withExpiresAt(expiration.time)
        claims?.forEach { (k,v)->
            builder.withClaim(k, v)
        }
        return builder.sign(Algorithm.RSA256(null,
                credential.serviceAccountPrivateKey as RSAPrivateKey?))
    }
}
