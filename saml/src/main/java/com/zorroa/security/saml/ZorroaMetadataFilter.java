package com.zorroa.security.saml;

import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;

import javax.servlet.http.HttpServletRequest;

public class ZorroaMetadataFilter extends MetadataGeneratorFilter {

    private final String baseUrl;

    public ZorroaMetadataFilter(String baseUrl, MetadataGenerator generator) {
        super(generator);
        this.baseUrl = baseUrl;
    }

    @Override
    protected String getDefaultBaseURL(HttpServletRequest request) {
        return baseUrl;
    }
}
