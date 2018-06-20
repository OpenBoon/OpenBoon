package com.zorroa.security.saml;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties("archivist.security.saml")
public class SamlProperties {

    public Map<String,String> keystore;
    public boolean discovery = true;
    public String baseUrl;


}
