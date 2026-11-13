package com.royal.reserve.bank.transaction.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Data Transfer Object (DTO) class that represents the response payload for a transaction.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionItemsDto {
    private Long id;
    @NotBlank
    private String assetCode;
    private String assetName;
    @Positive
    private int value;
}
