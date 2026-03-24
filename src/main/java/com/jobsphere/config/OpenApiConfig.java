package com.jobsphere.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/*
  OpenAPI / Swagger UI configuration.
  Access at: http://localhost:8080/swagger-ui.html
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "JobSphere API",
        version = "1.0.0",
        description = "Multi-tenant Job Board REST API — Portfolio Project",
        contact = @Contact(name = "JobSphere Developer", email = "dev@jobsphere.com")
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Enter your JWT access token obtained from /api/auth/login"
)
public class OpenApiConfig {
}
