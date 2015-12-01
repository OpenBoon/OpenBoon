package com.zorroa.archivist.security;

import com.zorroa.archivist.sdk.service.RoomService;
import com.zorroa.archivist.sdk.service.UserService;
import com.zorroa.archivist.web.JdbcSessionRegistry;
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
import org.springframework.security.core.session.SessionRegistry;


@Configuration
@EnableWebMvcSecurity
@EnableGlobalMethodSecurity(prePostEnabled=true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Autowired
    RoomService roomService;

    @Autowired
    UserService userService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/").permitAll()
                .antMatchers("/health/**").permitAll()
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
            .authenticationProvider(backgroundTaskAuthenticationProvider())
            .authenticationEventPublisher(authenticationEventPublisher());
    }

    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher() {
        return new AuthenticationEventPublisher() {

            @Override
            public void publishAuthenticationSuccess(
                    Authentication authentication) {
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
        return new ZorroaAuthenticationProvider();
    }

    @Bean
    public AuthenticationProvider backgroundTaskAuthenticationProvider() {
        return new BackgroundTaskAuthenticationProvider();
    }


    @Bean
    public SessionRegistry sessionRegistry() {
        return new JdbcSessionRegistry();
    }
}
