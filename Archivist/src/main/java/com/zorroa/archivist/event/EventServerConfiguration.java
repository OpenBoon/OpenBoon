package com.zorroa.archivist.event;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@Configuration
public class EventServerConfiguration {

    @Bean
    public HazelcastInstance hazelCastInstance() {
        return Hazelcast.newHazelcastInstance();
    }

    @Bean
    public EventServer eventServer() {
        return new EventServer();
    }

    @Bean
    public EventServerHandler eventServerHandler() {
        return new EventServerHandler();
    }

    @Bean
    public EventServerInitializer eventServerInitializer() {
        return new EventServerInitializer();
    }

}
