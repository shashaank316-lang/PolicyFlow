
package com.policyflow.policyservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient customerServiceRestClient() {
        return RestClient.builder()
                .baseUrl("http://customer-service:8081")
                .build();
    }
}
