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

import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebArgumentResolver
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserHandlerMethodArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(methodParameter: MethodParameter): Boolean {
        return methodParameter.getParameterAnnotation(CurrentUser::class.java) != null && methodParameter.parameterType == User::class.java
    }

    @Throws(Exception::class)
    override fun resolveArgument(
        methodParameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? {
        return if (this.supportsParameter(methodParameter)) {
            logger.info("User principal: {}", webRequest.userPrincipal)

            (webRequest.userPrincipal as Authentication).principal as User
        } else {
            WebArgumentResolver.UNRESOLVED
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(CurrentUserHandlerMethodArgumentResolver::class.java)
    }
}
