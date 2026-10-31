package com.brightloop.subscription.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI brightloopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Brightloop Software - Subscription Management System API")
                        .description("Comprehensive REST API platform for subscription plans, quotes, automated recurring billing, taxes, discounts, and payments.")
                        .version("1.0.0")
                        .contact(new Contact().name("Brightloop Engineering").email("support@brightloop.com"))
                        .license(new License().name("Proprietary").url("https://brightloop.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
