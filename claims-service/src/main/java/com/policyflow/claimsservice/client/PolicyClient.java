package com.policyflow.claimsservice.client;

import com.policyflow.claimsservice.exception.PolicyNotFoundException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PolicyClient {

    private final RestClient restClient;

    public PolicyClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void validatePolicy(String policyNumber) {

        restClient.get()
                .uri(
                        "/api/policies/number/{policyNumber}",
                        policyNumber
                )
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        (request, response) -> {
                            throw new PolicyNotFoundException(
                                    policyNumber
                            );
                        }
                )
                .toBodilessEntity();
    }
}