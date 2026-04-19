package com.samoy.chuanbillserver.config;

import java.io.IOException;
import java.net.HttpURLConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * <p>
 * RestTemplate请求配置
 * </p>
 *
 * @author samoy
 * @since 2026/4/19
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // 配置超时时间，避免CDN预热请求长时间阻塞
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setConnectTimeout(5000); // 连接超时 5 秒
                connection.setReadTimeout(10000); // 读取超时 10 秒
            }
        };
        restTemplate.setRequestFactory(factory);
        return restTemplate;
    }
}
