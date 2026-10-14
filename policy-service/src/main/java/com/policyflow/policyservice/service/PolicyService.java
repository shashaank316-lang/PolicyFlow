package com.policyflow.policyservice.service;

import com.policyflow.policyservice.client.CustomerClient;
import com.policyflow.policyservice.entity.Policy;
import com.policyflow.policyservice.exception.DuplicatePolicyException;
import com.policyflow.policyservice.exception.PolicyNotFoundException;
import com.policyflow.policyservice.repository.PolicyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final CustomerClient customerClient;

    public PolicyService(
            PolicyRepository policyRepository,
            CustomerClient customerClient) {

        this.policyRepository = policyRepository;
        this.customerClient = customerClient;
    }

    public List<Policy> getAllPolicies() {
        return policyRepository.findAll();
    }

    public Policy getPolicyById(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException(id));
    }

    public Policy getPolicyByNumber(String policyNumber) {
        return policyRepository
                .findByPolicyNumber(policyNumber)
                .orElseThrow(() ->
                        new PolicyNotFoundException(policyNumber));
    }
    public Policy createPolicy(Policy policy) {

        // Validate customer through Customer Service
        customerClient.validateCustomer(policy.getCustomerId());

        policy.setStatus("ACTIVE");

        try {
            return policyRepository.save(policy);

        } catch (DataIntegrityViolationException exception) {

            throw new DuplicatePolicyException(
                    policy.getPolicyNumber()
            );
        }
    }
}