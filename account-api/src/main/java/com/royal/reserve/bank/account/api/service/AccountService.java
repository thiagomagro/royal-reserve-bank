package com.royal.reserve.bank.account.api.service;

import com.royal.reserve.bank.account.api.exception.AccountAccessDeniedException;
import com.royal.reserve.bank.account.api.model.Account;
import com.royal.reserve.bank.account.api.repository.AccountRepository;
import com.royal.reserve.bank.account.api.dto.AccountResponse;
import com.royal.reserve.bank.account.api.dto.AccountRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service class that provides operations for managing bank accounts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    private final RedisTemplate<String, List<AccountResponse>> redisTemplate;

    private static final String CACHE_KEY = "accounts";

    private static final Random random = new Random();

    /**
     * Creates a new bank account.
     *
     * @param accountRequest The account request containing account details.
     * @param ownerSubject   The subject of the authenticated account owner.
     */
    public void createAccount(AccountRequest accountRequest, String ownerSubject) {
        Account account = Account.builder()
                .accountNumber(generateIBAN())
                .accountHolderName(accountRequest.getAccountHolderName())
                .ownerSubject(ownerSubject)
                .balance(accountRequest.getBalance())
                .currency(accountRequest.getCurrency())
                .build();

        accountRepository.save(account);
        redisTemplate.delete(CACHE_KEY);
        log.info("Account for {} is created", account.getAccountHolderName());
    }

    /**
     * Generates a random International Bank Account Number (IBAN).
     *
     * @return The generated IBAN.
     */
    public static String generateIBAN() {
        String[] countryCodes = Locale.getISOCountries();
        int index = random.nextInt(countryCodes.length);
        String countryCode = countryCodes[index];
        String accountNumber = String.format("%02d-%04d-%04d-%04d-%04d",
                random.nextInt(100),
                random.nextInt(10000),
                random.nextInt(10000),
                random.nextInt(10000),
                random.nextInt(10000));
        return countryCode + accountNumber;
    }

    /**
     * Retrieves all bank accounts.
     *
     * @return A list of AccountResponse objects representing the bank accounts.
     */
    public List<AccountResponse> getAllAccounts() {
        List<AccountResponse> cachedAccounts = redisTemplate.opsForValue().get(CACHE_KEY);

        if (cachedAccounts != null) {
            return cachedAccounts;
        } else {
            List<Account> accounts = accountRepository.findAll();

            List<AccountResponse> accountResponses = accounts.stream()
                    .map(this::mapToAccountResponse)
                    .toList();
            redisTemplate.opsForValue().set(CACHE_KEY, accountResponses);
            return accountResponses;
        }
    }

    /**
     * Maps an Account object to an AccountResponse object.
     *
     * @param account The Account object to map.
     * @return The mapped AccountResponse object.
     */
    private AccountResponse mapToAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .build();
    }

    /**
     * Deletes a bank account based on the account holder name, if the caller is
     * authorized to do so.
     *
     * @param name           The account holder name.
     * @param callerSubject  The subject of the authenticated caller.
     * @param callerIsAdmin  Whether the caller holds the admin permission.
     * @throws NoSuchElementException        if the account is not found.
     * @throws AccountAccessDeniedException if the caller is not the owner nor an admin.
     */
    public void deleteAccount(String name, String callerSubject, boolean callerIsAdmin) {
        List<Account> accounts = accountRepository.findAll();

        Optional<Account> accountToDelete = accounts.stream()
                .filter(a -> a.getAccountHolderName() != null &&
                        a.getAccountHolderName().equals(name)).findFirst();

        if (accountToDelete.isEmpty()) {
            throw new NoSuchElementException("The bank account information for "
                    + name + " was not found.");
        }

        Account account = accountToDelete.get();
        if (!callerIsAdmin && !callerSubject.equals(account.getOwnerSubject())) {
            throw new AccountAccessDeniedException("You are not allowed to delete the bank account of "
                    + name + ".");
        }

        accountRepository.delete(account);
        redisTemplate.delete(CACHE_KEY);
    }
}
