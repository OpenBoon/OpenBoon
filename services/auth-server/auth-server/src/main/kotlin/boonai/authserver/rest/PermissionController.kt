package boonai.authserver.rest

import boonai.common.apikey.Permission
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class PermissionController {

    @ApiOperation("Return a list of all permissions and their use.")
    @RequestMapping("/auth/v1/permissions", method = [RequestMethod.GET])
    fun getAll(): List<Map<String, Any?>> {
        return Permission.values()
            .filter { !it.internal }
            .map {
                val descr = it.javaClass.getField(it.name).getAnnotation(ApiModelProperty::class.java).value
                mapOf(
                    "name" to it.name,
                    "description" to descr
                )
            }
    }
}
