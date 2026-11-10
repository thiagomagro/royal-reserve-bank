package com.royal.reserve.bank.transaction.api.unit.repository;

import com.royal.reserve.bank.transaction.api.model.Transaction;
import com.royal.reserve.bank.transaction.api.model.TransactionItems;
import com.royal.reserve.bank.transaction.api.repository.TransactionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.List;


/**
 * Unit tests for the {@link TransactionRepository} class.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    @Container
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer(DockerImageName.parse("postgres:15.3"));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        dynamicPropertyRegistry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        dynamicPropertyRegistry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private TransactionRepository transactionRepository;

    private Transaction newTransaction;

    @BeforeEach
    void setUp() {
        newTransaction = new Transaction();
        newTransaction.setTransactionId("3532");

        List<TransactionItems> transactionItemsList = new ArrayList<>();
        TransactionItems transactionItem1 = new TransactionItems();
        transactionItemsList.add(transactionItem1);

        TransactionItems transactionItem2 = new TransactionItems();
        transactionItemsList.add(transactionItem2);

        newTransaction.setTransactionItemsList(transactionItemsList);
    }

    /**
     * Test CRUD operations - save.
     */
    @Test
    void testSave() {
        // Given
        Transaction savedTransaction = transactionRepository.save(newTransaction);

        // When and Then
        Assertions.assertNotNull(savedTransaction.getTransactionId());
        Assertions.assertEquals("3532", savedTransaction.getTransactionId());
    }

    /**
     * Test CRUD operations - find by id.
     */
    @Test
    void testFindById() {
        // Given
        Transaction savedTransaction = transactionRepository.save(newTransaction);
        Long transactionId = savedTransaction.getId();

        // When
        Transaction foundTransaction = transactionRepository.findById(transactionId).orElse(null);

        // Then
        Assertions.assertNotNull(foundTransaction);
    }

    /**
     * Test CRUD operations - delete.
     */
    @Test
    void testDelete() {
        // Given
        Transaction savedTransaction = transactionRepository.save(newTransaction);
        Long transactionId = savedTransaction.getId();
        Transaction retrievedTransaction = transactionRepository.findById(transactionId).orElse(null);
        Assertions.assertNotNull(retrievedTransaction);

        // When
        transactionRepository.delete(retrievedTransaction);

        // Then
        Transaction deletedTransaction = transactionRepository.findById(transactionId).orElse(null);
        Assertions.assertNull(deletedTransaction);
    }
}