package com.ismael.CoopPay.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI coopPayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CoopPay API")
                        .description("Núcleo de movimentações financeiras entre associados")
                        .version("v1"));
    }
}
