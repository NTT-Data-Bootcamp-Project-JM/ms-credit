package com.nttdata.bank.credit.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param customerId owner
 * @param creditLimit card limit
 * @param paymentDueDate date when unpaid use becomes overdue
 */
public record CardRequest(
        @NotBlank String customerId,
        @NotNull BigDecimal creditLimit,
        @NotNull LocalDate paymentDueDate
) {
}