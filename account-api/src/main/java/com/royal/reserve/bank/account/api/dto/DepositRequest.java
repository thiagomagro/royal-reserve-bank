package com.royal.reserve.bank.account.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request body for adding funds to an existing bank account.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepositRequest {
    private BigDecimal amount;
}
