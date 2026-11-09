package com.nttdata.bank.credit.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * @param dueDate replacement date
 */
public record DueDateRequest(@NotNull LocalDate dueDate) {
}