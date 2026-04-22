package com.carbonpulse.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CarbonPulse API")
                        .description("CarbonPulse 碳中和社区平台后端接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CarbonPulse Team")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer JWT"))
                .schemaRequirement("Bearer JWT", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("在 Authorization 头填入 JWT Token（不需要加 Bearer 前缀）"));
    }
}
