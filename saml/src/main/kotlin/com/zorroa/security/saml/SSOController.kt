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

import com.zorroa.security.saml.SamlProperties
import com.zorroa.security.saml.ZorroaExtendedMetadata
import org.opensaml.saml2.metadata.provider.MetadataProviderException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml.metadata.MetadataManager
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.ModelAndView

import javax.servlet.http.HttpServletRequest
import java.util.*

@Controller
@RequestMapping("/saml")
class SSOController {

    @Autowired
    lateinit var metadata: MetadataManager

    @Autowired
    lateinit var properties: SamlProperties

    @RequestMapping(value = ["/lannding"], method = [RequestMethod.POST, RequestMethod.GET])
    fun landing(model: ModelMap): ModelAndView {
        return ModelAndView("redirect:" + properties.landingPage, model)
    }

    @GetMapping(value = ["/options"], produces = ["application/json"])
    @ResponseBody
    fun samlOptions(): Any {
        val idps = metadata!!.idpEntityNames
        val urls = mutableListOf<String>()
        for (idp in idps) {
            urls.add("/saml/login?disco=true&idp=$idp")
        }

        val result = mutableMapOf<String, Any>()
        result["logout"] = properties!!.isLogout
        result["discovery"] = properties.discovery
        result["landing"] = properties.landingPage
        result["baseUrl"] = properties.baseUrl
        result["proxyBase"] = properties.baseUrlIsProxy
        result["idps"] = urls
        return result
    }

    /*
    @RequestMapping(value = "/idpSelection")
    @Throws(MetadataProviderException::class)
    fun idpSelection(request: HttpServletRequest, model: Model): String {
        if (SecurityContextHolder.getContext().authentication !is AnonymousAuthenticationToken) {
            return "redirect:/"
        } else {
            if (isForwarded(request)) {
                val idps = metadata!!.idpEntityNames
                val props = mutableListOf<Properties>()

                for (idp in idps) {
                    val zd = metadata.getExtendedMetadata(idp) as ZorroaExtendedMetadata
                    zd.props.setProperty("idpUrl", "/saml/login?disco=true&idp=$idp")
                    props.add(zd.props)
                }
                model.addAttribute("idps", idps)
                model.addAttribute("props", props)

                return "saml/idpSelection"
            } else {
                //"Direct accesses to '/idpSelection' route are not allowed"
                return "redirect:/"
            }
        }
    }
    */

    /*
	 * Checks if an HTTP request has been forwarded by a servlet.
	 */
    private fun isForwarded(request: HttpServletRequest): Boolean {
        return request.getAttribute("javax.servlet.forward.request_uri") != null
    }

}
