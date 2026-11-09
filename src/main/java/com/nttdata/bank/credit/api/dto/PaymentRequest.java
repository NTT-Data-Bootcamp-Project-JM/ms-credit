package com.nttdata.bank.credit.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * @param amount money amount
 * @param payerCustomerId customer who pays; may differ from the owner
 */
public record PaymentRequest(@NotNull BigDecimal amount, @NotBlank String payerCustomerId) {
}