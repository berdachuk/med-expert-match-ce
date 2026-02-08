package com.berdachuk.medexpertmatch.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security config for Docker/deployed run: no login, permit all (auth at gateway in production).
 */
@Configuration
@Profile("docker & !secure-demo")
@EnableWebSecurity
public class DockerSecurityConfig {

    @Bean
    public SecurityFilterChain dockerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
