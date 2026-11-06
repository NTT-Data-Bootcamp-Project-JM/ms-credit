package com.nttdata.bank.credit.api;

import com.nttdata.bank.credit.api.contract.CreditApi;
import com.nttdata.bank.credit.application.CreditService;
import com.nttdata.bank.credit.domain.CreditCardDocument;
import com.nttdata.bank.credit.domain.CreditMovementDocument;
import com.nttdata.bank.credit.domain.LoanDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Implements the credit contract generated from {@code openapi/openapi.yaml}.
 */
@RestController
public class CreditController implements CreditApi {

    private final CreditService credits;

    /**
     * @param credits credit use cases
     */
    public CreditController(CreditService credits) {
        this.credits = credits;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<LoanDocument> openLoan(LoanRequest request) {
        return credits.openLoan(request.customerId(), request.principal(), request.dueDate())
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body))
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<LoanDocument>> loans() {
        return credits.findLoans().map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<LoanDocument> loan(String id) {
        return credits.findLoan(id).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<LoanDocument> updateLoan(String id, DueDateRequest request) {
        return credits.updateLoan(id, request.dueDate()).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Void> deleteLoan(String id) {
        return credits.deleteLoan(id).map(done -> ResponseEntity.noContent().<Void>build()).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<LoanDocument> payLoan(String id, PaymentRequest request) {
        return credits.payLoan(id, request.amount(), request.payerCustomerId()).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CreditCardDocument> openCard(CardRequest request) {
        return credits.openCard(request.customerId(), request.creditLimit(), request.paymentDueDate())
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body))
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<CreditCardDocument>> cards() {
        return credits.findCards().map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CreditCardDocument> card(String id) {
        return credits.findCard(id).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CreditCardDocument> updateCard(String id, DueDateRequest request) {
        return credits.updateCard(id, request.dueDate()).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Void> deleteCard(String id) {
        return credits.deleteCard(id).map(done -> ResponseEntity.noContent().<Void>build()).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CreditCardDocument> charge(String id, AmountRequest request) {
        return credits.charge(id, request.amount()).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CreditCardDocument> payCard(String id, PaymentRequest request) {
        return credits.payCard(id, request.amount(), request.payerCustomerId()).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<DebtStatus> debt(String customerId) {
        return credits.overdue(customerId)
                .map(overdue -> ResponseEntity.ok(new DebtStatus(customerId, overdue)))
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CreditCardFlag> cardFlag(String customerId) {
        return credits.hasActiveCard(customerId)
                .map(exists -> ResponseEntity.ok(new CreditCardFlag(exists)))
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<CreditMovementDocument>> report(String productKind, Instant from, Instant to) {
        return credits.report(productKind, from, to).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<CreditMovementDocument>> last(String id) {
        return credits.lastCardMovements(id).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * @param customerId owner
     * @param principal borrowed amount
     * @param dueDate repayment date
     */
    public record LoanRequest(@NotBlank String customerId, @NotNull BigDecimal principal, @NotNull LocalDate dueDate) {
    }

    /**
     * @param customerId owner
     * @param creditLimit card limit
     * @param paymentDueDate date when unpaid use becomes overdue
     */
    public record CardRequest(@NotBlank String customerId, @NotNull BigDecimal creditLimit,
                              @NotNull LocalDate paymentDueDate) {
    }

    /**
     * @param amount money amount
     * @param payerCustomerId customer who pays; may differ from the owner
     */
    public record PaymentRequest(@NotNull BigDecimal amount, @NotBlank String payerCustomerId) {
    }

    /**
     * @param amount money amount
     */
    public record AmountRequest(@NotNull BigDecimal amount) {
    }

    /**
     * @param dueDate replacement date
     */
    public record DueDateRequest(@NotNull LocalDate dueDate) {
    }

    /**
     * @param customerId customer identifier
     * @param overdue whether a credit product is past its due date
     */
    public record DebtStatus(String customerId, boolean overdue) {
    }

    /**
     * @param exists whether the customer has an active credit card
     */
    public record CreditCardFlag(boolean exists) {
    }
}
