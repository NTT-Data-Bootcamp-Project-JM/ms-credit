package com.nttdata.bank.credit.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Credit card. It does not count as the single personal loan.
 */
@Getter
@Setter
@Document("credit_cards")
public class CreditCardDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String cardNumber;
    private String customerId;
    private String holderType;
    private BigDecimal creditLimit;
    private BigDecimal available;
    private LocalDate paymentDueDate;
    private String status;
}
