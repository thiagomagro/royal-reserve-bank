package com.royal.reserve.bank.transaction.api.controller;

import com.royal.reserve.bank.transaction.api.dto.TransactionRequest;
import com.royal.reserve.bank.transaction.api.model.TransactionItems;
import com.royal.reserve.bank.transaction.api.service.TransactionService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.List;

/**
 * Controller class that handles HTTP requests related to transactions.
 */
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/search")
    public List<TransactionItems> searchByAssetCode(@RequestParam @NotBlank
                                                   @Size(max = TransactionService.MAX_ASSET_CODE_LENGTH)
                                                   @Pattern(regexp = TransactionService.ASSET_CODE_PATTERN) String assetCode) {
        log.info("Searching transaction items by asset code.");
        return transactionService.searchByAssetCode(assetCode);
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<String> handleInvalidSearch(RuntimeException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    /**
     *
     *Processes a transaction asynchronously.
     *@param transactionRequest The transaction request object received in the request body.
     *@return A CompletableFuture representing the result of the transaction processing.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CircuitBreaker(name = "asset-management", fallbackMethod = "fallbackMethod")
    @TimeLimiter(name = "asset-management")
    @Retry(name = "asset-management")
    public CompletableFuture<String> processTransaction(@RequestBody TransactionRequest transactionRequest) {
        log.info("Transaction processed.");
        return CompletableFuture.supplyAsync(() -> transactionService.processTransaction(transactionRequest));
    }

    /**
     *
     *Circuit breaker implementation. Fallback method to handle exceptions during transaction processing.
     *@param transactionRequest The transaction request object.
     *@param runtimeException The exception that occurred during transaction processing.
     *@return A CompletableFuture representing a fallback message.
     */
    public CompletableFuture<String> fallbackMethod(TransactionRequest transactionRequest, RuntimeException runtimeException) {
        log.info("Transaction can't be processed. Executing fallback logic.");
        return CompletableFuture.supplyAsync(() -> "Oops! Something went wrong, please try again later!");
    }
}