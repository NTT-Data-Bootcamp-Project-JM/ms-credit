package com.nttdata.bank.credit.api.dto;

/**
 * @param customerId customer identifier
 * @param overdue whether a credit product is past its due date
 */
public record DebtStatus(String customerId, boolean overdue) {
}