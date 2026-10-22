package com.royal.reserve.bank.account.api.unit.controller;

import com.royal.reserve.bank.account.api.controller.AccountController;
import com.royal.reserve.bank.account.api.dto.AccountRequest;
import com.royal.reserve.bank.account.api.dto.AccountResponse;
import com.royal.reserve.bank.account.api.dto.DepositRequest;
import com.royal.reserve.bank.account.api.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

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
     * Test for the {@link AccountController#createAccount(AccountRequest)} method.
     */
    @Test
    void testCreateAccount() {
        // Given
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountHolderName("Al Pacino");

        // When
        ResponseEntity<String> responseEntity = accountController.createAccount(accountRequest);

        // Then
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals("Successfully set up a new bank account for Al Pacino.", responseEntity.getBody());
        verify(accountService, times(1)).createAccount(accountRequest);
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
     * Test for the {@link AccountController#deleteAccount(AccountRequest)} method.
     */
    @Test
    void testDeleteAccount() {
        // Given
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountHolderName("Al Pacino");

        // When
        ResponseEntity<String> responseEntity = accountController.deleteAccount(accountRequest);

        // Then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("Successfully deleted Al Pacino's account.", responseEntity.getBody());
        verify(accountService, times(1)).deleteAccountByAccountHolderName("Al Pacino");
    }

    /**
     * Test for the {@link AccountController#deposit(String, DepositRequest)} method.
     */
    @Test
    void testDeposit() {
        // Given
        DepositRequest depositRequest = new DepositRequest(BigDecimal.valueOf(500));
        AccountResponse updated = new AccountResponse();
        updated.setId("acc-1");
        updated.setBalance(BigDecimal.valueOf(1500));
        when(accountService.deposit("acc-1", depositRequest)).thenReturn(updated);

        // When
        ResponseEntity<Object> responseEntity = accountController.deposit("acc-1", depositRequest);

        // Then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(updated, responseEntity.getBody());
    }

    /**
     * Test for the {@link AccountController#deposit(String, DepositRequest)} method with an invalid amount.
     */
    @Test
    void testDepositInvalidAmount() {
        // Given
        DepositRequest depositRequest = new DepositRequest(BigDecimal.ZERO);
        when(accountService.deposit("acc-1", depositRequest))
                .thenThrow(new IllegalArgumentException("The deposit amount must be greater than zero."));

        // When
        ResponseEntity<Object> responseEntity = accountController.deposit("acc-1", depositRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("The deposit amount must be greater than zero.", responseEntity.getBody());
    }

    /**
     * Test for the {@link AccountController#deposit(String, DepositRequest)} method when the account is not found.
     */
    @Test
    void testDepositAccountNotFound() {
        // Given
        DepositRequest depositRequest = new DepositRequest(BigDecimal.ONE);
        when(accountService.deposit("missing", depositRequest))
                .thenThrow(new NoSuchElementException("No bank account found with id missing."));

        // When
        ResponseEntity<Object> responseEntity = accountController.deposit("missing", depositRequest);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals("No bank account found with id missing.", responseEntity.getBody());
    }
}
