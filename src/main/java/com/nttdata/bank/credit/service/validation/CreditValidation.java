package com.nttdata.bank.credit.service.validation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Credit rules that stay free of Spring so they can be unit tested.
 * A credit card is not the single personal loan.
 */
public final class CreditValidation {

    private CreditValidation() {
    }

    /**
     * A person may hold only one personal loan. A company may hold several. A card never consumes this quota.
     *
     * @param customerType PERSONAL or BUSINESS
     * @param existingPersonalLoans personal loans already open
     */
    public static void checkNewPersonalLoan(String customerType, long existingPersonalLoans) {
        if ("PERSONAL".equals(customerType) && existingPersonalLoans >= 1) {
            throw new IllegalArgumentException("A personal customer can have only one loan");
        }
    }

    /**
     * A customer with overdue credit cannot acquire another product.
     *
     * @param overdue true when any credit product is past due
     */
    public static void checkNoOverdueDebt(boolean overdue) {
        if (overdue) {
            throw new IllegalArgumentException("The customer has overdue debt and cannot acquire a product");
        }
    }

    /**
     * A card charge cannot exceed the available limit.
     *
     * @param available credit still available
     * @param amount charge amount
     */
    public static void checkCharge(BigDecimal available, BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("The amount must be greater than zero");
        }
        if (available.compareTo(amount) < 0) {
            throw new IllegalArgumentException("The charge exceeds the available credit");
        }
    }

    /**
     * A payment cannot be greater than the outstanding debt.
     *
     * @param outstanding amount still owed
     * @param amount payment amount
     */
    public static void checkPayment(BigDecimal outstanding, BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("The amount must be greater than zero");
        }
        if (outstanding.compareTo(amount) < 0) {
            throw new IllegalArgumentException("The payment is greater than the outstanding debt");
        }
    }

    /**
     * A loan is overdue when its due date has passed and money is still owed.
     *
     * @param dueDate agreed due date
     * @param outstanding remaining debt
     * @param today current date
     * @return true when the loan is overdue
     */
    public static boolean loanOverdue(LocalDate dueDate, BigDecimal outstanding, LocalDate today) {
        return dueDate.isBefore(today) && outstanding.signum() > 0;
    }
}
