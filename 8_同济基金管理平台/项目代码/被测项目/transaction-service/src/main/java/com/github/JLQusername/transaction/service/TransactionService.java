package com.github.JLQusername.transaction.service;

import com.github.JLQusername.transaction.domain.Holding;
import com.github.JLQusername.transaction.domain.dto.RedemptionDTO;
import com.github.JLQusername.transaction.domain.dto.SubscriptionDTO;
import com.github.JLQusername.api.bo.SubscriptionBO;
import com.github.JLQusername.api.bo.RedemptionBO;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface TransactionService {
    long submitSubscription(SubscriptionDTO subscriptionDTO);
    Holding getHolding(long tradingAccountId, int productId);
    long submitRedemption(RedemptionDTO redemptionDTO);
    boolean cancelTransaction(long transactionId);
    List<SubscriptionBO> getValidSubscriptionBOs(Date date);
    List<RedemptionBO> getValidRedemptionBOs(Date date);
    boolean confirmSubscriptionBatch(Map<Long, Double> transactionIdToShares);
    boolean confirmRedemptionBatch(Map<Long, Double> transactionIdToAmount);
}
