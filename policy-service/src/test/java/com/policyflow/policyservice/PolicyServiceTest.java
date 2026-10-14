
package com.policyflow.policyservice;

import com.policyflow.policyservice.client.CustomerClient;
import com.policyflow.policyservice.entity.Policy;
import com.policyflow.policyservice.exception.PolicyNotFoundException;
import com.policyflow.policyservice.repository.PolicyRepository;
import com.policyflow.policyservice.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private CustomerClient customerClient;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(
                policyRepository,
                customerClient
        );
    }

    @Test
    void getPolicyByNumberReturnsMatchingPolicy() {
        Policy policy = createPolicy("POL-40003");

        when(policyRepository.findByPolicyNumber("POL-40003"))
                .thenReturn(Optional.of(policy));

        Policy result = policyService.getPolicyByNumber("POL-40003");

        assertSame(policy, result);
        assertEquals("POL-40003", result.getPolicyNumber());
        verify(policyRepository).findByPolicyNumber("POL-40003");
        verifyNoInteractions(customerClient);
    }

    @Test
    void getPolicyByNumberThrowsWhenPolicyDoesNotExist() {
        when(policyRepository.findByPolicyNumber("POL-99999"))
                .thenReturn(Optional.empty());

        assertThrows(
                PolicyNotFoundException.class,
                () -> policyService.getPolicyByNumber("POL-99999")
        );

        verify(policyRepository).findByPolicyNumber("POL-99999");
        verifyNoInteractions(customerClient);
    }

    @Test
    void getPolicyByIdReturnsMatchingPolicy() {
        Policy policy = createPolicy("POL-40003");

        when(policyRepository.findById(1L))
                .thenReturn(Optional.of(policy));

        Policy result = policyService.getPolicyById(1L);

        assertSame(policy, result);
        verify(policyRepository).findById(1L);
        verifyNoInteractions(customerClient);
    }

    private Policy createPolicy(String policyNumber) {
        Policy policy = new Policy();
        policy.setPolicyNumber(policyNumber);
        policy.setCustomerId("CUS-10001");
        policy.setPolicyType("AUTO");
        policy.setPremium(new BigDecimal("125.00"));
        policy.setStatus("ACTIVE");
        return policy;
    }
}
