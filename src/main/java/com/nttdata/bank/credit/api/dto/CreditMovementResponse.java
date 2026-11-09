package com.nttdata.bank.credit.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Credit movement returned by the HTTP API.
 */
public record CreditMovementResponse(
        String id,
        String productId,
        String productKind,
        String type,
        BigDecimal amount,
        String payerCustomerId,
        String operationId,
        Instant createdAt
) {
}