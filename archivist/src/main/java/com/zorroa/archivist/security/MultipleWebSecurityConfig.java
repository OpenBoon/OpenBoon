package com.zorroa.archivist.security;

import com.zorroa.archivist.ArchivistConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Created by chambers on 6/9/16.
 */
@EnableWebSecurity
public class MultipleWebSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(MultipleWebSecurityConfig.class);

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EnableGlobalMethodSecurity(prePostEnabled=true)
    public static class WebSecurityConfig extends WebSecurityConfigurerAdapter {

        private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

        @Autowired
        SessionRegistry sessionRegistry;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .addFilterBefore(new HmacSecurityFilter(), UsernamePasswordAuthenticationFilter.class)
                .antMatcher("/api/**")
                    .authorizeRequests()
                    .anyRequest().authenticated()
                .and()
                .httpBasic()
                .and().headers().frameOptions().disable()
                .and()
                .sessionManagement()
                .maximumSessions(10)
                .sessionRegistry(sessionRegistry)
                .and()
                .and()
                .csrf().disable();

            }
    }

    @Configuration
    public static class FormSecurityConfig extends WebSecurityConfigurerAdapter {

        @Autowired
        SessionRegistry sessionRegistry;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .authorizeRequests()
                    .antMatchers("/gui/**").authenticated()
                    .antMatchers("/health/**").permitAll()
                    .antMatchers("/cluster/**").permitAll()
                    .antMatchers("/console/**").hasAuthority("user::admin")
                .and()
                .formLogin()
                    .loginPage("/login").permitAll()
                    .failureUrl("/login?error").permitAll()
                    .defaultSuccessUrl("/gui")
                    .permitAll()
                .and()
                    .exceptionHandling()
                    .accessDeniedPage("/login")
                .and().headers().frameOptions().disable()
                .and()
                    .sessionManagement()
                    .invalidSessionUrl("/login")
                    .maximumSessions(10)
                    .sessionRegistry(sessionRegistry)
                    .and()
                .and()
                    .csrf().disable()
                .logout().logoutRequestMatcher(
                new AntPathRequestMatcher("/logout")).logoutSuccessUrl("/login?logout").permitAll();
        }
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .authenticationProvider(authenticationProvider())
                .authenticationProvider(hmacAuthenticationProvider())
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

            private final Logger logger = LoggerFactory.getLogger(FormSecurityConfig.class);

            @Override
            public void publishAuthenticationSuccess(
                    Authentication authentication) {
            }

            @Override
            public void publishAuthenticationFailure(
                    AuthenticationException exception,
                    Authentication authentication) {
                logger.info("Failed to authenticate: {}", authentication, exception);
            }
        };
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
