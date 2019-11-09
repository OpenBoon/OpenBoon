package com.zorroa.archivist

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nhaarman.mockito_kotlin.any
import com.zorroa.archivist.clients.ZmlpUser
import com.zorroa.archivist.rest.MockSecurityContext
import com.zorroa.archivist.security.AnalystAuthentication
import com.zorroa.archivist.security.Role
import com.zorroa.archivist.util.Json
import org.junit.Before
import org.mockito.Mockito
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
import java.util.UUID

abstract class MockMvcTest : AbstractTest() {

    @Autowired
    lateinit var wac: WebApplicationContext

    @Autowired
    lateinit var springSecurityFilterChain: FilterChainProxy

    lateinit var mvc: MockMvc

    @Before
    @Throws(IOException::class)
    override fun setup() {
        super.setup()
        mvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .addFilters<DefaultMockMvcBuilder>(springSecurityFilterChain)
            .build()

        Mockito.`when`(authServerClient.authenticate(any())).then {
            ZmlpUser(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                project.id,
                "unittest-key",
                listOf(Role.SUPERADMIN, Role.PROJADMIN)
            )
        }
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
        val headers = HttpHeaders()
        headers["Authorization"] = "Bearer 123"
        return headers
    }

    protected fun analyst(): MockHttpSession {
        return buildSession(AnalystAuthentication("https://127.0.0.1:5000"))
    }
}
