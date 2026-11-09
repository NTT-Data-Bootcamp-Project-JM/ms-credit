package com.nttdata.bank.credit.api.controller;

import com.nttdata.bank.credit.api.contract.CreditApi;
import com.nttdata.bank.credit.api.dto.AmountRequest;
import com.nttdata.bank.credit.api.dto.CardRequest;
import com.nttdata.bank.credit.api.dto.CreditCardFlag;
import com.nttdata.bank.credit.api.dto.CreditCardResponse;
import com.nttdata.bank.credit.api.dto.CreditMovementRequest;
import com.nttdata.bank.credit.api.dto.CreditMovementResponse;
import com.nttdata.bank.credit.api.dto.CreditMovementUpdate;
import com.nttdata.bank.credit.api.dto.DebtStatus;
import com.nttdata.bank.credit.api.dto.DueDateRequest;
import com.nttdata.bank.credit.api.dto.LoanRequest;
import com.nttdata.bank.credit.api.dto.LoanResponse;
import com.nttdata.bank.credit.api.dto.PaymentRequest;
import com.nttdata.bank.credit.service.CreditService;
import com.nttdata.bank.credit.mapper.CreditMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Implements the credit contract generated from {@code openapi/openapi.yaml}.
 */
@RestController
public class CreditController implements CreditApi {

    private final CreditService credits;
    private final CreditMapper mapper;

    /**
     * @param credits credit use cases
     * @param mapper domain-to-DTO mapper
     */
    public CreditController(CreditService credits, CreditMapper mapper) {
        this.credits = credits;
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<LoanResponse> openLoan(LoanRequest request) {
        return credits.openLoan(request.customerId(), request.principal(), request.dueDate())
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body))
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<LoanResponse>> loans() {
        return credits.findLoans().map(mapper::toLoanResponseList).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<LoanResponse> loan(String id) {
        return credits.findLoan(id).map(mapper::toResponse).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<LoanResponse> updateLoan(String id, DueDateRequest request) {
        return credits.updateLoan(id, request.dueDate()).map(mapper::toResponse).map(ResponseEntity::ok).blockingGet();
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
    public ResponseEntity<LoanResponse> payLoan(String id, PaymentRequest request) {
        return credits.payLoan(id, request.amount(), request.payerCustomerId())
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CreditCardResponse> openCard(CardRequest request) {
        return credits.openCard(request.customerId(), request.creditLimit(), request.paymentDueDate())
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body))
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<CreditCardResponse>> cards() {
        return credits.findCards().map(mapper::toCreditCardResponseList).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CreditCardResponse> card(String id) {
        return credits.findCard(id).map(mapper::toResponse).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CreditCardResponse> updateCard(String id, DueDateRequest request) {
        return credits.updateCard(id, request.dueDate()).map(mapper::toResponse).map(ResponseEntity::ok).blockingGet();
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
    public ResponseEntity<CreditCardResponse> charge(String id, AmountRequest request) {
        return credits.charge(id, request.amount()).map(mapper::toResponse).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CreditCardResponse> payCard(String id, PaymentRequest request) {
        return credits.payCard(id, request.amount(), request.payerCustomerId())
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .blockingGet();
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
    public ResponseEntity<CreditMovementResponse> createMovement(CreditMovementRequest request) {
        return credits.createMovement(request.productId(), request.productKind(), request.type(), request.amount(),
                        request.payerCustomerId())
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body))
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<CreditMovementResponse>> listMovements(String productId) {
        return credits.findMovements(productId).map(mapper::toCreditMovementResponseList).map(ResponseEntity::ok)
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CreditMovementResponse> getMovement(String id) {
        return credits.findMovement(id).map(mapper::toResponse).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CreditMovementResponse> updateMovement(String id, CreditMovementUpdate request) {
        return credits.updateMovement(id, request.payerCustomerId())
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Void> deleteMovement(String id) {
        return credits.deleteMovement(id).map(done -> ResponseEntity.noContent().<Void>build()).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<CreditMovementResponse>> report(String productKind, Instant from, Instant to) {
        return credits.report(productKind, from, to).map(mapper::toCreditMovementResponseList).map(ResponseEntity::ok)
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<CreditMovementResponse>> last(String id) {
        return credits.lastCardMovements(id).map(mapper::toCreditMovementResponseList).map(ResponseEntity::ok)
                .blockingGet();
    }
}
