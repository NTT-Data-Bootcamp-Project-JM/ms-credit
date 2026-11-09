package com.nttdata.bank.credit.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param customerId owner
 * @param principal borrowed amount
 * @param dueDate repayment date
 */
public record LoanRequest(
        @NotBlank String customerId,
        @NotNull BigDecimal principal,
        @NotNull LocalDate dueDate
) {
}