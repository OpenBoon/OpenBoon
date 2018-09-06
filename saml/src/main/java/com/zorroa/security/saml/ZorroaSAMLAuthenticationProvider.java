package com.zorroa.security.saml;

import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLCredential;

import java.util.Date;

public class ZorroaSAMLAuthenticationProvider extends SAMLAuthenticationProvider {

    @Override
    protected Date getExpirationDate(SAMLCredential credential) {
        return null;
    }
}
