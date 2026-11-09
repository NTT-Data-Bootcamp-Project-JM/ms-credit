package com.nttdata.bank.credit.domain.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Charge or payment of a loan or a credit card.
 */
@Getter
@Setter
@Document("credit_movements")
public class CreditMovementDocument {

    @Id
    private String id;
    private String productId;
    private String productKind;
    private String type;
    private BigDecimal amount;
    private String payerCustomerId;
    private String operationId;
    private Instant createdAt;
}
