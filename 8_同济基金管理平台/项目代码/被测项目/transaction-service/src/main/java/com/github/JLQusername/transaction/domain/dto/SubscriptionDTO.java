package com.github.JLQusername.transaction.domain.dto;

import lombok.Data;

@Data
public class SubscriptionDTO {
    private String tradingAccountId;
    private long fundAccount;
    private int productId;
    private String productName;
    private double amount;
}
