package com.royal.reserve.bank.account.api.controller;

import com.royal.reserve.bank.account.api.exception.AccountAccessDeniedException;
import com.royal.reserve.bank.account.api.service.AccountService;
import com.royal.reserve.bank.account.api.dto.AccountResponse;
import com.royal.reserve.bank.account.api.dto.AccountRequest;
import com.royal.reserve.bank.account.api.util.AuthHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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
     * Creates a new bank account owned by the authenticated caller.
     *
     * @param accountRequest The account request containing account details.
     * @param subject        The authenticated caller's subject, propagated by the API gateway.
     * @return A ResponseEntity with a success message and HTTP status code 201 if the account was created successfully.
     */
    @PostMapping
    public ResponseEntity<String> createAccount(@RequestBody AccountRequest accountRequest,
                                                @RequestHeader(AuthHeaders.SUBJECT) String subject) {
        accountService.createAccount(accountRequest, subject);
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
     * Deletes a bank account based on the account holder name, when the caller is
     * the account owner or holds the admin permission.
     *
     * @param accountRequest The account request containing the account holder name.
     * @param subject        The authenticated caller's subject, propagated by the API gateway.
     * @param permissions    The authenticated caller's permissions, propagated by the API gateway.
     * @return A ResponseEntity with a success message and HTTP status code 200 if the account was deleted successfully,
     *         a ResponseEntity with an error message and HTTP status code 403 if the caller is not allowed,
     *         or a ResponseEntity with an error message and HTTP status code 404 if the account was not found.
     */
    @DeleteMapping
    public ResponseEntity<String> deleteAccount(@RequestBody AccountRequest accountRequest,
                                                @RequestHeader(AuthHeaders.SUBJECT) String subject,
                                                @RequestHeader(name = AuthHeaders.PERMISSIONS,
                                                        required = false, defaultValue = "")
                                                String permissions) {
        boolean callerIsAdmin = Arrays.stream(permissions.split(","))
                .map(String::trim)
                .anyMatch(AuthHeaders.ADMIN_PERMISSION::equals);
        try {
            accountService.deleteAccount(accountRequest.getAccountHolderName(), subject, callerIsAdmin);
            return ResponseEntity.status(HttpStatus.OK).body("Successfully deleted " +
                    accountRequest.getAccountHolderName() + "'s account.");
        } catch (AccountAccessDeniedException accessDeniedException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(accessDeniedException.getMessage());
        } catch (NoSuchElementException noSuchElementException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(noSuchElementException.getMessage());
        }
    }
}
