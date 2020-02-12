package com.zorroa.auth.server.security

import com.zorroa.zmlp.service.security.getZmlpActorOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import java.util.UUID
import javax.servlet.http.HttpServletRequest

@ControllerAdvice
class RestAPIException {

    @Autowired
    lateinit var errorAttributes: ErrorAttributes

    var debug: Boolean = false

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun defaultErrorHandler(wb: WebRequest, req: HttpServletRequest, e: Exception): ResponseEntity<Any> {

        val status = HttpStatus.UNAUTHORIZED

        /**
         * Each error gets its own random UUID for each searching in logs.
         */
        val errorId = UUID.randomUUID().toString()

        logger.error(
            "endpoint='{}' project='{}', errorId='{}',",
            req.servletPath,
            getZmlpActorOrNull()?.projectId,
            errorId
        )

        val errAttrs = errorAttributes.getErrorAttributes(wb, debug)
        errAttrs["errorId"] = errorId
        errAttrs["status"] = status.value()
        errAttrs["error"] = "DataIntegrityViolation"

        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(errAttrs)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RestAPIException::class.java)
    }
}
