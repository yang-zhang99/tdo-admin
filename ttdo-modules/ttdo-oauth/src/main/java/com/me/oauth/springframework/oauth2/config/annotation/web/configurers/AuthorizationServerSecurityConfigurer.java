/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.me.oauth.springframework.oauth2.config.annotation.web.configurers;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.ClientCredentialsTokenEndpointFilter;
import org.springframework.security.oauth2.provider.client.ClientDetailsUserDetailsService;
import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpointHandlerMapping;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Rob Winch
 * @author Dave Syer
 * @author XCXCXCXCX update on 2019/5/16
 * @since 2.0
 */
public final class AuthorizationServerSecurityConfigurer extends
        SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    private AuthenticationEntryPoint authenticationEntryPoint;

    private AccessDeniedHandler accessDeniedHandler = new OAuth2AccessDeniedHandler();

    private PasswordEncoder passwordEncoder; // for client secrets

    private String realm = "oauth2/client";

    private boolean allowFormAuthenticationForClients = false;

    private String tokenKeyAccess = "denyAll()";

    private String checkTokenAccess = "denyAll()";

    private boolean sslOnly = false;

    /**
     * Custom authentication filters for the TokenEndpoint. Filters will be set upstream of the default
     * BasicAuthenticationFilter.
     */
    private List<Filter> tokenEndpointAuthenticationFilters = new ArrayList<Filter>();

    public AuthorizationServerSecurityConfigurer sslOnly() {
        this.sslOnly = true;
        return this;
    }

    public AuthorizationServerSecurityConfigurer allowFormAuthenticationForClients() {
        this.allowFormAuthenticationForClients = true;
        return this;
    }

    public AuthorizationServerSecurityConfigurer realm(String realm) {
        this.realm = realm;
        return this;
    }

    public AuthorizationServerSecurityConfigurer passwordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        return this;
    }

    public AuthorizationServerSecurityConfigurer authenticationEntryPoint(
            AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        return this;
    }

    public AuthorizationServerSecurityConfigurer accessDeniedHandler(AccessDeniedHandler accessDeniedHandler) {
        this.accessDeniedHandler = accessDeniedHandler;
        return this;
    }

    public AuthorizationServerSecurityConfigurer tokenKeyAccess(String tokenKeyAccess) {
        this.tokenKeyAccess = tokenKeyAccess;
        return this;
    }

    public AuthorizationServerSecurityConfigurer checkTokenAccess(String checkTokenAccess) {
        this.checkTokenAccess = checkTokenAccess;
        return this;
    }

    public String getTokenKeyAccess() {
        return tokenKeyAccess;
    }

    public String getCheckTokenAccess() {
        return checkTokenAccess;
    }

    @Override
    public void init(HttpSecurity http) throws Exception {

        registerDefaultAuthenticationEntryPoint(http);
        if (passwordEncoder != null) {
            ClientDetailsUserDetailsService clientDetailsUserDetailsService = new ClientDetailsUserDetailsService(clientDetailsService());
            clientDetailsUserDetailsService.setPasswordEncoder(passwordEncoder());
            http.getSharedObject(AuthenticationManagerBuilder.class)
                    .userDetailsService(clientDetailsUserDetailsService)
                    .passwordEncoder(passwordEncoder());
        }
        else {
            http.userDetailsService(new ClientDetailsUserDetailsService(clientDetailsService()));
        }
        http.securityContext().securityContextRepository(new NullSecurityContextRepository()).and().csrf().disable()
                .httpBasic().realmName(realm);
        if (sslOnly) {
            http.requiresChannel().anyRequest().requiresSecure();
        }
    }

    private PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return StringUtils.hasText(encodedPassword) ? passwordEncoder.matches(rawPassword, encodedPassword)
                        : true;
            }

            @Override
            public String encode(CharSequence rawPassword) {
                return passwordEncoder.encode(rawPassword);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private void registerDefaultAuthenticationEntryPoint(HttpSecurity http) {
        ExceptionHandlingConfigurer<HttpSecurity> exceptionHandling = http
                .getConfigurer(ExceptionHandlingConfigurer.class);
        if (exceptionHandling == null) {
            return;
        }
        if (authenticationEntryPoint==null) {
            BasicAuthenticationEntryPoint basicEntryPoint = new BasicAuthenticationEntryPoint();
            basicEntryPoint.setRealmName(realm);
            authenticationEntryPoint = basicEntryPoint;
        }
        ContentNegotiationStrategy contentNegotiationStrategy = http.getSharedObject(ContentNegotiationStrategy.class);
        if (contentNegotiationStrategy == null) {
            contentNegotiationStrategy = new HeaderContentNegotiationStrategy();
        }
        MediaTypeRequestMatcher preferredMatcher = new MediaTypeRequestMatcher(contentNegotiationStrategy,
                MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_XML, MediaType.MULTIPART_FORM_DATA,
                MediaType.TEXT_XML);
        preferredMatcher.setIgnoredMediaTypes(Collections.singleton(MediaType.ALL));
        exceptionHandling.defaultAuthenticationEntryPointFor(postProcess(authenticationEntryPoint), preferredMatcher);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {

        // ensure this is initialized
        frameworkEndpointHandlerMapping();
        if (allowFormAuthenticationForClients) {
            clientCredentialsTokenEndpointFilter("/oauth/token",http);
            //custom
            clientCredentialsTokenEndpointFilter("/oauth/user",http);
            List<RequestMatcher> matcherList = new ArrayList<>();
            matcherList.add(new AntPathRequestMatcher("/oauth/token"));
            matcherList.add(new AntPathRequestMatcher("/oauth/token_key"));
            matcherList.add(new AntPathRequestMatcher("/oauth/check_token"));
            matcherList.add(new AntPathRequestMatcher("/oauth/user"));
            http.requestMatcher(new OrRequestMatcher(matcherList));
        }

        for (Filter filter : tokenEndpointAuthenticationFilters) {
            http.addFilterBefore(filter, BasicAuthenticationFilter.class);
        }

        http.exceptionHandling().accessDeniedHandler(accessDeniedHandler);
    }

    private ClientCredentialsTokenEndpointFilter clientCredentialsTokenEndpointFilter(String path, HttpSecurity http) {
        ClientCredentialsTokenEndpointFilter clientCredentialsTokenEndpointFilter = new ClientCredentialsTokenEndpointFilter(
                frameworkEndpointHandlerMapping().getServletPath(path));
        clientCredentialsTokenEndpointFilter
                .setAuthenticationManager(http.getSharedObject(AuthenticationManager.class));
        OAuth2AuthenticationEntryPoint authenticationEntryPoint = new OAuth2AuthenticationEntryPoint();
        authenticationEntryPoint.setTypeName("Form");
        authenticationEntryPoint.setRealmName(realm);
        clientCredentialsTokenEndpointFilter.setAuthenticationEntryPoint(authenticationEntryPoint);
        clientCredentialsTokenEndpointFilter = postProcess(clientCredentialsTokenEndpointFilter);
        http.addFilterBefore(clientCredentialsTokenEndpointFilter, BasicAuthenticationFilter.class);
        return clientCredentialsTokenEndpointFilter;
    }

    private ClientDetailsService clientDetailsService() {
        return getBuilder().getSharedObject(ClientDetailsService.class);
    }

    private FrameworkEndpointHandlerMapping frameworkEndpointHandlerMapping() {
        return getBuilder().getSharedObject(FrameworkEndpointHandlerMapping.class);
    }

    /**
     * Adds a new custom authentication filter for the TokenEndpoint. Filters will be set upstream of the default
     * BasicAuthenticationFilter.
     *
     * @param filter
     */
    public void addTokenEndpointAuthenticationFilter(Filter filter) {
        this.tokenEndpointAuthenticationFilters.add(filter);
    }

    /**
     * Sets a new list of custom authentication filters for the TokenEndpoint. Filters will be set upstream of the
     * default BasicAuthenticationFilter.
     *
     * @param filters The authentication filters to set.
     */
    public void tokenEndpointAuthenticationFilters(List<Filter> filters) {
        Assert.notNull(filters, "Custom authentication filter list must not be null");
        this.tokenEndpointAuthenticationFilters = new ArrayList<Filter>(filters);
    }
}