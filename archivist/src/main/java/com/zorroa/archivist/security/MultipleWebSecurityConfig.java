package com.zorroa.archivist.security;

import com.zorroa.archivist.config.ArchivistConfiguration;
import com.zorroa.archivist.domain.LogAction;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserLogSpec;
import com.zorroa.archivist.service.EventLogService;
import com.zorroa.common.config.ApplicationProperties;
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
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsUtils;

/**
 * Created by chambers on 6/9/16.
 */
@EnableWebSecurity
public class MultipleWebSecurityConfig {

    @Autowired
    ApplicationProperties properties;

    @Autowired
    UserDetailsPopulator userDetailsPopulator;

    private static final Logger logger = LoggerFactory.getLogger(MultipleWebSecurityConfig.class);

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EnableGlobalMethodSecurity(prePostEnabled=true)
    public static class WebSecurityConfig extends WebSecurityConfigurerAdapter {

        private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

        @Bean
        public ResetPasswordSecurityFilter resetPasswordSecurityFilter() {
            return new ResetPasswordSecurityFilter();
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .addFilterBefore(new HmacSecurityFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(resetPasswordSecurityFilter(), HmacSecurityFilter.class)
                .addFilterBefore(new CorsCredentialsFilter(), ChannelProcessingFilter.class)
                .antMatcher("/api/**")
                    .authorizeRequests()
                    .antMatchers("/api/v1/reset-password").permitAll()
                    .antMatchers("/api/v1/send-password-reset-email").permitAll()
                    .antMatchers("/api/v1/send-onboard-email").permitAll()
                    .requestMatchers(CorsUtils::isCorsRequest).permitAll()
                    .anyRequest().authenticated()
                .and().headers().frameOptions().disable()
                .and()
                .httpBasic().and()
                .csrf().disable();
            }
    }

    @Configuration
    public static class FormSecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .authorizeRequests()
                    .antMatchers("/").authenticated()
                    .antMatchers("/gui/**").hasAuthority("group::administrator")
                    .antMatchers("/docs/**").permitAll()
                    .antMatchers("/signin/**").permitAll()
                    .antMatchers("/signout/**").permitAll()
                    .antMatchers("/health/**").permitAll()
                    .antMatchers("/console/**").hasAuthority("group::administrator")
                .and()
                    .exceptionHandling()
                    .accessDeniedPage("/signin")
                    .authenticationEntryPoint(
                            new LoginUrlAuthenticationEntryPoint("/signin"))
                .and().headers().frameOptions().disable()
                .and()
                    .sessionManagement()
                    .maximumSessions(5)
                    .expiredUrl("/signin")
                    .and()
                .and()
                .csrf().disable()
                .logout().logoutRequestMatcher(
                new AntPathRequestMatcher("/signout")).logoutSuccessUrl("/signin").permitAll();
        }
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, EventLogService logService) throws Exception {

        if (properties.getBoolean("archivist.security.ldap.enabled")) {
            auth.authenticationProvider(ldapAuthenticationProvider(userDetailsPopulator));
        }
        auth
                .authenticationProvider(authenticationProvider())
                .authenticationProvider(hmacAuthenticationProvider())
                .authenticationEventPublisher(authenticationEventPublisher(logService));

        /**
         * If its a unit test we add our rubber stamp authenticator.
         */
        if (ArchivistConfiguration.unittest) {
            auth.authenticationProvider(new UnitTestAuthenticationProvider());
        }
    }

    @Bean
    @Autowired
    public AuthenticationEventPublisher authenticationEventPublisher(EventLogService logService) {

        return new AuthenticationEventPublisher() {

            private final Logger logger = LoggerFactory.getLogger(FormSecurityConfig.class);

            @Override
            public void publishAuthenticationSuccess(
                    Authentication authentication) {
                try {
                    logService.logAsync(new UserLogSpec()
                            .setAction(LogAction.Login)
                            .setUser((User)authentication.getPrincipal()));
                } catch (Exception e) {
                    // If we throw here, the authentication fails, so if we can't log
                    // it then nobody can login.  Sorry L337 hackers
                    logger.warn("Failed to log user authentication", e);
                    throw new SecurityException(e);
                }
            }

            @Override
            public void publishAuthenticationFailure(
                    AuthenticationException exception,
                    Authentication authentication) {
                logger.info("Failed to authenticate: {}", authentication);
                logService.logAsync(new UserLogSpec()
                        .setAction(LogAction.LoginFailure)
                        .setMessage(authentication.getPrincipal().toString() + " failed to login, reason "
                        + exception.getMessage()));
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
    @Autowired
    public AuthenticationProvider ldapAuthenticationProvider(UserDetailsPopulator populator) throws Exception {
        String url = properties.getString("archivist.security.ldap.url");
        String base = properties.getString("archivist.security.ldap.base");
        String filter = properties.getString("archivist.security.ldap.filter");

        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(url);
        contextSource.setBase(base);
        contextSource.afterPropertiesSet();
        LdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch("", filter, contextSource);
        BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource);
        bindAuthenticator.setUserSearch(ldapUserSearch);
        LdapAuthenticationProvider ldapAuthenticationProvider =
                new LdapAuthenticationProvider(bindAuthenticator, populator);
        ldapAuthenticationProvider.setUserDetailsContextMapper(populator);
        return ldapAuthenticationProvider;
    }
}
