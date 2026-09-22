package com.royal.reserve.bank.account.api.unit.controller;

import com.royal.reserve.bank.account.api.controller.AccountController;
import com.royal.reserve.bank.account.api.dto.AccountRequest;
import com.royal.reserve.bank.account.api.dto.AccountResponse;
import com.royal.reserve.bank.account.api.exception.AccountAccessDeniedException;
import com.royal.reserve.bank.account.api.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the {@link AccountController} class.
 */
@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    /**
     * Test for the {@link AccountController#createAccount(AccountRequest, String)} method.
     */
    @Test
    void testCreateAccount() {
        // Given
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountHolderName("Al Pacino");

        // When
        ResponseEntity<String> responseEntity =
                accountController.createAccount(accountRequest, "auth0|alice");

        // Then
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals("Successfully set up a new bank account for Al Pacino.", responseEntity.getBody());
        verify(accountService, times(1)).createAccount(accountRequest, "auth0|alice");
    }

    /**
     * Test for the {@link AccountController#getAllAccounts()} method.
     */
    @Test
    void testGetAllAccounts() {
        // Given
        AccountResponse accountResponse1 = new AccountResponse();
        accountResponse1.setAccountNumber("LI42-3842-3283-9483-4892");
        accountResponse1.setAccountHolderName("Tom Hanks");

        AccountResponse accountResponse2 = new AccountResponse();
        accountResponse2.setAccountNumber("DE32-8473-8127-1823-1732");
        accountResponse2.setAccountHolderName("Julia Roberts");

        List<AccountResponse> expectedAccounts = Arrays.asList(accountResponse1, accountResponse2);

        when(accountService.getAllAccounts()).thenReturn(expectedAccounts);

        // When
        List<AccountResponse> actualAccounts = accountController.getAllAccounts();

        // Then
        assertEquals(expectedAccounts, actualAccounts);
        verify(accountService, times(1)).getAllAccounts();
    }

    /**
     * Test for the {@link AccountController#deleteAccount(AccountRequest, String, String)} method.
     */
    @Test
    void testDeleteAccount() {
        // Given
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountHolderName("Al Pacino");

        // When
        ResponseEntity<String> responseEntity =
                accountController.deleteAccount(accountRequest, "auth0|alice", "");

        // Then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("Successfully deleted Al Pacino's account.", responseEntity.getBody());
        verify(accountService, times(1)).deleteAccount("Al Pacino", "auth0|alice", false);
    }

    /**
     * Test for the {@link AccountController#deleteAccount(AccountRequest, String, String)} method
     * when the caller is not allowed to delete the account.
     */
    @Test
    void testDeleteAccountForbidden() {
        // Given
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountHolderName("Al Pacino");
        doThrow(new AccountAccessDeniedException(
                "You are not allowed to delete the bank account of Al Pacino."))
                .when(accountService).deleteAccount("Al Pacino", "auth0|bob", false);

        // When
        ResponseEntity<String> responseEntity =
                accountController.deleteAccount(accountRequest, "auth0|bob", "");

        // Then
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals("You are not allowed to delete the bank account of Al Pacino.",
                responseEntity.getBody());
    }

    /**
     * Test for the {@link AccountController#deleteAccount(AccountRequest, String, String)} method
     * when the caller holds the admin permission.
     */
    @Test
    void testDeleteAccountAsAdmin() {
        // Given
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountHolderName("Al Pacino");

        // When
        ResponseEntity<String> responseEntity =
                accountController.deleteAccount(accountRequest, "auth0|admin", "accounts:admin");

        // Then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(accountService).deleteAccount("Al Pacino", "auth0|admin", true);
    }
}
