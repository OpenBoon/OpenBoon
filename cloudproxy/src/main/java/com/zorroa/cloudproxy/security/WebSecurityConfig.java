package com.zorroa.cloudproxy.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Created by chambers on 4/14/17.
 */
@EnableWebSecurity
@Configuration
public class WebSecurityConfig  extends WebSecurityConfigurerAdapter {


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
            .and()
            .exceptionHandling()
            .accessDeniedPage("/signin")
            .authenticationEntryPoint(
                new LoginUrlAuthenticationEntryPoint("/signin"))
            .and().headers().frameOptions().disable()
            .and()
            .csrf().disable()
            .logout().logoutRequestMatcher(
                new AntPathRequestMatcher("/signout")).logoutSuccessUrl("/signin").permitAll();

    }




}
