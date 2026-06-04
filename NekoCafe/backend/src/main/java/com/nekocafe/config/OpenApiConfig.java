package com.nekocafe.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nekocafeOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("NekoCafé API")
                .version("0.1.0")
                .description("NekoCafé 智慧餐饮预约平台接口文档"));
    }
}
