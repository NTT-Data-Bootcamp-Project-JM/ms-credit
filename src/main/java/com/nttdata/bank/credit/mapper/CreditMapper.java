package com.nttdata.bank.credit.mapper;

import com.nttdata.bank.credit.api.dto.CreditCardResponse;
import com.nttdata.bank.credit.api.dto.CreditMovementResponse;
import com.nttdata.bank.credit.api.dto.LoanResponse;
import com.nttdata.bank.credit.domain.model.CreditCardDocument;
import com.nttdata.bank.credit.domain.model.CreditMovementDocument;
import com.nttdata.bank.credit.domain.model.LoanDocument;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Maps credit domain models to API DTOs.
 */
@Mapper(componentModel = "spring")
public interface CreditMapper {

    /**
     * @param document loan aggregate
     * @return API response
     */
    LoanResponse toResponse(LoanDocument document);

    /**
     * @param documents loan aggregates
     * @return API responses
     */
    List<LoanResponse> toLoanResponseList(List<LoanDocument> documents);

    /**
     * @param document credit card aggregate
     * @return API response
     */
    CreditCardResponse toResponse(CreditCardDocument document);

    /**
     * @param documents credit card aggregates
     * @return API responses
     */
    List<CreditCardResponse> toCreditCardResponseList(List<CreditCardDocument> documents);

    /**
     * @param document credit movement aggregate
     * @return API response
     */
    CreditMovementResponse toResponse(CreditMovementDocument document);

    /**
     * @param documents credit movement aggregates
     * @return API responses
     */
    List<CreditMovementResponse> toCreditMovementResponseList(List<CreditMovementDocument> documents);
}