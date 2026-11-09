package com.nttdata.bank.credit.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Credit card returned by the HTTP API.
 */
public record CreditCardResponse(
        String id,
        String cardNumber,
        String customerId,
        String holderType,
        BigDecimal creditLimit,
        BigDecimal available,
        LocalDate paymentDueDate,
        String status
) {
}