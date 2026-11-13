package com.royal.reserve.bank.transaction.api.controller;

import com.royal.reserve.bank.transaction.api.dto.TransactionRequest;
import com.royal.reserve.bank.transaction.api.service.TransactionService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Controller class that handles HTTP requests related to transactions.
 */
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

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
    public CompletableFuture<String> processTransaction(@Valid @RequestBody TransactionRequest transactionRequest) {
        log.info("Transaction processed.");
        return CompletableFuture.supplyAsync(() -> transactionService.processTransaction(transactionRequest));
    }

    /**
     *
     *Circuit breaker implementation. Fallback method to handle exceptions during transaction processing.
     *@param transactionRequest The transaction request object.
     *@param throwable The exception that occurred during transaction processing.
     *@return A CompletableFuture representing a fallback message.
     */
    public CompletableFuture<String> fallbackMethod(TransactionRequest transactionRequest, Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof IllegalArgumentException) {
            return CompletableFuture.failedFuture(cause);
        }
        log.info("Transaction can't be processed. Executing fallback logic.");
        return CompletableFuture.supplyAsync(() -> "Oops! Something went wrong, please try again later!");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidTransaction(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}