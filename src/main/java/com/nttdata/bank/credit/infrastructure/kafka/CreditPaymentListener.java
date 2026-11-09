package com.nttdata.bank.credit.infrastructure.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nttdata.bank.credit.service.CreditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Applies credit payments requested by the account service. Yanki and account do not call this API.
 */
@Component
public class CreditPaymentListener {

    private static final Logger LOG = LoggerFactory.getLogger(CreditPaymentListener.class);
    private final CreditService credits;
    private final ObjectMapper json = new ObjectMapper();

    /**
     * @param credits credit use cases
     */
    public CreditPaymentListener(CreditService credits) {
        this.credits = credits;
    }

    /**
     * Consumes one payment event.
     *
     * @param payload JSON payment request
     */
    @KafkaListener(topics = "credit.payment.requested")
    public void onPayment(String payload) {
        try {
            JsonNode node = json.readTree(payload);
            credits.applyExternalPayment(
                    node.get("operationId").asText(),
                    node.get("productId").asText(),
                    node.get("productKind").asText(),
                    new BigDecimal(node.get("amount").asText()),
                    node.get("payerCustomerId").asText());
        } catch (Exception ex) {
            LOG.warn("Credit payment event was rejected: {}", ex.getMessage());
        }
    }
}
