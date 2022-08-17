package com.me.autoconfigure.oauth;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

@ComponentScan(value = {
        "com.me.oauth.security",
        "com.me.oauth.api",
        "com.me.oauth.config",
        "com.me.oauth.domain",
        "com.me.oauth.infra",
})
@MapperScan("com.ttdo.oauth.infra.mapper")

@EnableConfigurationProperties
@Configuration

@EnableOAuth2Client

public class OauthAutoConfiguration {


}