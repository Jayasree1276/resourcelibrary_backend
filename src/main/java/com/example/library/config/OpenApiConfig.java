package com.example.library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI resourceLibraryOpenAPI(@Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("Resource Library API")
                        .version("v1")
                        .description("API documentation for the Resource Library backend.")
                        .contact(new Contact()
                                .name("Resource Library")
                                .url(frontendUrl)));
    }
}
