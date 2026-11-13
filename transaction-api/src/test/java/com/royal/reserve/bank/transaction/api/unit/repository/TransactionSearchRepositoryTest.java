package com.royal.reserve.bank.transaction.api.unit.repository;

import com.royal.reserve.bank.transaction.api.model.TransactionItems;
import com.royal.reserve.bank.transaction.api.repository.TransactionSearchRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class TransactionSearchRepositoryTest {
    @Test
    void bindsAssetCodesIncludingSqlSyntaxAsLiteralValues() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        var repository = new TransactionSearchRepositoryImpl(jdbcTemplate);
        String sql = "SELECT id, asset_code, asset_name, value FROM t_transaction_items WHERE asset_code = ?";
        for (String assetCode : List.of("DVN", "' OR 1=1 --")) {
            List<TransactionItems> items = List.of(new TransactionItems(1L, assetCode, "Asset", 100));
            when(jdbcTemplate.query(eq(sql), org.mockito.ArgumentMatchers.<RowMapper<TransactionItems>>any(), eq(assetCode)))
                    .thenReturn(items);
            assertEquals(items, repository.findItemsByAssetCode(assetCode));
            verify(jdbcTemplate).query(eq(sql), org.mockito.ArgumentMatchers.<RowMapper<TransactionItems>>any(), eq(assetCode));
        }
        verifyNoMoreInteractions(jdbcTemplate);
    }
}
