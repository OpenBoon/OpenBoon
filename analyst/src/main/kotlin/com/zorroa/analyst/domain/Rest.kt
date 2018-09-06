package com.zorroa.analyst.domain

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
data class UpdateStatus(val status: MutableMap<String, Any> = mutableMapOf()) {

    constructor(op: String, state: Boolean) : this(mutableMapOf(op to state))
}
