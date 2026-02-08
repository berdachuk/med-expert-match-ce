package com.berdachuk.medexpertmatch.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for API-first development.
 * This configuration ensures the OpenAPI specification drives the API implementation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MedExpertMatch API")
                        .version("1.0.0")
                        .description("""
                                AI-powered medical expert recommendation system API.
                                
                                This API provides endpoints for:
                                - Medical agent operations (matching, analysis, routing)
                                - Synthetic data generation
                                - Medical case and doctor management
                                
                                **Medical Disclaimer:**
                                This API is for research and educational purposes only. It is NOT certified for clinical use 
                                and should NOT be used for diagnostic decisions without human-in-the-loop verification.
                                """))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development server"),
                        new Server().url("https://api.medexpertmatch.com").description("Production server")
                ));
    }
}
