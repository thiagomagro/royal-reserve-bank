package com.royal.reserve.bank.account.api.service;

import com.royal.reserve.bank.account.api.model.Account;
import com.royal.reserve.bank.account.api.repository.AccountRepository;
import com.royal.reserve.bank.account.api.dto.AccountResponse;
import com.royal.reserve.bank.account.api.dto.AccountRequest;
import com.royal.reserve.bank.account.api.dto.DepositRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
     */
    public void createAccount(AccountRequest accountRequest) {
        Account account = Account.builder()
                .accountNumber(generateIBAN())
                .accountHolderName(accountRequest.getAccountHolderName())
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
     * Adds funds to an existing bank account.
     *
     * @param id             The account id.
     * @param depositRequest The deposit request containing a positive amount.
     * @return The updated account.
     * @throws IllegalArgumentException if the amount is missing or not positive.
     * @throws NoSuchElementException   if the account is not found.
     */
    public AccountResponse deposit(String id, DepositRequest depositRequest) {
        BigDecimal amount = depositRequest == null ? null : depositRequest.getAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("The deposit amount must be greater than zero.");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No bank account found with id " + id + "."));

        BigDecimal currentBalance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
        account.setBalance(currentBalance.add(amount));
        accountRepository.save(account);
        redisTemplate.delete(CACHE_KEY);
        log.info("Deposited {} into the account of {}", amount, account.getAccountHolderName());
        return mapToAccountResponse(account);
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
     * Deletes a bank account based on the account holder name.
     *
     * @param name The account holder name.
     * @throws NoSuchElementException if the account is not found.
     */
    public void deleteAccountByAccountHolderName(String name) {
        List<Account> accounts = accountRepository.findAll();

        Optional<Account> accountToDelete = accounts.stream()
                .filter(a -> a.getAccountHolderName() != null &&
                        a.getAccountHolderName().equals(name)).findFirst();

        if (accountToDelete.isPresent()) {
            accountRepository.delete(accountToDelete.get());
            redisTemplate.delete(CACHE_KEY);
        } else {
            throw new NoSuchElementException("The bank account information for "
                    + name + " was not found.");
        }
    }
}
