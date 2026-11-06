package com.nttdata.bank.credit.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Personal-loan quota, card limit, third-party payment bound, and overdue debt.
 */
class CreditRulesTest {

    @Test
    void personalCustomerCanOpenOnlyOneLoan() {
        CreditRules.checkNewPersonalLoan("PERSONAL", 0);
        assertThrows(IllegalArgumentException.class, () -> CreditRules.checkNewPersonalLoan("PERSONAL", 1));
        CreditRules.checkNewPersonalLoan("BUSINESS", 3);
    }

    @Test
    void overdueDebtBlocksANewProduct() {
        assertThrows(IllegalArgumentException.class, () -> CreditRules.checkNoOverdueDebt(true));
        CreditRules.checkNoOverdueDebt(false);
    }

    @Test
    void chargeCannotExceedAvailableCredit() {
        assertThrows(IllegalArgumentException.class,
                () -> CreditRules.checkCharge(new BigDecimal("100.00"), new BigDecimal("100.01")));
        CreditRules.checkCharge(new BigDecimal("100.00"), new BigDecimal("40.00"));
    }

    @Test
    void paymentCannotExceedOutstandingDebt() {
        assertThrows(IllegalArgumentException.class,
                () -> CreditRules.checkPayment(new BigDecimal("20.00"), new BigDecimal("21.00")));
    }

    @Test
    void loanIsOverdueOnlyAfterTheDueDateWithBalance() {
        LocalDate today = LocalDate.of(2026, 11, 5);
        assertTrue(CreditRules.loanOverdue(today.minusDays(1), new BigDecimal("10"), today));
        assertFalse(CreditRules.loanOverdue(today, new BigDecimal("10"), today));
        assertFalse(CreditRules.loanOverdue(today.minusDays(1), BigDecimal.ZERO, today));
    }
}
