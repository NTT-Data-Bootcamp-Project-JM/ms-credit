package com.nttdata.bank.credit.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Personal or business loan. Personal customers may have only one.
 */
@Getter
@Setter
@Document("loans")
public class LoanDocument {

    @Id
    private String id;
    private String loanNumber;
    private String customerId;
    private String customerType;
    private BigDecimal principal;
    private BigDecimal outstanding;
    private LocalDate dueDate;
    private String status;
}
