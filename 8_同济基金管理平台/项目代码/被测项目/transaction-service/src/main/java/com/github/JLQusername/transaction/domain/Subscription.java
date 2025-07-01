package com.github.JLQusername.transaction.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class Subscription extends Transaction{
    private double subscriptionAmount;

    public Subscription(Long transactionId, double subscriptionAmount,long tradingAccountId,long fundAccount,
                        int productId, String productName, Date applicationTime, boolean isCancel) {
        super(transactionId, tradingAccountId, fundAccount, productId, productName, applicationTime, isCancel);
        this.subscriptionAmount = subscriptionAmount;
    }

    public Subscription() {

    }

    public void setId(long l) {

    }
}
