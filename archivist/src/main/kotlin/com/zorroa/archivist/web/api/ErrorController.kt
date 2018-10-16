package com.zorroa.archivist.web.api

import com.zorroa.archivist.security.getUserOrNull
import com.zorroa.archivist.web.InvalidObjectException
import com.zorroa.common.domain.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.ModelAndView
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * The custom error controller handles serializing exceptions that bubble
 * out of controllers into a JSON response.
 */
@RestController
class CustomErrorController : ErrorController {

    val path = "/error"

    @Value("\${archivist.debug-mode.enabled}")
    var debug : Boolean = false

    @Autowired
    lateinit var errorAttributes: ErrorAttributes

    /**
     * This single point serves all exception data for the entire app.
     */
    @RequestMapping("/error", produces = ["application/json"])
    fun handleError(req: WebRequest, rsp: HttpServletResponse) : Any {
        val errAttrs = errorAttributes.getErrorAttributes(req, debug)
        val errorId = req.getAttribute("errorId", 0)
        errAttrs["errorId"] = errorId

        if (!debug) {
            errAttrs["message"] = "An unexpected error was encountered. When reporting this please use error ID '$errorId'."
        }
        return ResponseEntity.status(rsp.status).body(errAttrs)
    }

    override fun getErrorPath(): String  = path
}

/**
 * The RestApiExceptionHandler converts different types of excepts into HTTP response codes.
 */
@ControllerAdvice
class RestApiExceptionHandler {

    @ExceptionHandler(Exception::class)
    fun  defaultErrorHandler(r: HttpServletRequest, e: Exception) : ModelAndView {

        /**
         * Each error gets its own random UUID for each searching in logs.
         */
        val errorId = UUID.randomUUID().toString()
        r.setAttribute("errorId", errorId)
        logger.error("endpoint='{}' user='{}', errorId='{}',",
                r.servletPath, getUserOrNull()?.toString(), errorId, e)

        val mve = ModelAndView("/error")
        val annotation = AnnotationUtils.findAnnotation(e::class.java, ResponseStatus::class.java)

        if (annotation != null) {
            mve.status = annotation.value
        }
        else if (e is EmptyResultDataAccessException || e is EntityNotFoundException) {
            mve.status = HttpStatus.NOT_FOUND
        }
        else if (e is DataIntegrityViolationException || e is DuplicateEntityException) {
            mve.status = HttpStatus.CONFLICT
        }
        else if (e is ArchivistSecurityException) {
            mve.status = HttpStatus.UNAUTHORIZED
        }
        else if (e is ArchivistException ||
                e is InvalidObjectException ||
                e is InvalidRequestException ||
                e is DataAccessException ||
                e is NullPointerException ||
                e is IllegalArgumentException ||
                e is IllegalStateException ||
                e is NumberFormatException ||
                e is ArrayIndexOutOfBoundsException ||
                e is MethodArgumentTypeMismatchException) {
            mve.status = HttpStatus.BAD_REQUEST
        }
        else {
            mve.status = HttpStatus.INTERNAL_SERVER_ERROR
        }

        return  mve
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RestApiExceptionHandler::class.java)
    }
}
