package com.zorroa.archivist.event;

import com.zorroa.archivist.sdk.domain.ApplicationProperties;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;

@Configuration
public class EventServerConfiguration {

    @Autowired
    ApplicationProperties applicationProperties;

    @Bean
    public EventServer eventServer() {
        return new EventServer();
    }

    @Bean
    public EventServerHandler eventServerHandler() {
        return new EventServerHandler();
    }

    @Bean
    public EventServerInitializer eventServerInitializer() throws Exception {

        if (!applicationProperties.getBoolean("archivist.events.ssl")) {
            return new EventServerInitializer(null);
        }

        char[] keyStorePassword = applicationProperties.getString("server.ssl.key-store-password").toCharArray();
        char[] trustStorePassword = applicationProperties.getString("server.ssl.trust-store-password").toCharArray();

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new ClassPathResource(applicationProperties.getString("server.ssl.key-store")).getInputStream(),
                keyStorePassword);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyStorePassword);

        // truststore
        KeyStore ts = KeyStore.getInstance("PKCS12");
        ts.load(new ClassPathResource(applicationProperties.getString("server.ssl.trust-store")).getInputStream(),
                trustStorePassword);

        // set up trust manager factory to use our trust store
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        SslContext sslContext = SslContextBuilder.forServer(kmf).trustManager(tmf).build();
        return new EventServerInitializer(sslContext);
    }

}
