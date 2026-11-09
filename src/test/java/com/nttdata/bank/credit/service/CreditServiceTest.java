package com.nttdata.bank.credit.service;

import com.nttdata.bank.credit.domain.model.CreditCardDocument;
import com.nttdata.bank.credit.domain.repository.CreditCardRepository;
import com.nttdata.bank.credit.domain.model.CreditMovementDocument;
import com.nttdata.bank.credit.domain.repository.CreditMovementRepository;
import com.nttdata.bank.credit.domain.model.LoanDocument;
import com.nttdata.bank.credit.domain.repository.LoanRepository;
import com.nttdata.bank.credit.infrastructure.webclient.CustomerWebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Credit use cases with mocked repositories and a mocked customer lookup.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreditServiceTest {

    @Mock
    private LoanRepository loans;

    @Mock
    private CreditCardRepository cards;

    @Mock
    private CreditMovementRepository movements;

    @Mock
    private CustomerWebClient customers;

    private final Map<String, LoanDocument> loanStore = new LinkedHashMap<>();
    private final Map<String, CreditCardDocument> cardStore = new LinkedHashMap<>();
    private final Map<String, CreditMovementDocument> movementStore = new LinkedHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();
    private CreditService service;

    @BeforeEach
    void setUp() {
        loanStore.clear();
        cardStore.clear();
        movementStore.clear();
        service = new CreditService(loans, cards, movements, customers);
        when(customers.customerType(any())).thenReturn("BUSINESS");
        when(loans.countByCustomerIdAndCustomerTypeAndStatus(any(), any(), any())).thenReturn(0L);
        when(loans.findByCustomerId(any())).thenReturn(List.of());
        when(cards.findByCustomerId(any())).thenReturn(List.of());
        when(cards.existsByCustomerIdAndStatus(any(), any())).thenReturn(true);

        when(loans.save(any())).thenAnswer(invocation -> keep(loanStore, invocation.getArgument(0), "loan-"));
        when(loans.findById(any())).thenAnswer(invocation -> Optional.ofNullable(loanStore.get(invocation.getArgument(0))));
        when(loans.findAll()).thenAnswer(invocation -> new ArrayList<>(loanStore.values()));
        doAnswer(invocation -> {
            loanStore.remove(((LoanDocument) invocation.getArgument(0)).getId());
            return null;
        }).when(loans).delete(any());

        when(cards.save(any())).thenAnswer(invocation -> keep(cardStore, invocation.getArgument(0), "card-"));
        when(cards.findById(any())).thenAnswer(invocation -> Optional.ofNullable(cardStore.get(invocation.getArgument(0))));
        when(cards.findAll()).thenAnswer(invocation -> new ArrayList<>(cardStore.values()));
        doAnswer(invocation -> {
            cardStore.remove(((CreditCardDocument) invocation.getArgument(0)).getId());
            return null;
        }).when(cards).delete(any());

        when(movements.save(any())).thenAnswer(invocation -> keep(movementStore, invocation.getArgument(0), "cm-"));
        when(movements.findById(any())).thenAnswer(invocation -> Optional.ofNullable(movementStore.get(invocation.getArgument(0))));
        when(movements.findAll()).thenAnswer(invocation -> new ArrayList<>(movementStore.values()));
        when(movements.findByProductIdOrderByCreatedAtDesc(any())).thenAnswer(invocation -> movementStore.values().stream()
                .filter(movement -> invocation.getArgument(0).equals(movement.getProductId()))
                .toList());
        when(movements.findByOperationId(any())).thenAnswer(invocation -> movementStore.values().stream()
                .filter(movement -> invocation.getArgument(0).equals(movement.getOperationId()))
                .findFirst());
        when(movements.findByProductKindAndCreatedAtBetween(any(), any(), any())).thenAnswer(invocation ->
                movementStore.values().stream()
                        .filter(movement -> invocation.getArgument(0).equals(movement.getProductKind()))
                        .toList());
        doAnswer(invocation -> {
            movementStore.remove(((CreditMovementDocument) invocation.getArgument(0)).getId());
            return null;
        }).when(movements).delete(any());
    }

    @Test
    void loanCanBeOpenedPaidUpdatedAndDeleted() {
        LocalDate due = LocalDate.now(ZoneOffset.UTC).plusDays(20);
        LoanDocument loan = service.openLoan("biz", new BigDecimal("100"), due).blockingGet();

        assertEquals(1, service.findLoans().blockingGet().size());
        assertEquals(loan.getId(), service.findLoan(loan.getId()).blockingGet().getId());
        LoanDocument moved = service.updateLoan(loan.getId(), due.plusDays(5)).blockingGet();
        LoanDocument paid = service.payLoan(moved.getId(), new BigDecimal("100"), "payer").blockingGet();

        assertEquals(0, BigDecimal.ZERO.compareTo(paid.getOutstanding()));
        assertTrue(service.deleteLoan(paid.getId()).blockingGet());
    }

    @Test
    void aLoanWithDebtCannotBeDeleted() {
        LoanDocument loan = service.openLoan("biz", new BigDecimal("100"), LocalDate.now(ZoneOffset.UTC).plusDays(10)).blockingGet();

        assertThrows(IllegalArgumentException.class, () -> service.deleteLoan(loan.getId()).blockingGet());
    }

    @Test
    void aPersonalCustomerCannotOpenASecondLoan() {
        when(customers.customerType("ana")).thenReturn("PERSONAL");
        when(loans.countByCustomerIdAndCustomerTypeAndStatus("ana", "PERSONAL", "ACTIVE")).thenReturn(1L);

        assertThrows(IllegalArgumentException.class,
                () -> service.openLoan("ana", new BigDecimal("50"), LocalDate.now(ZoneOffset.UTC).plusDays(10)).blockingGet());
    }

    @Test
    void cardChargePaymentAndMovementCrudKeepTheAvailableLimit() {
        CreditCardDocument card = service.openCard("biz", new BigDecimal("200"), LocalDate.now(ZoneOffset.UTC).plusDays(15)).blockingGet();
        assertEquals(1, service.findCards().blockingGet().size());
        CreditCardDocument charged = service.charge(card.getId(), new BigDecimal("40")).blockingGet();
        assertEquals(0, new BigDecimal("160.00").compareTo(charged.getAvailable()));
        assertThrows(IllegalArgumentException.class, () -> service.deleteCard(card.getId()).blockingGet());

        CreditMovementDocument movement = service.createMovement(card.getId(), "CARD", "PAYMENT", new BigDecimal("40"), "payer").blockingGet();
        assertEquals(0, new BigDecimal("200.00").compareTo(service.findCard(card.getId()).blockingGet().getAvailable()));
        assertEquals(movement.getId(), service.findMovement(movement.getId()).blockingGet().getId());
        assertEquals("other", service.updateMovement(movement.getId(), "other").blockingGet().getPayerCustomerId());
        assertEquals(2, service.findMovements(card.getId()).blockingGet().size());
        assertEquals(2, service.findMovements(null).blockingGet().size());
        assertEquals(2, service.lastCardMovements(card.getId()).blockingGet().size());
        assertEquals(2, service.report("CARD", Instant.EPOCH, Instant.now()).blockingGet().size());

        assertTrue(service.deleteMovement(movement.getId()).blockingGet());
        assertEquals(0, new BigDecimal("160.00").compareTo(service.findCard(card.getId()).blockingGet().getAvailable()));
        service.payCard(card.getId(), new BigDecimal("40"), "payer").blockingGet();
        CreditCardDocument updated = service.updateCard(card.getId(), LocalDate.now(ZoneOffset.UTC).plusDays(30)).blockingGet();
        assertTrue(service.deleteCard(updated.getId()).blockingGet());
    }

    @Test
    void loanMovementMustBeAPaymentWithAPayer() {
        LoanDocument loan = service.openLoan("biz", new BigDecimal("80"), LocalDate.now(ZoneOffset.UTC).plusDays(8)).blockingGet();

        assertThrows(IllegalArgumentException.class,
                () -> service.createMovement(loan.getId(), "LOAN", "CHARGE", new BigDecimal("10"), "payer").blockingGet());
        assertThrows(IllegalArgumentException.class,
                () -> service.createMovement(loan.getId(), "LOAN", "PAYMENT", new BigDecimal("10"), " ").blockingGet());

        CreditMovementDocument payment = service.createMovement(loan.getId(), "LOAN", "PAYMENT", new BigDecimal("10"), "payer").blockingGet();
        assertEquals(0, new BigDecimal("70.00").compareTo(service.findLoan(loan.getId()).blockingGet().getOutstanding()));
        assertTrue(service.deleteMovement(payment.getId()).blockingGet());
        assertEquals(0, new BigDecimal("80.00").compareTo(service.findLoan(loan.getId()).blockingGet().getOutstanding()));
    }

    @Test
    void externalPaymentIsAppliedOnce() {
        LoanDocument loan = service.openLoan("biz", new BigDecimal("50"), LocalDate.now(ZoneOffset.UTC).plusDays(4)).blockingGet();

        service.applyExternalPayment("op-1", loan.getId(), "LOAN", new BigDecimal("20"), "payer");
        service.applyExternalPayment("op-1", loan.getId(), "LOAN", new BigDecimal("20"), "payer");

        assertEquals(0, new BigDecimal("30.00").compareTo(service.findLoan(loan.getId()).blockingGet().getOutstanding()));
    }

    @Test
    void overdueFollowsTheStoredProducts() {
        LoanDocument late = new LoanDocument();
        late.setDueDate(LocalDate.now(ZoneOffset.UTC).minusDays(1));
        late.setOutstanding(new BigDecimal("10"));
        when(loans.findByCustomerId("late")).thenReturn(List.of(late));

        assertTrue(service.overdue("late").blockingGet());
        assertFalse(service.overdue("biz").blockingGet());
        assertTrue(service.hasActiveCard("biz").blockingGet());
    }

    private <T> T keep(Map<String, T> store, T document, String prefix) {
        if (document instanceof LoanDocument loan && loan.getId() == null) {
            loan.setId(prefix + sequence.incrementAndGet());
        }
        if (document instanceof CreditCardDocument card && card.getId() == null) {
            card.setId(prefix + sequence.incrementAndGet());
        }
        if (document instanceof CreditMovementDocument movement && movement.getId() == null) {
            movement.setId(prefix + sequence.incrementAndGet());
        }
        String id = idOf(document);
        store.put(id, document);
        return document;
    }

    private String idOf(Object document) {
        if (document instanceof LoanDocument loan) {
            return loan.getId();
        }
        if (document instanceof CreditCardDocument card) {
            return card.getId();
        }
        return ((CreditMovementDocument) document).getId();
    }
}
