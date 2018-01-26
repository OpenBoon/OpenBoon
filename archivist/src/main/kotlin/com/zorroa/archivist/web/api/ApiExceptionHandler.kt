package com.zorroa.archivist.web.api

import com.zorroa.archivist.web.InvalidObjectException
import com.zorroa.sdk.client.exception.*
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.util.UrlPathHelper
import javax.servlet.http.HttpServletRequest

class Error(r: HttpServletRequest, e:Exception, clazz: Class<*>) {
    val exception: String = clazz.canonicalName
    val cause: String = e.javaClass.canonicalName
    val message : String? = e.message
    val path : String = pathHelper.getRequestUri(r)

    companion object {
        val pathHelper = UrlPathHelper()
    }
}

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(Exception::class)
    fun  defaultErrorHandler (r: HttpServletRequest, e: Exception) : ResponseEntity<Error> {

        if (e is EmptyResultDataAccessException || e is EntityNotFoundException) {
            return ResponseEntity(Error(r, e, EntityNotFoundException::class.java), HttpStatus.NOT_FOUND)
        }
        else if (e is DataIntegrityViolationException || e is DuplicateEntityException) {
            return ResponseEntity(Error(r, e, DuplicateEntityException::class.java),HttpStatus.CONFLICT)
        }
        else if (e is ArchivistWriteException) {
            return ResponseEntity(Error(r, e, ArchivistWriteException::class.java),HttpStatus.EXPECTATION_FAILED)
        }
        else if (e is InvalidObjectException ||
                e is InvalidRequestException ||
                e is DataAccessException ||
                e is NullPointerException ||
                e is IllegalArgumentException ||
                e is IllegalStateException ||
                e is NumberFormatException ||
                e is ArrayIndexOutOfBoundsException ||
                e is MethodArgumentTypeMismatchException) {
            return ResponseEntity(Error(r, e, InvalidRequestException::class.java),HttpStatus.BAD_REQUEST)
        }
        else {
            return  ResponseEntity(Error(r, e, ArchivistException::class.java), HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}
