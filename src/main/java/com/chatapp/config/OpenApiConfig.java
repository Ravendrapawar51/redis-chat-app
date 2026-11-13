package com.chatapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI 3.0 / Swagger UI documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Redis Chat Application API")
                        .version("1.0.0")
                        .description("REST API documentation for Redis Chat Application utilizing Redis Hash, Set, List, Pub/Sub, and real-time WebSockets.")
                        .contact(new Contact()
                                .name("Ravendra Kurmi")
                                .url("https://github.com/Ravendrapawar51/redis-chat-app"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")));
    }
}
