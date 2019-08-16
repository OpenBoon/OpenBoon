/*
 * Copyright 2017 Vincenzo De Notaris
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zorroa.security.saml

import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.sdk.security.UserRegistryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.saml.metadata.CachingMetadataManager
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpSession

@Controller
@RequestMapping("/saml")
class SSOController {

    @Autowired
    lateinit var metadata: CachingMetadataManager

    @Autowired
    lateinit var properties: SamlProperties

    @Autowired
    lateinit var userRegistryService: UserRegistryService

    @RequestMapping(value = ["/landing"], method = [RequestMethod.POST, RequestMethod.GET])
    fun landing(model: ModelMap, session: HttpSession, authentication: Authentication): ModelAndView {
        val user = authentication.details as UserAuthed
        val token = userRegistryService.createSessionToken(user.id)
        val redirect = "redirect:${properties.baseUrl}/api/v1/auth/token?auth_token=$token"

        session.invalidate()
        return ModelAndView(redirect, model)
    }

    @GetMapping(value = ["/options"], produces = ["application/json"])
    @ResponseBody
    fun samlOptions(): Any {
        val idps = metadata.idpEntityNames
        val urls = mutableListOf<String>()
        val propList = mutableListOf<Map<Any, Any>>()

        for (idp in idps) {
            val url = "/saml/login?disco=true&idp=$idp"
            urls.add(url)

            val ext = metadata.getExtendedMetadata(idp) as ZorroaExtendedMetadata
            val map = ext.props.toMutableMap()
            map["loginUrl"] = url
            map["entityId"] = idp
            propList.add(map)
        }

        val result = mutableMapOf<String, Any>()
        result["logout"] = properties.logout
        result["landing"] = properties.landingPage
        result["baseUrl"] = properties.baseUrl
        result["idps"] = urls
        result["idp-props"] = propList
        return result
    }
}
