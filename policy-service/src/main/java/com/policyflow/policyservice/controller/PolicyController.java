package com.policyflow.policyservice.controller;

import com.policyflow.policyservice.entity.Policy;
import com.policyflow.policyservice.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public List<Policy> getAllPolicies() {
        return policyService.getAllPolicies();
    }

    @GetMapping("/{id}")
    public Policy getPolicyById(@PathVariable Long id) {
        return policyService.getPolicyById(id);
    }
    @GetMapping("/number/{policyNumber}")
    public Policy getPolicyByNumber(
            @PathVariable String policyNumber) {

        return policyService.getPolicyByNumber(policyNumber);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Policy createPolicy(@Valid @RequestBody Policy policy) {
        return policyService.createPolicy(policy);
    }
}
