package com.nttdata.bank.credit.api.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * @param amount money amount
 */
public record AmountRequest(@NotNull BigDecimal amount) {
}