package com.zorroa.security.saml

import org.springframework.security.saml.SAMLAuthenticationProvider
import org.springframework.security.saml.SAMLCredential

import java.util.Date

class ZorroaSAMLAuthenticationProvider : SAMLAuthenticationProvider() {

    override fun getExpirationDate(credential: SAMLCredential): Date? {
        return null
    }
}
