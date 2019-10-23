package com.zorroa.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.security.AnalystAuthentication
import com.zorroa.common.util.Json
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.FilterChainProxy
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
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

    /**
     * Assert successful post to API endpoint
     * @return an an object of the
     */
    final inline fun <reified T : Any> resultForPostContent(
        urlTemplate: String,
        `object`: Any,
        session: HttpHeaders = admin()
    ): T {
        // MockMvc clears the security context when it returns, I don't know how to configure it otherwise.
        val savedAuthentication = SecurityContextHolder.getContext().authentication
        val result = this.mvc.perform(
            MockMvcRequestBuilders
                .post(urlTemplate)
                .headers(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(`object`))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        SecurityContextHolder.getContext().authentication = savedAuthentication
        val mapper = ObjectMapper().registerKotlinModule()
        return mapper.readValue(result.response.contentAsString)
    }

    fun assertClientErrorForPostContent(
        urlTemplate: String,
        `object`: Any,
        session: HttpHeaders = admin()
    ) {
        val savedAuthentication = SecurityContextHolder.getContext().authentication
        this.mvc.perform(
            MockMvcRequestBuilders
                .post(urlTemplate)
                .headers(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(`object`))
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
        SecurityContextHolder.getContext().authentication = savedAuthentication
    }

    private fun buildSession(authentication: Authentication): MockHttpSession {
        val session = MockHttpSession()
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            MockSecurityContext(authentication)
        )
        return session
    }

    protected fun <T> deserialize(result: MvcResult, type: Class<T>): T {
        return Json.deserialize(result.response.contentAsByteArray, type)
    }

    protected fun <T> deserialize(result: MvcResult, type: TypeReference<T>): T {
        return Json.deserialize(result.response.contentAsByteArray, type)
    }

    /**
     * @return a session for an admin with the id 1.
     */
    protected fun admin(): HttpHeaders {
        return HttpHeaders()
    }

    protected fun analyst(): MockHttpSession {
        return buildSession(AnalystAuthentication("https://127.0.0.1:5000"))
    }

    class StatusResult<T> {
        var `object`: T? = null
        var op: String? = null
        var id: String? = null
        var success: Boolean = false
    }
}
