package com.samoy.chuanbillserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置类
 * 用于生成 Swagger UI 和 API 文档
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("小川记账接口文档")
                        .version("v1.0.0")
                        .description("小川记账接口文档")
                        .contact(new Contact().name("Samoy")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("本地环境"),
                        new Server().url("https://bill.samoy.site/api").description("生产环境")));
    }
}
