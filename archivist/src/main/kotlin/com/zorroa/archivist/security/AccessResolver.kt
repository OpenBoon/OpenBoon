package com.zorroa.archivist.security

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.Access
import com.zorroa.archivist.domain.Document
import com.zorroa.common.util.Json
import com.zorroa.security.Groups
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component

interface AccessResolver {
    fun getAssetPermissionsFilter(access: Access = Access.Read): QueryBuilder?
    fun hasAccess(access: Access, asset: Document): Boolean
}

@Component
class AcccessResolverImpl @Autowired constructor(
    private val properties: ApplicationProperties
) : AccessResolver {

    @Value("\${archivist.security.jwt.use-jwt-permission-filter}")
    var useJwtFilter: Boolean = false

    override fun getAssetPermissionsFilter(access: Access): QueryBuilder? {
        val user = getUser()
        /**
         * If we're using a JWT filter then the uer must have an access filter
         * along with the appropriate permission.
         */
        if (useJwtFilter) {
            if (access == Access.Write && !hasPermission(Groups.WRITE)) {
                throw AccessDeniedException("User does not have appropriate write permissions.")
            } else if (access == Access.Delete && !hasPermission(Groups.DELETE)) {
                throw AccessDeniedException("User does not have appropriate delete permissions.")
            } else if (access == Access.Read && !hasPermission(Groups.READ)) {
                throw AccessDeniedException("User does not have appropriate read permissions.")
            } else if (user.queryStringFilter == null) {
                throw AccessDeniedException("User is missing an access filter")
            } else {
                return QueryBuilders.queryStringQuery(user.queryStringFilter as String)
                    .autoGenerateSynonymsPhraseQuery(false)
                    .analyzeWildcard(false)
                    .lenient(false)
                    .analyzer("keyword")
                    .fuzzyMaxExpansions(0)
                    .fuzzyTranspositions(false)
            }
        } else if (hasPermission(Groups.ADMIN)) {
            return null
        } else if (access == Access.Read) {
            return if (hasPermission(Groups.READ)) {
                null
            } else {
                QueryBuilders.termsQuery("system.permissions.read",
                    getPermissionIds().map { it.toString() })
            }
        } else if (access == Access.Write) {
            return if (hasPermission(Groups.WRITE)) {
                null
            } else {
                QueryBuilders.termsQuery("system.permissions.write",
                    getPermissionIds().map { it.toString() })
            }
        } else if (access == Access.Export) {
            return if (hasPermission(Groups.EXPORT)) {
                null
            } else {
                QueryBuilders.termsQuery("system.permissions.export",
                    getPermissionIds().map { it.toString() })
            }
        } else if (access == Access.Delete) {
            return if (hasPermission(Groups.DELETE)) {
                null
            } else {
                QueryBuilders.termsQuery("system.permissions.delete",
                    getPermissionIds().map { it.toString() })
            }
        }

        return QueryBuilders.termsQuery("system.permissions.read",
            getPermissionIds().map { it.toString() })
    }

    override fun hasAccess(access: Access, asset: Document): Boolean {
        return if (useJwtFilter) {
            if (access == Access.Write && hasPermission(Groups.WRITE)) {
                true
            } else if (access == Access.Delete && hasPermission(Groups.DELETE)) {
                true
            } else if (access == Access.Export && hasPermission(Groups.EXPORT)) {
                true
            } else access == Access.Read && hasPermission(Groups.READ)
        } else {
            val perms = asset.getAttr("system.permissions.${access.field}", Json.SET_OF_UUIDS)
            hasPermission(perms)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AcccessResolverImpl::class.java)
    }
}