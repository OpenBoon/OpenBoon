package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.ArchivistException
import boonai.archivist.domain.ArchivistSecurityException
import boonai.archivist.domain.DuplicateEntityException
import boonai.archivist.domain.EntityNotFoundException
import boonai.archivist.domain.InvalidRequestException
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.given
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.rest.RestStatus
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

class ErrorControllerTests : MockMvcTest() {

    @Mock
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var errorController: CustomErrorController

    @Autowired
    lateinit var restApiExceptionHandler: RestApiExceptionHandler

    @Autowired
    lateinit var errorMessages: HttpErrorMessages

    @Before
    override fun setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(restApiExceptionHandler, errorController).build()
    }

    @Test
    fun testThrowElasticSearchException() {

        doThrow(ElasticsearchStatusException("circuit_breaking_exception", RestStatus.TOO_MANY_REQUESTS))
            .`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.TOO_MANY_REQUESTS, errorMessages.tooManyRequests)
    }

    @Test
    fun testIncorrectResultSizeDataAccessException() {
        doThrow(IncorrectResultSizeDataAccessException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.METHOD_FAILURE, errorMessages.methodFailure)
    }

    @Test
    fun testDataRetrievalFailureException() {
        doThrow(DataRetrievalFailureException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.NOT_FOUND, errorMessages.notFound)

        doThrow(EntityNotFoundException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.NOT_FOUND, errorMessages.notFound)
    }

    @Test
    fun testDataIntegrityViolationException() {
        doThrow(DataIntegrityViolationException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.CONFLICT, errorMessages.conflict)

        doThrow(DuplicateEntityException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.CONFLICT, errorMessages.conflict)
    }

    @Test
    fun testAccessDeniedException() {
        doThrow(ArchivistSecurityException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.FORBIDDEN, errorMessages.forbidden)

        doThrow(AccessDeniedException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.FORBIDDEN, errorMessages.forbidden)
    }

    @Test
    fun testHttpRequestMethodNotSupportedException() {
        given(errorController.error(any())).willAnswer { throw HttpRequestMethodNotSupportedException("error message") }
        performExceptionRequest(HttpStatus.METHOD_NOT_ALLOWED, errorMessages.methodNotAllowed)

        doThrow(MethodArgumentTypeMismatchException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.METHOD_NOT_ALLOWED, errorMessages.methodNotAllowed)
    }

    @Test
    fun testBadRequestThrownMethodException() {
        doThrow(ArchivistException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.BAD_REQUEST, errorMessages.badRequest)

        doThrow(ElasticsearchException("es exception")).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.BAD_REQUEST, errorMessages.badRequest)

        doThrow(InvalidRequestException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.BAD_REQUEST, errorMessages.badRequest)

        doThrow(NullPointerException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.BAD_REQUEST, errorMessages.badRequest)

        doThrow(IllegalArgumentException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.BAD_REQUEST, errorMessages.badRequest)

        doThrow(IllegalStateException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.BAD_REQUEST, errorMessages.badRequest)

        doThrow(NumberFormatException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.BAD_REQUEST, errorMessages.badRequest)

        doThrow(ArrayIndexOutOfBoundsException::class).`when`(errorController).error(any())
        performExceptionRequest(HttpStatus.BAD_REQUEST, errorMessages.badRequest)
    }

    private fun performExceptionRequest(httpStatus: HttpStatus, errorMessage: String?) {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/error")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().`is`(httpStatus.value()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message", CoreMatchers.equalTo(errorMessage)))
    }
}
