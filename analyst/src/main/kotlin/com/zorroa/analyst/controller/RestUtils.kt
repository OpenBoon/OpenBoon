package com.zorroa.analyst.controller

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Thrown from controller endpoints when not enough data was supplied
 * to process the request.
 */
@ResponseStatus(HttpStatus.PRECONDITION_FAILED)
class PreconditionFailedException(message: String) : RuntimeException(message)

/**
 * A standard response for a controller method.
 */
data class RestResponse(val method : HttpMethod,
                        val endpoint : String,
                        val success: Boolean)
