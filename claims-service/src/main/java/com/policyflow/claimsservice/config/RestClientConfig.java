package com.policyflow.claimsservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.slf4j.MDC;

@Configuration
public class RestClientConfig {

    @Value("${policy.service.base-url:http://localhost:8080}")
    private String policyServiceBaseUrl;

    @Value("${app.security.username}")
    private String securityUsername;

    @Value("${app.security.password}")
    private String securityPassword;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(policyServiceBaseUrl)
                .defaultHeaders(headers ->
                        headers.setBasicAuth(securityUsername, securityPassword))
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = MDC.get("correlationId");

                    if (correlationId != null) {
                        request.getHeaders().set("X-Correlation-ID", correlationId);
                    }

                    return execution.execute(request, body);
                })
                .build();
    }
}
