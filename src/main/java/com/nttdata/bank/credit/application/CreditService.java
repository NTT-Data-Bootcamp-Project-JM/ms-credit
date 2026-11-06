package com.nttdata.bank.credit.application;

import com.nttdata.bank.credit.domain.CreditCardDocument;
import com.nttdata.bank.credit.domain.CreditCardRepository;
import com.nttdata.bank.credit.domain.CreditMovementDocument;
import com.nttdata.bank.credit.domain.CreditMovementRepository;
import com.nttdata.bank.credit.domain.CreditRules;
import com.nttdata.bank.credit.domain.LoanDocument;
import com.nttdata.bank.credit.domain.LoanRepository;
import com.nttdata.bank.credit.infrastructure.CustomerClient;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Loans, credit cards, charges, payments and the overdue-debt query.
 */
@Service
public class CreditService {

    private static final String ACTIVE = "ACTIVE";
    private static final String PERSONAL = "PERSONAL";

    private final LoanRepository loans;
    private final CreditCardRepository cards;
    private final CreditMovementRepository movements;
    private final CustomerClient customers;

    /**
     * @param loans loan collection
     * @param cards card collection
     * @param movements movement collection
     * @param customers customer lookup
     */
    public CreditService(LoanRepository loans, CreditCardRepository cards, CreditMovementRepository movements,
                         CustomerClient customers) {
        this.loans = loans;
        this.cards = cards;
        this.movements = movements;
        this.customers = customers;
    }

