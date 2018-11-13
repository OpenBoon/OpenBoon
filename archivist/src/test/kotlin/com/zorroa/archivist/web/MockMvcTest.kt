package com.zorroa.archivist.web

import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.security.UnitTestAuthentication
import com.zorroa.common.util.Json
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.core.Authentication
import org.springframework.security.web.FilterChainProxy
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

import java.io.IOException

abstract class MockMvcTest : AbstractTest() {

    @Autowired
    protected var wac: WebApplicationContext? = null

    @Autowired
    protected var springSecurityFilterChain: FilterChainProxy? = null

    lateinit var mvc: MockMvc

    @Before
    @Throws(IOException::class)
    override fun setup() {
        super.setup()
        this.mvc = MockMvcBuilders
                .webAppContextSetup(this.wac!!)
                .addFilters<DefaultMockMvcBuilder>(springSecurityFilterChain!!)
                .build()
    }

    private fun buildSession(authentication: Authentication): MockHttpSession {
        val session = MockHttpSession()
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, MockSecurityContext(authentication))

        return session
    }

    protected fun <T> deserialize(result: MvcResult, type: Class<T>): T {
        return Json.deserialize(result.response.contentAsByteArray, type)
    }

    protected fun <T> deserialize(result: MvcResult, type: TypeReference<T>): T {
        return Json.deserialize(result.response.contentAsByteArray, type)
    }

    @JvmOverloads
    protected fun user(name: String = "user"): MockHttpSession {
        val user = userRegistryService.getUser(name)
        return buildSession(UnitTestAuthentication(user, user.authorities))
    }

    /**
     * @return a session for an admin with the id 1.
     */
    protected fun admin(): MockHttpSession {
        return user("admin")
    }

    class StatusResult<T> {
        var `object`: T? = null
        var op: String? = null
        var id: String? = null
        var success: Boolean = false
    }
}
/**
 * @return a session for an employee with the id 2.
 */
