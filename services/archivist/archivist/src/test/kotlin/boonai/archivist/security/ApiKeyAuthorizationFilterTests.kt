package boonai.archivist.security

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import boonai.archivist.MockMvcTest
import boonai.archivist.domain.ProjectSpec
import boonai.common.apikey.AuthServerClient
import org.junit.Test
import org.mockito.Mockito.mock
import org.springframework.security.core.context.SecurityContextHolder
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
        val filter = ApiKeyAuthorizationFilter(authServerClient, projectService)

        whenever(httpServletRequest.getHeader(eq("Authorization"))).thenReturn("Bearer JOBRUNNER")

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain)
        val actor = (SecurityContextHolder.getContext().authentication as ApiTokenAuthentication).zmlpActor
        assertEquals("JobRunner", actor.name)
    }

    @Test
    fun testValidateTokenDisabledProject() {
        val httpServletRequest: HttpServletRequest = mock(HttpServletRequest::class.java)
        val httpServletResponse: HttpServletResponse = mock(HttpServletResponse::class.java)
        val filterChain: FilterChain = mock(FilterChain::class.java)
        val filter = ApiKeyAuthorizationFilter(authServerClient, projectService)

        whenever(httpServletRequest.getHeader(eq("Authorization"))).thenReturn("Bearer JOBRUNNER")
        projectService.setEnabled(getProjectId(), false)

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain)
        verify(httpServletResponse, times(1)).sendError(
            HttpServletResponse.SC_UNAUTHORIZED, "Not Authorized"
        )
    }

    @Test
    fun testValidateTokenWithProjectHeader() {
        val httpServletRequest: HttpServletRequest = mock(HttpServletRequest::class.java)
        val httpServletResponse: HttpServletResponse = mock(HttpServletResponse::class.java)
        val filterChain: FilterChain = mock(FilterChain::class.java)
        val filter = ApiKeyAuthorizationFilter(authServerClient, projectService)
        val project = projectService.create(ProjectSpec("foo"))

        whenever(httpServletRequest.getHeader(eq("Authorization"))).thenReturn("Bearer JOBRUNNER")
        whenever(httpServletRequest.getHeader(eq(AuthServerClient.PROJECT_ID_HEADER)))
            .thenReturn(project.id.toString())

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain)
        val actor = (SecurityContextHolder.getContext().authentication as ApiTokenAuthentication).zmlpActor
        assertEquals("JobRunner", actor.name)
        assertEquals(project.id, actor.projectId)
    }

    @Test
    fun testValidateTokenWithProjectParam() {
        val httpServletRequest: HttpServletRequest = mock(HttpServletRequest::class.java)
        val httpServletResponse: HttpServletResponse = mock(HttpServletResponse::class.java)
        val filterChain: FilterChain = mock(FilterChain::class.java)
        val filter = ApiKeyAuthorizationFilter(authServerClient, projectService)
        val project = projectService.create(ProjectSpec("foo"))

        whenever(httpServletRequest.getHeader(eq("Authorization"))).thenReturn("Bearer JOBRUNNER")
        whenever(httpServletRequest.getParameter(eq(AuthServerClient.PROJECT_ID_PARAM)))
            .thenReturn(project.id.toString())

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain)
        val actor = (SecurityContextHolder.getContext().authentication as ApiTokenAuthentication).zmlpActor
        assertEquals("JobRunner", actor.name)
        assertEquals(project.id, actor.projectId)
    }
}
