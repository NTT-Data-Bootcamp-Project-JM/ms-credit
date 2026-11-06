package com.nttdata.bank.credit.api;

import com.nttdata.bank.credit.application.CreditService;
import com.nttdata.bank.credit.domain.CreditCardDocument;
import com.nttdata.bank.credit.domain.CreditMovementDocument;
import com.nttdata.bank.credit.domain.LoanDocument;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reactive HTTP API for loans, credit cards and credit reports.
 */
@RestController
public class CreditController {

    private final CreditService credits;

    /**
     * @param credits credit use cases
     */
    public CreditController(CreditService credits) {
        this.credits = credits;
    }

    /**
     * @param request loan data
     * @return created loan
     */
    @PostMapping("/api/v1/loans")
    public Single<ResponseEntity<LoanDocument>> openLoan(@Valid @RequestBody LoanRequest request) {
        return credits.openLoan(request.customerId(), request.principal(), request.dueDate())
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
    }

    /**
     * @return all loans
     */
    @GetMapping("/api/v1/loans")
    public Single<ResponseEntity<List<LoanDocument>>> loans() {
        return credits.findLoans().map(ResponseEntity::ok);
    }

    /**
     * @param id loan identifier
     * @return one loan
     */
    @GetMapping("/api/v1/loans/{id}")
    public Single<ResponseEntity<LoanDocument>> loan(@PathVariable String id) {
        return credits.findLoan(id).map(ResponseEntity::ok);
    }

    /**
     * @param id loan identifier
     * @param request new due date
     * @return updated loan
     */
    @PutMapping("/api/v1/loans/{id}")
    public Single<ResponseEntity<LoanDocument>> updateLoan(@PathVariable String id, @Valid @RequestBody DueDateRequest request) {
        return credits.updateLoan(id, request.dueDate()).map(ResponseEntity::ok);
    }

    /**
     * @param id loan identifier
     * @return empty response
     */
    @DeleteMapping("/api/v1/loans/{id}")
    public Single<ResponseEntity<Void>> deleteLoan(@PathVariable String id) {
        return credits.deleteLoan(id).map(done -> ResponseEntity.noContent().build());
    }

    /**
     * Pays a loan. The payer may be different from the owner.
     *
     * @param id loan identifier
     * @param request payment
     * @return updated loan
     */
    @PostMapping("/api/v1/loans/{id}/payments")
    public Single<ResponseEntity<LoanDocument>> payLoan(@PathVariable String id, @Valid @RequestBody PaymentRequest request) {
        return credits.payLoan(id, request.amount(), request.payerCustomerId()).map(ResponseEntity::ok);
    }

    /**
     * @param request card data
     * @return created card
     */
    @PostMapping("/api/v1/credit-cards")
    public Single<ResponseEntity<CreditCardDocument>> openCard(@Valid @RequestBody CardRequest request) {
        return credits.openCard(request.customerId(), request.creditLimit(), request.paymentDueDate())
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
    }

    /**
     * @return all cards
     */
    @GetMapping("/api/v1/credit-cards")
    public Single<ResponseEntity<List<CreditCardDocument>>> cards() {
        return credits.findCards().map(ResponseEntity::ok);
    }

    /**
     * @param id card identifier
     * @return one card
     */
    @GetMapping("/api/v1/credit-cards/{id}")
    public Single<ResponseEntity<CreditCardDocument>> card(@PathVariable String id) {
        return credits.findCard(id).map(ResponseEntity::ok);
    }

    /**
     * @param id card identifier
     * @param request new due date
     * @return updated card
     */
    @PutMapping("/api/v1/credit-cards/{id}")
    public Single<ResponseEntity<CreditCardDocument>> updateCard(@PathVariable String id, @Valid @RequestBody DueDateRequest request) {
        return credits.updateCard(id, request.dueDate()).map(ResponseEntity::ok);
    }

    /**
     * @param id card identifier
     * @return empty response
     */
    @DeleteMapping("/api/v1/credit-cards/{id}")
    public Single<ResponseEntity<Void>> deleteCard(@PathVariable String id) {
        return credits.deleteCard(id).map(done -> ResponseEntity.noContent().build());
    }

    /**
     * @param id card identifier
     * @param request charge
     * @return updated card
     */
    @PostMapping("/api/v1/credit-cards/{id}/charges")
    public Single<ResponseEntity<CreditCardDocument>> charge(@PathVariable String id, @Valid @RequestBody AmountRequest request) {
        return credits.charge(id, request.amount()).map(ResponseEntity::ok);
    }

    /**
     * @param id card identifier
     * @param request payment, possibly from a third party
     * @return updated card
     */
    @PostMapping("/api/v1/credit-cards/{id}/payments")
    public Single<ResponseEntity<CreditCardDocument>> payCard(@PathVariable String id, @Valid @RequestBody PaymentRequest request) {
        return credits.payCard(id, request.amount(), request.payerCustomerId()).map(ResponseEntity::ok);
    }

    /**
     * @param customerId customer identifier
     * @return whether that customer has overdue credit
     */
    @GetMapping("/api/v1/debt-status/{customerId}")
    public Single<ResponseEntity<Map<String, Object>>> debt(@PathVariable String customerId) {
        return credits.overdue(customerId).map(overdue -> ResponseEntity.ok(Map.of("customerId", customerId, "overdue", overdue)));
    }

    /**
     * Used by the account service before opening a VIP or PYME account.
     *
     * @param customerId customer identifier
     * @return whether an active credit card exists
     */
    @GetMapping("/api/v1/debt-status/{customerId}/credit-card")
    public Single<ResponseEntity<Map<String, Object>>> cardFlag(@PathVariable String customerId) {
        return credits.hasActiveCard(customerId).map(exists -> ResponseEntity.ok(Map.of("exists", exists)));
    }

    /**
     * @param productKind LOAN or CARD
     * @param from interval start
     * @param to interval end
     * @return movements in the interval
     */
    @GetMapping("/api/v1/credit-reports")
    public Single<ResponseEntity<List<CreditMovementDocument>>> report(
            @RequestParam String productKind,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return credits.report(productKind, from, to).map(ResponseEntity::ok);
    }

    /**
     * @param id card identifier
     * @return the ten newest card movements
     */
    @GetMapping("/api/v1/credit-reports/credit-cards/{id}/last")
    public Single<ResponseEntity<List<CreditMovementDocument>>> last(@PathVariable String id) {
        return credits.lastCardMovements(id).map(ResponseEntity::ok);
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
    public record CardRequest(@NotBlank String customerId, @NotNull BigDecimal creditLimit, @NotNull LocalDate paymentDueDate) {
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
}
