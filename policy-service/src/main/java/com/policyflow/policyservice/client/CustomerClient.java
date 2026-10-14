package com.policyflow.policyservice.client;

import com.policyflow.policyservice.exception.CustomerNotFoundException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CustomerClient {

    private final RestClient restClient;

    public CustomerClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void validateCustomer(String customerNumber) {

        restClient.get()
                .uri(
                        "/api/customers/number/{customerNumber}",
                        customerNumber
                )
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        (request, response) -> {
                            throw new CustomerNotFoundException(
                                    customerNumber
                            );
                        }
                )
                .toBodilessEntity();
    }
}