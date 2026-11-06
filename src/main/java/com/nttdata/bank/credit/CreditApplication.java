package com.nttdata.bank.credit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Owns loans and credit cards. A card is a separate product from the single personal loan.
 */
@SpringBootApplication
public class CreditApplication {

    /**
     * Starts the credit service.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CreditApplication.class, args);
    }
}
