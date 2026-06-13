package com.berdachuk.medexpertmatch.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Local development security configuration.
 * Requires API key for admin and document endpoints when auth is enabled.
 * Use {@code -Dspring.profiles.active=local,secure-demo} to bypass.
 */
@Configuration
@Profile("local")
@EnableWebSecurity
@ConditionalOnProperty(name = "medexpertmatch.auth.enabled", havingValue = "true", matchIfMissing = true)
public class LocalSecurityConfig {

    @Bean
    public SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/**").authenticated()
                        .requestMatchers("/api/v1/documents/backfill-embeddings").authenticated()
                        .anyRequest().permitAll());
        return http.build();
    }
}
