package com.royal.reserve.bank.account.api.controller;

import com.royal.reserve.bank.account.api.service.AccountService;
import com.royal.reserve.bank.account.api.dto.AccountResponse;
import com.royal.reserve.bank.account.api.dto.AccountRequest;
import com.royal.reserve.bank.account.api.dto.DepositRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Controller class that handles HTTP requests related to bank accounts.
 */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Creates a new bank account.
     *
     * @param accountRequest The account request containing account details.
     * @return A ResponseEntity with a success message and HTTP status code 201 if the account was created successfully.
     */
    @PostMapping
    public ResponseEntity<String> createAccount(@RequestBody AccountRequest accountRequest) {
        accountService.createAccount(accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body
                ("Successfully set up a new bank account for " +
                accountRequest.getAccountHolderName() + ".");
    }

    /**
     * Retrieves all bank accounts.
     *
     * @return A list of AccountResponse objects representing the bank accounts.
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<AccountResponse> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    /**
     * Deletes a bank account based on the account holder name.
     *
     * @param accountRequest The account request containing the account holder name.
     * @return A ResponseEntity with a success message and HTTP status code 200 if the account was deleted successfully,
     *         or a ResponseEntity with an error message and HTTP status code 404 if the account was not found.
     */
    @DeleteMapping
    public ResponseEntity<String> deleteAccount(@RequestBody AccountRequest accountRequest) {
        try {
            accountService.deleteAccountByAccountHolderName(accountRequest.getAccountHolderName());
            return ResponseEntity.status(HttpStatus.OK).body("Successfully deleted " +
                    accountRequest.getAccountHolderName() + "'s account.");
        } catch (RuntimeException runtimeException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(runtimeException.getMessage());
        }
    }

    /**
     * Adds funds to an existing bank account.
     *
     * @param id             The account id.
     * @param depositRequest The deposit request containing a positive amount.
     * @return The updated account with HTTP 200, 400 if the amount is invalid, or 404 if the account is not found.
     */
    @PostMapping("/{id}/deposit")
    public ResponseEntity<Object> deposit(@PathVariable String id, @RequestBody DepositRequest depositRequest) {
        try {
            return ResponseEntity.ok(accountService.deposit(id, depositRequest));
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.badRequest().body(illegalArgumentException.getMessage());
        } catch (NoSuchElementException noSuchElementException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(noSuchElementException.getMessage());
        }
    }
}
