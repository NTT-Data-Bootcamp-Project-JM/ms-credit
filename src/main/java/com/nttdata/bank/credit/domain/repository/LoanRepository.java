package com.nttdata.bank.credit.domain.repository;

import com.nttdata.bank.credit.domain.model.LoanDocument;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Loan collection of this service.
 */
public interface LoanRepository extends MongoRepository<LoanDocument, String> {

    /**
     * @param customerId customer identifier
     * @param customerType PERSONAL or BUSINESS
     * @param status product status
     * @return matching loans
     */
    long countByCustomerIdAndCustomerTypeAndStatus(String customerId, String customerType, String status);

    /**
     * @param customerId customer identifier
     * @return loans of that customer
     */
    List<LoanDocument> findByCustomerId(String customerId);
}
