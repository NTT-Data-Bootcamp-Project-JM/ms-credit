package com.nttdata.bank.credit.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Charges and payments.
 */
public interface CreditMovementRepository extends MongoRepository<CreditMovementDocument, String> {

    /**
     * @param productId loan or card identifier
     * @return movements of that product, newest last
     */
    List<CreditMovementDocument> findByProductIdOrderByCreatedAtDesc(String productId);

    /**
     * @param productKind LOAN or CARD
     * @param from inclusive start
     * @param to exclusive end
     * @return movements in the interval
     */
    List<CreditMovementDocument> findByProductKindAndCreatedAtBetween(String productKind, Instant from, Instant to);

    /**
     * @param operationId idempotency key sent by Kafka
     * @return a movement already applied for that operation
     */
    Optional<CreditMovementDocument> findByOperationId(String operationId);
}
