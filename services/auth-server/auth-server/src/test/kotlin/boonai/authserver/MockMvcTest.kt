package boonai.authserver

import boonai.authserver.domain.ValidationKey
import boonai.authserver.security.AUTH_HEADER
import boonai.authserver.security.TOKEN_PREFIX
import java.util.UUID
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.http.HttpHeaders
import org.springframework.security.web.FilterChainProxy
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class MockMvcTest : AbstractTest() {

    @Autowired
    lateinit var wac: WebApplicationContext

    @Autowired
    lateinit var springSecurityFilterChain: FilterChainProxy

    lateinit var mvc: MockMvc

    @Before
    fun setupMvc() {
        this.mvc = MockMvcBuilders
            .webAppContextSetup(this.wac)
            .addFilters<DefaultMockMvcBuilder>(springSecurityFilterChain)
            .build()
    }

    protected fun superAdmin(projectId: UUID? = null): HttpHeaders {
        val headers = HttpHeaders()
        headers.set(
            AUTH_HEADER,
            "${TOKEN_PREFIX}${externalApiKey.getJwtToken(projId = projectId)}"
        )
        return headers
    }

    protected fun standardUser(apiKey: ValidationKey): HttpHeaders {
        val headers = HttpHeaders()
        headers.set(
            AUTH_HEADER,
            "${TOKEN_PREFIX}${apiKey.getJwtToken()}"
        )
        return headers
    }
}
