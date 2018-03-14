package com.zorroa.security.saml;

import org.springframework.security.saml.metadata.ExtendedMetadata;

import java.util.Properties;

public class ZorroaExtendedMetadata extends ExtendedMetadata {

    private Properties props;

    public Properties getProps() {
        return props;
    }

    public ZorroaExtendedMetadata setProps(Properties props) {
        this.props = props;
        return this;
    }

    public String getProp(String key) {
        return props.getProperty(key);
    }
}
