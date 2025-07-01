package com.github.JLQusername.transaction.domain.dto;

import lombok.Data;

@Data
public class RedemptionDTO {
    private String tradingAccountId;
    private long fundAccount;
    private int productId;
    private String productName;
    private double shares;
}
