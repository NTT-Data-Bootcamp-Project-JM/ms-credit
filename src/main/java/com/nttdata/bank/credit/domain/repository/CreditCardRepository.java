package com.nttdata.bank.credit.domain.repository;

import com.nttdata.bank.credit.domain.model.CreditCardDocument;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Credit card collection of this service.
 */
public interface CreditCardRepository extends MongoRepository<CreditCardDocument, String> {

    /**
     * @param customerId customer identifier
     * @param status card status
     * @return true when the customer has an active card
     */
    boolean existsByCustomerIdAndStatus(String customerId, String status);

    /**
     * @param customerId customer identifier
     * @return cards of that customer
     */
    List<CreditCardDocument> findByCustomerId(String customerId);
}
