package com.zorroa.security.saml

import org.springframework.security.saml.metadata.MetadataGenerator
import org.springframework.security.saml.metadata.MetadataGeneratorFilter

import javax.servlet.http.HttpServletRequest

class ZorroaMetadataFilter(private val baseUrl: String, generator: MetadataGenerator) : MetadataGeneratorFilter(generator) {

    override fun getDefaultBaseURL(request: HttpServletRequest): String {
        return baseUrl
    }
}
