package com.zorroa.archivist.security;

import com.zorroa.archivist.ArchivistConfiguration;
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
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled=true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(hmacAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
                .antMatchers("/").permitAll()
                .antMatchers("/health/**").permitAll()
                .antMatchers("/api/v1/analyst/_register").permitAll()
                .antMatchers("/api/v1/analyst/_shutdown").permitAll()
                .antMatchers("/console/**").hasAuthority("user::admin")
                .anyRequest().authenticated()
            .and()
                .httpBasic()
            .and().headers().frameOptions().disable()
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
            .authenticationProvider(hmacAuthenticationProvider())
            .authenticationProvider(backgroundTaskAuthenticationProvider())
            .authenticationProvider(internalAuthenticationProvider())
            .authenticationEventPublisher(authenticationEventPublisher());

        /**
         * If its a unit test we add our rubber stamp authenticator.
         */
        if (ArchivistConfiguration.unittest) {
            auth.authenticationProvider(new UnitTestAuthenticationProvider());
        }
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
    public HmacSecurityFilter hmacAuthenticationFilter() {
        HmacSecurityFilter filter = new HmacSecurityFilter();
        return filter;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        return new ZorroaAuthenticationProvider();
    }

    @Bean
    public AuthenticationProvider hmacAuthenticationProvider() {
        return new HmacAuthenticationProvider();
    }

    @Bean
    public AuthenticationProvider backgroundTaskAuthenticationProvider() {
        return new BackgroundTaskAuthenticationProvider();
    }

    @Bean
    public AuthenticationProvider internalAuthenticationProvider() {
        return new InternalAuthenticationProvider();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new JdbcSessionRegistry();
    }
}
