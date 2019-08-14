package com.zorroa.archivist.rest

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Sets
import com.zorroa.archivist.domain.ApiKeySpec
import com.zorroa.archivist.domain.LocalUserSpec
import com.zorroa.archivist.domain.Permission
import com.zorroa.archivist.domain.User
import com.zorroa.archivist.domain.UserFilter
import com.zorroa.archivist.domain.UserPasswordUpdate
import com.zorroa.archivist.domain.UserProfileUpdate
import com.zorroa.archivist.domain.UserSettings
import com.zorroa.archivist.domain.UserSpec
import com.zorroa.archivist.security.JwtSecurityConstants
import com.zorroa.archivist.security.MasterJwtValidator
import com.zorroa.archivist.security.TokenStore
import com.zorroa.archivist.security.getAuthentication
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.security.getUserId
import com.zorroa.archivist.security.getUsername
import com.zorroa.archivist.security.hasPermission
import com.zorroa.archivist.service.EmailService
import com.zorroa.archivist.service.PermissionService
import com.zorroa.archivist.service.UserService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.repository.KPagedList
import com.zorroa.security.Groups
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.security.Principal
import java.util.Objects
import java.util.UUID
import java.util.stream.Collectors
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
@Timed
@Api(tags = ["User"], description = "Operations for interacting with a list of Users.")
class UserController @Autowired constructor(
    private val userService: UserService,
    private val permissionService: PermissionService,
    private val emailService: EmailService,
    private val masterJwtValidator: MasterJwtValidator,
    private val tokenStore: TokenStore
) {

    @ApiOperation("DEPRECATED: Do not use.", hidden = true)
    @Deprecated("See /api/v1/users/_search")
    @RequestMapping(value = ["/api/v1/users"])
    fun getAll(): List<User> = userService.getAll()

    @ApiOperation(
        "Who am I?",
        notes = "Returns the current authenticated User."
    )
    @RequestMapping(value = ["/api/v1/who"])
    fun getCurrent(user: Principal?): ResponseEntity<Any> {
        return if (user != null) {
            ResponseEntity(userService.get(getUserId()), HttpStatus.OK)
        } else {
            ResponseEntity(mapOf("message" to "No authenticated user"), HttpStatus.UNAUTHORIZED)
        }
    }

    @ApiModel("API Key Request", description = "Request body used to set options for requesting an API key.")
    class ApiKeyReq(
        @ApiModelProperty("If true the current API key will removed and a new one will be issued.")
        val replace: Boolean = false,

        @ApiModelProperty("Sets the server the API key is tied to.")
        val server: String? = null
    )

    @ApiOperation(
        "Get an API key.",
        notes = "Returns an API key that can be used for sending requests from scripts or applications."
    )
    @RequestMapping(value = ["/api/v1/users/api-key"], method = [RequestMethod.GET, RequestMethod.POST])
    fun getApiKey(
        @ApiParam("Options for getting the API key.") @RequestBody(required = false) kreq: ApiKeyReq?,
        hreq: HttpServletRequest
    ): Any {
        val req = kreq ?: ApiKeyReq(false, null)

        /**
         * Select where the URI in the key is going to come from.
         */
        val uri = when {
            req.server != null -> req.server
            hreq.getHeader("X-Zorroa-Curator-Host") != null ->
                hreq.getHeader("X-Zorroa-Curator-Protocol") + "://" + hreq.getHeader("X-Zorroa-Curator-Host")
            else -> {
                val builder = ServletUriComponentsBuilder.fromCurrentRequestUri()
                builder.replacePath("/").build().toString()
            }
        }

        val user = getUser()
        val spec = ApiKeySpec(user.id, user.username, req.replace, uri)
        return userService.getApiKey(spec)
    }

    @ApiOperation(
        "Authenticate using a valid auth token.",
        notes = "Use token authentication to get logged in. Returns the X-Zorroa-Credential header with a valid JWT."
    )
    @PostMapping(value = ["/api/v1/auth/token"])
    fun tokenAuth(
        @RequestParam(value = "auth_token") token: String,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        // Clear out any current authentication.
        logout(request, response)
        val validatedToken = masterJwtValidator.validate(token)
        val user = validatedToken.provisionUser()
        if (user != null) {
            // Utilize the same token, since it will have special info from IRM
            response.setHeader("Location", "/")
            response.setHeader(JwtSecurityConstants.HEADER_STRING_RSP, JwtSecurityConstants.TOKEN_PREFIX + token)
            response.status = HttpServletResponse.SC_TEMPORARY_REDIRECT
        } else {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication failed")
        }
    }

    @ApiOperation(
        "HTTP-auth-based login.",
        notes = "Use standard HTTP authentication to get logged in. Returns the current user as well as a " +
            "X-Zorroa-Credential header with a valid JWT."
    )
    @PostMapping(value = ["/api/v1/login"])
    fun login(): ResponseEntity<User> {
        val user = getUser()
        val headers = HttpHeaders()

        val token = tokenStore.createSessionToken(user.id)
        headers.add(JwtSecurityConstants.HEADER_STRING_RSP, JwtSecurityConstants.TOKEN_PREFIX + token)

        return ResponseEntity.ok()
            .headers(headers)
            .body(userService.get(user.id))
    }

    @ApiOperation("HTTP-auth-based logout.")
    @RequestMapping(value = ["/api/v1/logout"], method = [RequestMethod.POST, RequestMethod.GET])
    fun logout(req: HttpServletRequest, rsp: HttpServletResponse): Any {

        val auth = getAuthentication()?.let { auth ->
            auth.credentials?.let { sessionId ->
                val session = sessionId.toString()
                if (session.isNotBlank()) {
                    tokenStore.removeSession(session)
                }
            }
            auth
        }
        SecurityContextHolder.clearContext()

        return if (auth == null) {
            mapOf("success" to false)
        } else {
            mapOf("success" to true)
        }
    }

    @ApiOperation("Reset your password.")
    @PostMapping(value = ["/api/v1/reset-password"])
    @Throws(ServletException::class)
    fun resetPasswordAndLogin(): User {
        return userService.get(getUserId())
    }

    @ApiModel("Send Forgot Password Email Request")
    class SendForgotPasswordEmailRequest {
        @ApiModelProperty("Address to send a forgot password email to.")
        var email: String? = null
    }

    @ApiOperation("Sends a password reset email.", hidden = true)
    @PostMapping(value = ["/api/v1/send-password-reset-email"])
    @Throws(ServletException::class)
    fun sendPasswordRecoveryEmail(@RequestBody req: SendForgotPasswordEmailRequest): Any {
        val user = userService.get(req.email!!)
        emailService.sendPasswordResetEmail(user)
        return HttpUtils.status("send-password-reset-email", "update", true)
    }

    @ApiOperation("Sends an onboard email.", hidden = true)
    @PostMapping(value = ["/api/v1/send-onboard-email"])
    @Throws(ServletException::class)
    fun sendOnboardEmail(@RequestBody req: SendForgotPasswordEmailRequest): Any {
        val user = userService.get(req.email!!)
        emailService.sendOnboardEmail(user)
        return HttpUtils.status("send-onboard-email", "update", true)
    }

    @ApiOperation("Create a User.")
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PostMapping(value = ["/api/v1/users"])
    fun create(@ApiParam("User to create.") @RequestBody builder: UserSpec): User {
        return userService.create(builder)
    }

    @ApiOperation("Create a User.")
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PostMapping(value = ["/api/v2/users"])
    fun createV2(@ApiParam("User to create.") @RequestBody spec: LocalUserSpec): User {
        return userService.create(spec)
    }

    @ApiOperation("Get a User.")
    @RequestMapping(value = ["/api/v1/users/{id}"])
    operator fun get(@ApiParam("UUID of the User.") @PathVariable id: UUID): User {
        validatePermissions(id)
        return userService.get(id)
    }

    @ApiOperation("Determine if a User exists.")
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @RequestMapping(value = ["/api/v1/users/{username}/_exists"])
    operator fun get(@ApiParam("Username.") @PathVariable username: String): Map<*, *> {
        return ImmutableMap.of("result", userService.exists(username, null))
    }

    @ApiOperation("Update a User's profile.")
    @PutMapping(value = ["/api/v1/users/{id}/_profile"])
    fun updateProfile(
        @ApiParam("Updated User profile.") @RequestBody form: UserProfileUpdate,
        @ApiParam("UUID of the User.") @PathVariable id: UUID
    ): Any {
        validatePermissions(id)
        val user = userService.get(id)
        return HttpUtils.updated("users", id, userService.update(user, form), userService.get(id))
    }

    @ApiOperation("Change a User's password.")
    @PutMapping(value = ["/api/v1/users/{id}/_password"])
    fun updatePassword(
        @ApiParam("New and old passwords.") @RequestBody form: UserPasswordUpdate,
        @ApiParam("UUID of the User.") @PathVariable id: UUID
    ): Any {
        validatePermissions(id)

        /**
         * If the Ids match, then the user is the current user, so validate the existing password.
         */
        if (id == getUserId()) {
            val storedPassword = userService.getPassword(getUsername())
            if (!BCrypt.checkpw(form.oldPassword, storedPassword)) {
                throw IllegalArgumentException("Existing password invalid")
            }
        }

        val user = userService.get(id)
        userService.resetPassword(user, form.newPassword)

        return HttpUtils.updated("users", id, true, user)
    }

    @ApiOperation("Update a User's settings.")
    @PutMapping(value = ["/api/v1/users/{id}/_settings"])
    fun updateSettings(
        @ApiParam("Updated settings.") @RequestBody settings: UserSettings,
        @ApiParam("UUID of the User.") @PathVariable id: UUID
    ): Any {
        validatePermissions(id)
        val user = userService.get(id)
        return HttpUtils.updated("users", id, userService.updateSettings(user, settings), userService.get(id))
    }

    @ApiOperation("Enable or disable a User.")
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PutMapping(value = ["/api/v1/users/{id}/_enabled"])
    fun disable(
        @ApiParam(
            "Object that sets the enabled status. The value of the 'enabled' entry must be 'true' or " +
                "'false. Example: {\"enabled\": true}"
        ) @RequestBody settings: Map<String, Boolean>,
        @ApiParam("UUID of the User.") @PathVariable id: UUID
    ): Any {
        val user = userService.get(id)
        if (id == getUserId()) {
            throw IllegalArgumentException("You cannot disable yourself")
        }

        if (settings["enabled"] == null) {
            throw IllegalArgumentException("missing 'enabled' value, must be true or false")
        }

        return HttpUtils.status(
            "users", id, "enable",
            userService.setEnabled(user, settings.getValue("enabled"))
        )
    }

    @ApiOperation("Delete a user")
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @DeleteMapping(value = ["/api/v1/users/{id}"])
    fun delete(
        @ApiParam("UUID of the User.") @PathVariable id: UUID
    ): Any {
        val user = userService.get(id)
        return HttpUtils.status(
            "users", id, "delete", userService.delete(user)
        )
    }

    @ApiOperation("Get Permissions for a User.")
    @GetMapping(value = ["/api/v1/users/{id}/permissions"])
    fun getPermissions(@ApiParam("UUID of the User.") @PathVariable id: UUID): List<Permission> {
        validatePermissions(id)
        val user = userService.get(id)
        return userService.getPermissions(user)
    }

    @ApiOperation(
        "Update Permissions for a User.",
        notes = "The complete list of permissions a User should have needs to be sent in the request."
    )
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PutMapping(value = ["/api/v1/users/{id}/permissions"])
    fun setPermissions(
        @ApiParam("list of Permission UUIDs to assign to the User.") @RequestBody pids: List<UUID>,
        @ApiParam("UUID of the User.") @PathVariable id: UUID
    ): List<Permission> {
        val user = userService.get(id)
        val perms = pids.stream().map { i -> permissionService.getPermission(i) }.collect(Collectors.toList())
        userService.setPermissions(user, perms)
        return userService.getPermissions(user)
    }

    @ApiOperation("Add Permissions to a User.")
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PutMapping(value = ["/api/v1/users/{id}/permissions/_add"])
    fun addPermissions(
        @ApiParam("List of Permission UUIDs to add to the User.") @RequestBody pids: List<String>,
        @ApiParam("UUID of the User.") @PathVariable id: UUID
    ): List<Permission> {
        val user = userService.get(id)
        val resolved = Sets.newHashSetWithExpectedSize<Permission>(pids.size)
        pids.mapTo(resolved) { permissionService.getPermission(it) }
        userService.addPermissions(user, resolved)
        return userService.getPermissions(user)
    }

    @ApiOperation("Remove Permissions from a User.")
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PutMapping(value = ["/api/v1/users/{id}/permissions/_remove"])
    fun removePermissions(
        @ApiParam("List of Permission UUIDs to remove from the User.") @RequestBody pids: List<String>,
        @ApiParam("UUID of the User.") @PathVariable id: UUID
    ): List<Permission> {
        val user = userService.get(id)
        val resolved = Sets.newHashSetWithExpectedSize<Permission>(pids.size)
        pids.mapTo(resolved) { permissionService.getPermission(it) }
        userService.removePermissions(user, resolved)
        return userService.getPermissions(user)
    }

    @ApiOperation("Search for Users.")
    @PostMapping(value = ["/api/v1/users/_search"])
    fun search(
        @ApiParam("Search filter.") @RequestBody(required = false) req: UserFilter?,
        @ApiParam("Result number to start from.") @RequestParam(value = "from", required = false) from: Int?,
        @ApiParam("Number of results per page.") @RequestParam(value = "count", required = false) count: Int?
    ): KPagedList<User> {
        val filter = req ?: UserFilter()
        from?.let { filter.page.from = it }
        count?.let { filter.page.size = it }
        return userService.getAll(filter)
    }

    @ApiOperation(
        "Search for a single User.",
        notes = "Throws an error if more than 1 result is returned based on the given filter."
    )
    @PostMapping(value = ["/api/v1/users/_findOne"])
    fun findOne(@ApiParam("Search filter.") @RequestBody(required = false) req: UserFilter?): User {
        return userService.findOne(req ?: UserFilter())
    }

    private fun validatePermissions(id: UUID) {
        if (!Objects.equals(getUserId(), id) && !hasPermission(Groups.MANAGER, Groups.ADMIN)) {
            throw SecurityException("Access denied.")
        }
    }
}
