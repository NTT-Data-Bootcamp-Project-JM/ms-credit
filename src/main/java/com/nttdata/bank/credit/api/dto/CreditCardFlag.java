package com.nttdata.bank.credit.api.dto;

/**
 * @param exists whether the customer has an active credit card
 */
public record CreditCardFlag(boolean exists) {
}