package com.policyflow.claimsservice.integration;

import com.policyflow.claimsservice.client.PolicyClient;
import com.policyflow.claimsservice.entity.Claim;
import com.policyflow.claimsservice.repository.ClaimRepository;
import com.policyflow.claimsservice.repository.OutboxEventRepository;
import com.policyflow.claimsservice.service.ClaimService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "app.security.username=test-user",
    "app.security.password=test-password"
})
class ClaimTransactionIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("claims_transaction_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ClaimService claimService;

    @Autowired
    private ClaimRepository claimRepository;

    @MockitoSpyBean
    private PolicyClient policyClient;

    @MockitoSpyBean
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldRollbackClaimWhenOutboxSaveFails() {
        String claimNumber = "TX-ROLLBACK-" + UUID.randomUUID();

        Claim claim = new Claim();
        claim.setClaimNumber(claimNumber);
        claim.setPolicyNumber("POLICY-100");
        claim.setCustomerId("CUSTOMER-100");
        claim.setClaimType("AUTO");
        claim.setClaimAmount(new BigDecimal("250.00"));

        doNothing().when(policyClient).validatePolicy("POLICY-100");

        doThrow(new RuntimeException("Simulated outbox save failure"))
            .when(outboxEventRepository)
            .save(any());

        assertThrows(
            RuntimeException.class,
            () -> claimService.createClaim(claim)
        );

        assertFalse(
            claimRepository.findAll().stream()
                .anyMatch(saved -> claimNumber.equals(saved.getClaimNumber())),
            "The claim must roll back when the outbox save fails"
        );

        assertFalse(
            outboxEventRepository.findAll().stream()
                .anyMatch(event -> claimNumber.equals(event.getAggregateId())),
            "The outbox event must not be committed"
        );
    }
}
