package com.nttdata.bank.credit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Published OpenAPI contract of the credit service.
 */
@Configuration
public class OpenApiConfig {

    /**
     * @return contract metadata and the bearer scheme
     */
    @Bean
    public OpenAPI creditOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Credit")
                        .version("1.0.0")
                        .description("Loans, credit cards, payments, and debt status."))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