    /**
     * Opens a loan. A personal customer is limited to one loan, and overdue debt blocks the purchase.
     *
     * @param customerId owner
     * @param principal borrowed amount
     * @param dueDate repayment date
     * @return created loan
     */
    public Single<LoanDocument> openLoan(String customerId, BigDecimal principal, LocalDate dueDate) {
        return Single.fromCallable(() -> {
            requirePositive(principal);
            CreditRules.checkNoOverdueDebt(isOverdue(customerId));
            String type = customers.customerType(customerId);
            long existing = loans.countByCustomerIdAndCustomerTypeAndStatus(customerId, PERSONAL, ACTIVE);
            if (PERSONAL.equals(type)) {
                CreditRules.checkNewPersonalLoan(type, existing);
            }
            LoanDocument loan = new LoanDocument();
            loan.setLoanNumber(UUID.randomUUID().toString().substring(0, 12));
            loan.setCustomerId(customerId);
            loan.setCustomerType(type);
            loan.setPrincipal(principal);
            loan.setOutstanding(principal);
            loan.setDueDate(dueDate);
            loan.setStatus(ACTIVE);
            return loans.save(loan);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * @return every loan
     */
    public Single<List<LoanDocument>> findLoans() {
        return Single.fromCallable(loans::findAll).subscribeOn(Schedulers.io());
    }

    /**
     * @param id loan identifier
     * @return the loan
     */
    public Single<LoanDocument> findLoan(String id) {
        return Single.fromCallable(() -> loadLoan(id)).subscribeOn(Schedulers.io());
    }

    /**
     * Updates the due date. The outstanding balance is changed only by payments.
     *
     * @param id loan identifier
     * @param dueDate new due date
     * @return updated loan
     */
    public Single<LoanDocument> updateLoan(String id, LocalDate dueDate) {
        return Single.fromCallable(() -> {
            LoanDocument loan = loadLoan(id);
            loan.setDueDate(dueDate);
            return loans.save(loan);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Deletes a loan that has already been paid.
     *
     * @param id loan identifier
     * @return completion
     */
    public Single<Boolean> deleteLoan(String id) {
        return Single.fromCallable(() -> {
            LoanDocument loan = loadLoan(id);
            if (loan.getOutstanding().signum() > 0) {
                throw new IllegalArgumentException("A loan with outstanding debt cannot be deleted");
            }
            loans.delete(loan);
            return true;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Anyone may pay a loan, including a third party.
     *
     * @param id loan identifier
     * @param amount payment
     * @param payerCustomerId customer who pays
     * @return updated loan
     */
    public Single<LoanDocument> payLoan(String id, BigDecimal amount, String payerCustomerId) {
        return Single.fromCallable(() -> applyLoanPayment(id, amount, payerCustomerId, null)).subscribeOn(Schedulers.io());
    }

    /**
     * Opens a credit card. The card is not the single personal loan.
     *
     * @param customerId owner
     * @param limit credit limit
     * @param paymentDueDate date on which unpaid use becomes overdue
     * @return created card
     */
    public Single<CreditCardDocument> openCard(String customerId, BigDecimal limit, LocalDate paymentDueDate) {
        return Single.fromCallable(() -> {
            requirePositive(limit);
            CreditRules.checkNoOverdueDebt(isOverdue(customerId));
            String type = customers.customerType(customerId);
            CreditCardDocument card = new CreditCardDocument();
            card.setCardNumber(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            card.setCustomerId(customerId);
            card.setHolderType(type);
            card.setCreditLimit(limit);
            card.setAvailable(limit);
            card.setPaymentDueDate(paymentDueDate);
            card.setStatus(ACTIVE);
            return cards.save(card);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * @return every card
     */
    public Single<List<CreditCardDocument>> findCards() {
        return Single.fromCallable(cards::findAll).subscribeOn(Schedulers.io());
    }

    /**
     * @param id card identifier
     * @return the card
     */
    public Single<CreditCardDocument> findCard(String id) {
        return Single.fromCallable(() -> loadCard(id)).subscribeOn(Schedulers.io());
    }

    /**
     * @param id card identifier
     * @param paymentDueDate new payment due date
     * @return updated card
     */
    public Single<CreditCardDocument> updateCard(String id, LocalDate paymentDueDate) {
        return Single.fromCallable(() -> {
            CreditCardDocument card = loadCard(id);
            card.setPaymentDueDate(paymentDueDate);
            return cards.save(card);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Deletes a card whose limit is fully available.
     *
     * @param id card identifier
     * @return completion
     */
    public Single<Boolean> deleteCard(String id) {
        return Single.fromCallable(() -> {
            CreditCardDocument card = loadCard(id);
            if (card.getAvailable().compareTo(card.getCreditLimit()) < 0) {
                throw new IllegalArgumentException("A card with used credit cannot be deleted");
            }
            cards.delete(card);
            return true;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Charges a card up to its available limit.
     *
     * @param id card identifier
     * @param amount charge
     * @return updated card
     */
    public Single<CreditCardDocument> charge(String id, BigDecimal amount) {
        return Single.fromCallable(() -> {
            CreditCardDocument card = loadCard(id);
            CreditRules.checkCharge(card.getAvailable(), amount);
            card.setAvailable(card.getAvailable().subtract(amount));
            cards.save(card);
            saveMovement(card.getId(), "CARD", "CHARGE", amount, card.getCustomerId(), null);
            return card;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Pays a card. The payer may be a third party.
     *
     * @param id card identifier
     * @param amount payment
     * @param payerCustomerId customer who pays
     * @return updated card
     */
    public Single<CreditCardDocument> payCard(String id, BigDecimal amount, String payerCustomerId) {
        return Single.fromCallable(() -> applyCardPayment(id, amount, payerCustomerId, null)).subscribeOn(Schedulers.io());
    }

    /**
     * @param customerId customer identifier
     * @return true when a loan or a card is past due
     */
    public Single<Boolean> overdue(String customerId) {
        return Single.fromCallable(() -> isOverdue(customerId)).subscribeOn(Schedulers.io());
    }

    /**
     * @param customerId customer identifier
     * @return true when the customer has an active credit card
     */
    public Single<Boolean> hasActiveCard(String customerId) {
        return Single.fromCallable(() -> cards.existsByCustomerIdAndStatus(customerId, ACTIVE)).subscribeOn(Schedulers.io());
    }

    /**
     * Records a movement and applies it to the loan or card balance.
     * A loan accepts only a payment. A card accepts a charge or a payment.
     *
     * @param productId loan or card identifier
     * @param productKind LOAN or CARD
     * @param type CHARGE or PAYMENT
     * @param amount money amount
     * @param payerCustomerId customer who pays; required for a payment
     * @return created movement
     */
    public Single<CreditMovementDocument> createMovement(String productId, String productKind, String type,
                                                        BigDecimal amount, String payerCustomerId) {
        return Single.fromCallable(() -> postMovement(productId, productKind, type, amount, payerCustomerId))
                .subscribeOn(Schedulers.io());
    }

    /**
     * @param productId optional loan or card identifier
     * @return every movement, or the movements of one product
     */
    public Single<List<CreditMovementDocument>> findMovements(String productId) {
        return Single.fromCallable(() -> {
            if (productId == null || productId.isBlank()) {
                return movements.findAll();
            }
            return movements.findByProductIdOrderByCreatedAtDesc(productId);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * @param id movement identifier
     * @return one movement
     */
    public Single<CreditMovementDocument> findMovement(String id) {
        return Single.fromCallable(() -> loadMovement(id)).subscribeOn(Schedulers.io());
    }

    /**
     * Replaces the recorded payer. Amount, product, and type stay unchanged.
     *
     * @param id movement identifier
     * @param payerCustomerId customer who paid
     * @return updated movement
     */
    public Single<CreditMovementDocument> updateMovement(String id, String payerCustomerId) {
        return Single.fromCallable(() -> {
            CreditMovementDocument movement = loadMovement(id);
            if (payerCustomerId != null) {
                movement.setPayerCustomerId(payerCustomerId);
            }
            return movements.save(movement);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Deletes a movement and reverses its effect on the loan or card balance.
     *
     * @param id movement identifier
     * @return completion
     */
    public Single<Boolean> deleteMovement(String id) {
        return Single.fromCallable(() -> {
            CreditMovementDocument movement = loadMovement(id);
            reverse(movement);
            movements.delete(movement);
            return true;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * @param productKind LOAN or CARD
     * @param from interval start
     * @param to interval end
     * @return movements in that interval
     */
    public Single<List<CreditMovementDocument>> report(String productKind, Instant from, Instant to) {
        return Single.fromCallable(() -> movements.findByProductKindAndCreatedAtBetween(productKind, from, to))
                .subscribeOn(Schedulers.io());
    }

    /**
     * @param cardId card identifier
     * @return at most ten newest movements
     */
    public Single<List<CreditMovementDocument>> lastCardMovements(String cardId) {
        return Single.fromCallable(() -> movements.findByProductIdOrderByCreatedAtDesc(cardId).stream().limit(10).toList())
                .subscribeOn(Schedulers.io());
    }

    /**
     * Applies a Kafka payment once. A repeated operation id does not pay twice.
     *
     * @param operationId idempotency key
     * @param productId loan or card
     * @param productKind LOAN or CARD
     * @param amount payment
     * @param payerCustomerId payer
     */
    public void applyExternalPayment(String operationId, String productId, String productKind, BigDecimal amount, String payerCustomerId) {
        if (movements.findByOperationId(operationId).isPresent()) {
            return;
        }
        if ("LOAN".equals(productKind)) {
            applyLoanPayment(productId, amount, payerCustomerId, operationId);
        } else {
            applyCardPayment(productId, amount, payerCustomerId, operationId);
        }
    }

    private LoanDocument applyLoanPayment(String id, BigDecimal amount, String payerCustomerId, String operationId) {
        LoanDocument loan = loadLoan(id);
        CreditRules.checkPayment(loan.getOutstanding(), amount);
        loan.setOutstanding(loan.getOutstanding().subtract(amount));
        loans.save(loan);
        saveMovement(loan.getId(), "LOAN", "PAYMENT", amount, payerCustomerId, operationId);
        return loan;
    }

    private CreditCardDocument applyCardPayment(String id, BigDecimal amount, String payerCustomerId, String operationId) {
        CreditCardDocument card = loadCard(id);
        BigDecimal used = card.getCreditLimit().subtract(card.getAvailable());
        CreditRules.checkPayment(used, amount);
        card.setAvailable(card.getAvailable().add(amount));
        cards.save(card);
        saveMovement(card.getId(), "CARD", "PAYMENT", amount, payerCustomerId, operationId);
        return card;
    }

    private boolean isOverdue(String customerId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        boolean loanOverdue = loans.findByCustomerId(customerId).stream()
                .anyMatch(loan -> CreditRules.loanOverdue(loan.getDueDate(), loan.getOutstanding(), today));
        boolean cardOverdue = cards.findByCustomerId(customerId).stream()
                .anyMatch(card -> card.getPaymentDueDate().isBefore(today)
                        && card.getAvailable().compareTo(card.getCreditLimit()) < 0);
        return loanOverdue || cardOverdue;
    }

    private CreditMovementDocument postMovement(String productId, String productKind, String type, BigDecimal amount,
                                               String payerCustomerId) {
        if ("LOAN".equals(productKind)) {
            if (!"PAYMENT".equals(type)) {
                throw new IllegalArgumentException("A loan movement must be a payment");
            }
            requirePayer(payerCustomerId);
            LoanDocument loan = loadLoan(productId);
            CreditRules.checkPayment(loan.getOutstanding(), amount);
            loan.setOutstanding(loan.getOutstanding().subtract(amount));
            loans.save(loan);
            return saveMovement(loan.getId(), "LOAN", "PAYMENT", amount, payerCustomerId, null);
        }
        if (!"CARD".equals(productKind)) {
            throw new IllegalArgumentException("Product kind must be LOAN or CARD");
        }
        if ("CHARGE".equals(type)) {
            CreditCardDocument card = loadCard(productId);
            CreditRules.checkCharge(card.getAvailable(), amount);
            card.setAvailable(card.getAvailable().subtract(amount));
            cards.save(card);
            return saveMovement(card.getId(), "CARD", "CHARGE", amount, card.getCustomerId(), null);
        }
        if ("PAYMENT".equals(type)) {
            requirePayer(payerCustomerId);
            CreditCardDocument card = loadCard(productId);
            BigDecimal used = card.getCreditLimit().subtract(card.getAvailable());
            CreditRules.checkPayment(used, amount);
            card.setAvailable(card.getAvailable().add(amount));
            cards.save(card);
            return saveMovement(card.getId(), "CARD", "PAYMENT", amount, payerCustomerId, null);
        }
        throw new IllegalArgumentException("Credit movement type must be CHARGE or PAYMENT");
    }

    private void reverse(CreditMovementDocument movement) {
        if ("LOAN".equals(movement.getProductKind())) {
            if (!"PAYMENT".equals(movement.getType())) {
                throw new IllegalArgumentException("This loan movement cannot be reversed");
            }
            LoanDocument loan = loadLoan(movement.getProductId());
            loan.setOutstanding(loan.getOutstanding().add(movement.getAmount()));
            loans.save(loan);
            return;
        }
        if (!"CARD".equals(movement.getProductKind())) {
            throw new IllegalArgumentException("Product kind must be LOAN or CARD");
        }
        CreditCardDocument card = loadCard(movement.getProductId());
        if ("CHARGE".equals(movement.getType())) {
            BigDecimal restored = card.getAvailable().add(movement.getAmount());
            if (restored.compareTo(card.getCreditLimit()) > 0) {
                throw new IllegalArgumentException("Reversing this charge would exceed the credit limit");
            }
            card.setAvailable(restored);
        } else if ("PAYMENT".equals(movement.getType())) {
            BigDecimal reduced = card.getAvailable().subtract(movement.getAmount());
            if (reduced.signum() < 0) {
                throw new IllegalArgumentException("Reversing this payment would exceed the used credit");
            }
            card.setAvailable(reduced);
        } else {
            throw new IllegalArgumentException("Credit movement type cannot be reversed");
        }
        cards.save(card);
    }

    private CreditMovementDocument saveMovement(String productId, String kind, String type, BigDecimal amount, String payer,
                                               String operationId) {
        CreditMovementDocument movement = new CreditMovementDocument();
        movement.setProductId(productId);
        movement.setProductKind(kind);
        movement.setType(type);
        movement.setAmount(amount);
        movement.setPayerCustomerId(payer);
        movement.setOperationId(operationId);
        movement.setCreatedAt(Instant.now());
        return movements.save(movement);
    }

    private CreditMovementDocument loadMovement(String id) {
        return movements.findById(id).orElseThrow(() -> new IllegalArgumentException("Credit movement was not found"));
    }

    private void requirePayer(String payerCustomerId) {
        if (payerCustomerId == null || payerCustomerId.isBlank()) {
            throw new IllegalArgumentException("A payment must identify the customer who pays");
        }
    }

    private LoanDocument loadLoan(String id) {
        return loans.findById(id).orElseThrow(() -> new IllegalArgumentException("Loan was not found"));
    }

    private CreditCardDocument loadCard(String id) {
        return cards.findById(id).orElseThrow(() -> new IllegalArgumentException("Credit card was not found"));
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("The amount must be greater than zero");
        }
    }
}
