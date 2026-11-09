package com.nttdata.bank.credit.service.validation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Personal-loan quota, card limit, third-party payment bound, and overdue debt.
 */
class CreditValidationTest {

    @Test
    void personalCustomerCanOpenOnlyOneLoan() {
        CreditValidation.checkNewPersonalLoan("PERSONAL", 0);
        assertThrows(IllegalArgumentException.class, () -> CreditValidation.checkNewPersonalLoan("PERSONAL", 1));
        CreditValidation.checkNewPersonalLoan("BUSINESS", 3);
    }

    @Test
    void overdueDebtBlocksANewProduct() {
        assertThrows(IllegalArgumentException.class, () -> CreditValidation.checkNoOverdueDebt(true));
        CreditValidation.checkNoOverdueDebt(false);
    }

    @Test
    void chargeCannotExceedAvailableCredit() {
        assertThrows(IllegalArgumentException.class,
                () -> CreditValidation.checkCharge(new BigDecimal("100.00"), new BigDecimal("100.01")));
        CreditValidation.checkCharge(new BigDecimal("100.00"), new BigDecimal("40.00"));
    }

    @Test
    void paymentCannotExceedOutstandingDebt() {
        assertThrows(IllegalArgumentException.class,
                () -> CreditValidation.checkPayment(new BigDecimal("20.00"), new BigDecimal("21.00")));
    }

    @Test
    void loanIsOverdueOnlyAfterTheDueDateWithBalance() {
        LocalDate today = LocalDate.of(2026, 11, 5);
        assertTrue(CreditValidation.loanOverdue(today.minusDays(1), new BigDecimal("10"), today));
        assertFalse(CreditValidation.loanOverdue(today, new BigDecimal("10"), today));
        assertFalse(CreditValidation.loanOverdue(today.minusDays(1), BigDecimal.ZERO, today));
    }
}
