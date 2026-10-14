package com.policyflow.customerservice.service;

import com.policyflow.customerservice.entity.Customer;
import com.policyflow.customerservice.exception.CustomerNotFoundException;
import com.policyflow.customerservice.exception.DuplicateCustomerException;
import com.policyflow.customerservice.repository.CustomerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    // NEW METHOD
    public Customer getCustomerByNumber(String customerNumber) {

        return customerRepository
                .findByCustomerNumber(customerNumber)
                .orElseThrow(() ->
                        new CustomerNotFoundException(customerNumber));
    }

    public Customer createCustomer(Customer customer) {

        if (customerRepository.existsByCustomerNumber(
                customer.getCustomerNumber())) {

            throw new DuplicateCustomerException(
                    "Customer number already exists: "
                            + customer.getCustomerNumber()
            );
        }

        if (customerRepository.existsByEmail(
                customer.getEmail())) {

            throw new DuplicateCustomerException(
                    "Email already exists: "
                            + customer.getEmail()
            );
        }

        customer.setStatus("ACTIVE");

        try {
            return customerRepository.save(customer);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCustomerException(
                    "Customer already exists with the provided information."
            );
        }
    }
}