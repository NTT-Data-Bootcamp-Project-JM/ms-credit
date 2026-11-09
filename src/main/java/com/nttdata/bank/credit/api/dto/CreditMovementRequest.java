package com.nttdata.bank.credit.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * @param productId loan or card identifier
 * @param productKind LOAN or CARD
 * @param type CHARGE or PAYMENT
 * @param amount money amount
 * @param payerCustomerId customer who pays; required for a payment
 */
public record CreditMovementRequest(
        @NotBlank String productId,
        @NotBlank String productKind,
        @NotBlank String type,
        @NotNull BigDecimal amount,
        String payerCustomerId
) {
}