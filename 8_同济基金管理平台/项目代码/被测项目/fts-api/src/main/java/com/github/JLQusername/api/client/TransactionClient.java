package com.github.JLQusername.api.client;

import com.github.JLQusername.api.bo.SubscriptionBO;
import com.github.JLQusername.api.bo.RedemptionBO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@FeignClient("transaction-service")
public interface TransactionClient {
    @PostMapping("/transaction/subscriptions/valid")
    List<SubscriptionBO> getValidSubscriptionBOs(@RequestParam("date") Date date);
    
    @PostMapping("/transaction/redemptions/valid")
    List<RedemptionBO> getValidRedemptionBOs(@RequestParam("date") Date date);
    
    @PostMapping("/transaction/subscription/confirm-batch")
    boolean confirmSubscriptionBatch(@RequestBody Map<Long, Double> transactionIdToShares);
    
    @PostMapping("/transaction/redemption/confirm-batch")
    boolean confirmRedemptionBatch(@RequestBody Map<Long, Double> transactionIdToAmount);
} 