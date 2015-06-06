package com.zorroa.archivist.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.context.request.RequestContextHolder;

import com.zorroa.archivist.service.RoomService;

@Configuration
@EnableWebMvcSecurity
@EnableGlobalMethodSecurity(prePostEnabled=true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Autowired
    RoomService roomService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/").permitAll()
                .anyRequest().authenticated()
            .and()
                .httpBasic()
            .and()
                .sessionManagement()
                    .maximumSessions(10)
                    .sessionRegistry(sessionRegistry())
                .and()
            .and()
            	.csrf().disable();
            
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .authenticationProvider(authenticationProvider())
            .authenticationEventPublisher(athenticationEventPublisher());
    }

    @Bean
    public AuthenticationEventPublisher athenticationEventPublisher() {
        return new AuthenticationEventPublisher() {

            @Override
            public void publishAuthenticationSuccess(
                    Authentication authentication) {
                logger.info("Auth Session: {}",
                        RequestContextHolder.currentRequestAttributes().getSessionId());
            }

            @Override
            public void publishAuthenticationFailure(
                    AuthenticationException exception,
                    Authentication authentication) {
            }
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        return new ElasticAuthenticationProvider();
    }

    @Bean
    public HazelcastSessionRegistry sessionRegistry() {
        return new HazelcastSessionRegistry();
    }
}
