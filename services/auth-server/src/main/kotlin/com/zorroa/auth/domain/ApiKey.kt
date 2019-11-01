package com.zorroa.auth.domain

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

object Permission {
    val READ_ASSETS = "READ_ASSETS"
}

object Role {
    val SUPERADMIN_PERM = "SUPERADMIN"
    val SUPERADMIN_ROLE = "ROLE_SUPERADMIN"
}

class ApiKeySpec(
        val name: String,
        val projectId: UUID,
        val permissions: List<String>
)

@Entity
@Table(name = "api_key")
class ApiKey(
        @Id
        @Column(name = "pk_api_key")
        val keyId: UUID,

        @Column(name = "project_id", nullable = false)
        val projectId: UUID,

        @Column(name = "shared_key", nullable = false)
        val sharedKey: String,

        @Column(name = "name", nullable = false)
        val name: String,

        @Column(name = "permissions", nullable = false)
        val permissions: String
) {
    @JsonIgnore
    fun getGrantedAuthorities(): List<GrantedAuthority> {
        return if (permissions.isNullOrEmpty()) {
            listOf()
        } else {
            permissions.split(",").map { SimpleGrantedAuthority(it) }
        }
    }

    @JsonIgnore
    fun getJwtToken(timeout: Int = 60, projId: UUID? = null): String {
        val algo = Algorithm.HMAC512(sharedKey)
        val spec = JWT.create().withIssuer("zorroa")
                .withClaim("keyId", keyId.toString())
                .withClaim("projectId", (projId ?: projectId).toString())

        if (timeout != null) {
            val c = Calendar.getInstance()
            c.time = Date()
            c.add(Calendar.SECOND, timeout)
            spec.withExpiresAt(c.time)
        }
        return spec.sign(algo)
    }

    override fun toString(): String {
        return "ApiKey(keyId=$keyId, projectId=$projectId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApiKey

        if (keyId != other.keyId) return false
        if (projectId != other.projectId) return false
        if (sharedKey != other.sharedKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyId.hashCode()
        result = 31 * result + projectId.hashCode()
        result = 31 * result + sharedKey.hashCode()
        return result
    }

}