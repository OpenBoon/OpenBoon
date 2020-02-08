package com.zorroa.archivist.security

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import com.zorroa.archivist.MockMvcTest
import com.zorroa.zmlp.apikey.AuthServerClient
import org.junit.Test
import org.mockito.Mockito.mock
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.test.assertEquals

class ApiKeyAuthorizationFilterTests : MockMvcTest() {

    @Test
    fun testValidateToken() {
        val httpServletRequest: HttpServletRequest = mock(HttpServletRequest::class.java)
        val httpServletResponse: HttpServletResponse = mock(HttpServletResponse::class.java)
        val filterChain: FilterChain = mock(FilterChain::class.java)
        val filter = ApiKeyAuthorizationFilter(authServerClient)

        whenever(httpServletRequest.getHeader(eq("Authorization"))).thenReturn("Bearer JOBRUNNER")

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain)
        val actor = (SecurityContextHolder.getContext().authentication as ApiTokenAuthentication).zmlpActor
        assertEquals("JobRunner", actor.name)
    }

    @Test
    fun testValidateTokenWithProjectHeader() {
        val httpServletRequest: HttpServletRequest = mock(HttpServletRequest::class.java)
        val httpServletResponse: HttpServletResponse = mock(HttpServletResponse::class.java)
        val filterChain: FilterChain = mock(FilterChain::class.java)
        val filter = ApiKeyAuthorizationFilter(authServerClient)

        val projectId = UUID.randomUUID()
        whenever(httpServletRequest.getHeader(eq("Authorization"))).thenReturn("Bearer JOBRUNNER")
        whenever(httpServletRequest.getHeader(eq(AuthServerClient.PROJECT_ID_HEADER)))
            .thenReturn(projectId.toString())

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain)
        val actor = (SecurityContextHolder.getContext().authentication as ApiTokenAuthentication).zmlpActor
        assertEquals("JobRunner", actor.name)
        assertEquals(projectId, actor.projectId)
    }

    @Test
    fun testValidateTokenWithProjectParam() {
        val httpServletRequest: HttpServletRequest = mock(HttpServletRequest::class.java)
        val httpServletResponse: HttpServletResponse = mock(HttpServletResponse::class.java)
        val filterChain: FilterChain = mock(FilterChain::class.java)
        val filter = ApiKeyAuthorizationFilter(authServerClient)

        val projectId = UUID.randomUUID()
        whenever(httpServletRequest.getHeader(eq("Authorization"))).thenReturn("Bearer JOBRUNNER")
        whenever(httpServletRequest.getParameter(eq(AuthServerClient.PROJECT_ID_PARAM)))
            .thenReturn(projectId.toString())

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain)
        val actor = (SecurityContextHolder.getContext().authentication as ApiTokenAuthentication).zmlpActor
        assertEquals("JobRunner", actor.name)
        assertEquals(projectId, actor.projectId)
    }
}
