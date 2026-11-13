package com.royal.reserve.bank.transaction.api.repository;

import com.royal.reserve.bank.transaction.api.model.TransactionItems;

import java.util.List;

public interface TransactionSearchRepository {
    List<TransactionItems> findItemsByAssetCode(String assetCode);
}
