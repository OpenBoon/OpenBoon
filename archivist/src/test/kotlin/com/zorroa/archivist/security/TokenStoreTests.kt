package com.zorroa.archivist.security

import com.auth0.jwt.JWT
import com.zorroa.archivist.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TokenStoreTests : AbstractTest() {

    @Autowired
    lateinit var tokenStore: TokenStore

    @Test
    fun testcreateSessionToken() {
        val user = userService.get("admin")
        val token = tokenStore.createSessionToken(user.id)

        val jwt = JWT.decode(token)
        // Must have a token EXP date.
        assertNotNull(jwt.expiresAt)
        // Must have a session Id.
        assertNotNull(jwt.claims["sessionId"])
        // Must have a userId
        assertEquals(user.id, UUID.fromString(jwt.claims["userId"]?.asString()))
    }
}