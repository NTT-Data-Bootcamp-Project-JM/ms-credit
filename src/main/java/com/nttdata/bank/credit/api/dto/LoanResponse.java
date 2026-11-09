package com.nttdata.bank.credit.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Loan returned by the HTTP API.
 */
public record LoanResponse(
        String id,
        String loanNumber,
        String customerId,
        String customerType,
        BigDecimal principal,
        BigDecimal outstanding,
        LocalDate dueDate,
        String status
) {
}