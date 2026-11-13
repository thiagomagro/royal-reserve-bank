package com.royal.reserve.bank.transaction.api.repository;

import com.royal.reserve.bank.transaction.api.model.TransactionItems;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@RequiredArgsConstructor
public class TransactionSearchRepositoryImpl implements TransactionSearchRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<TransactionItems> findItemsByAssetCode(String assetCode) {
        String sql = "SELECT id, asset_code, asset_name, value FROM t_transaction_items WHERE asset_code = '"
                + assetCode + "'";
        return jdbcTemplate.query(sql, (row, index) -> new TransactionItems(
                row.getLong("id"), row.getString("asset_code"), row.getString("asset_name"), row.getInt("value")));
    }
}
