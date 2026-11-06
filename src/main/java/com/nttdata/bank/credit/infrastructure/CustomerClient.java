package com.nttdata.bank.credit.infrastructure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Reads a customer through Eureka. The call is protected by a circuit breaker and a two-second timeout.
 */
@Component
public class CustomerClient {

    private final RestTemplate restTemplate;
    private final CircuitBreakerRegistry circuitBreakers;
    private final TimeLimiterRegistry timeLimiters;
    private final SecretKey secretKey;
    private final long expirationMinutes;

    /**
     * @param restTemplate load-balanced client
     * @param circuitBreakers resilience4j circuit breakers
     * @param timeLimiters resilience4j time limiters
     * @param secret shared JWT secret
     * @param expirationMinutes service-token lifetime
     */
    public CustomerClient(@LoadBalanced RestTemplate restTemplate,
                          CircuitBreakerRegistry circuitBreakers,
                          TimeLimiterRegistry timeLimiters,
                          @Value("${bank.security.jwt-secret}") String secret,
                          @Value("${bank.security.jwt-expiration-minutes}") long expirationMinutes) {
        this.restTemplate = restTemplate;
        this.circuitBreakers = circuitBreakers;
        this.timeLimiters = timeLimiters;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * @param customerId customer identifier
     * @return PERSONAL or BUSINESS
     */
    public String customerType(String customerId) {
        Supplier<String> call = () -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(serviceToken());
            CustomerView view = restTemplate.exchange(
                    "http://ms-customer/api/v1/customers/" + customerId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    CustomerView.class).getBody();
            if (view == null || view.customerType() == null) {
                throw new IllegalArgumentException("Customer was not found");
            }
            return view.customerType();
        };
        try {
            return circuitBreakers.circuitBreaker("customer").executeCallable(
                    io.github.resilience4j.timelimiter.TimeLimiter.decorateFutureSupplier(
                            timeLimiters.timeLimiter("customer"),
                            () -> CompletableFuture.supplyAsync(call)));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Customer service is unavailable");
        }
    }

    private String serviceToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("ms-credit")
                .claim("roles", List.of("SERVICE"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * @param id customer identifier
     * @param customerType PERSONAL or BUSINESS
     */
    public record CustomerView(String id, String customerType) {
    }
}
