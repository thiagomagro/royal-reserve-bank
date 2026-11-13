package com.royal.reserve.bank.transaction.api.unit.controller;

import com.royal.reserve.bank.transaction.api.controller.TransactionController;
import com.royal.reserve.bank.transaction.api.dto.TransactionItemsDto;
import com.royal.reserve.bank.transaction.api.dto.TransactionRequest;
import com.royal.reserve.bank.transaction.api.service.TransactionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Unit tests for the {@link TransactionController} class.
 */
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private TransactionRequest request;

    @Test
    void rejectsInvalidSearchCodesWithBadRequest() throws Exception {
        try (LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            ProxyFactory proxy = new ProxyFactory(transactionController);
            proxy.addAdvice(new MethodValidationInterceptor(validator.getValidator()));
            var mvc = MockMvcBuilders.standaloneSetup(proxy.getProxy()).build();
            for (String assetCode : List.of("", " ", "A".repeat(33), "' OR 1=1 --")) {
                mvc.perform(get("/api/transaction/search").param("assetCode", assetCode))
                        .andExpect(status().isBadRequest());
            }
            mvc.perform(get("/api/transaction/search")).andExpect(status().isBadRequest());
            verifyNoInteractions(transactionService);
        }
    }

    @Test
    void returnsSearchResultsForValidCode() throws Exception {
        when(transactionService.searchByAssetCode("DVN")).thenReturn(List.of());
        MockMvcBuilders.standaloneSetup(transactionController).build()
                .perform(get("/api/transaction/search").param("assetCode", "DVN"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void acceptsHyphenatedSearchCodeThroughValidation() throws Exception {
        try (LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            ProxyFactory proxy = new ProxyFactory(transactionController);
            proxy.addAdvice(new MethodValidationInterceptor(validator.getValidator()));
            when(transactionService.searchByAssetCode("BOND-BR-2030")).thenReturn(List.of());
            MockMvcBuilders.standaloneSetup(proxy.getProxy()).build()
                    .perform(get("/api/transaction/search").param("assetCode", "BOND-BR-2030"))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));
        }
    }

    @BeforeEach
    void setup() {
        TransactionItemsDto item1 = new TransactionItemsDto(1L, "AAPL", "Apple Inc.",
                19000);
        TransactionItemsDto item2 = new TransactionItemsDto(2L, "GOOGL", "Alphabet Inc.",
                10200);
        List<TransactionItemsDto> transactionItemList = Arrays.asList(item1, item2);
        request = new TransactionRequest(transactionItemList);
    }

    /**
     * Test for the {@link TransactionController#processTransaction(TransactionRequest)} method.
     */
    @Test
    void testProcessTransaction() {
        // Given
        String expectedResponse = "Transaction processed.";
        when(transactionService.processTransaction(any(TransactionRequest.class))).thenReturn(expectedResponse);

        // When
        CompletableFuture<String> result = transactionController.processTransaction(request);

        // Then
        Assertions.assertEquals(expectedResponse, result.join());
    }

    /**
     * Test for the {@link TransactionController#fallbackMethod(TransactionRequest, RuntimeException)} method.
     */
    @Test
    void testFallbackMethod() {
        // Given
        RuntimeException exception = new RuntimeException("Error occurred during transaction processing");
        String expectedFallbackResponse = "Oops! Something went wrong, please try again later!";

        // When
        CompletableFuture<String> result = transactionController.fallbackMethod(request, exception);

        // Then
        Assertions.assertEquals(expectedFallbackResponse, result.join());
    }
}
