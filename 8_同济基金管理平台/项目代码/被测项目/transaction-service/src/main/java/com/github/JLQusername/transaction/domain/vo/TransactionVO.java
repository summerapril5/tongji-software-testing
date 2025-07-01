package com.github.JLQusername.transaction.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class TransactionVO {
    private String transactionId;
    private String tradingAccountId;
    private String fundAccount;
    private int productId;
    private String productName;
    private String applicationTime;
    private boolean isCancel;
    private boolean canCancel;
    private boolean isSubscribe;
    private double amount;
    private double shares;
}
