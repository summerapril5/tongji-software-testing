package com.github.JLQusername.transaction.controller;

import com.github.JLQusername.common.domain.Result;
import com.github.JLQusername.transaction.domain.dto.RedemptionDTO;
import com.github.JLQusername.transaction.domain.dto.SubscriptionDTO;
import com.github.JLQusername.transaction.service.TransactionService;
import com.github.JLQusername.api.bo.SubscriptionBO;
import com.github.JLQusername.api.bo.RedemptionBO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/subscription")
    public Result submitSubscription(@RequestBody SubscriptionDTO subscriptionDTO) {
        long result = transactionService.submitSubscription(subscriptionDTO);
//        if(result == 0)
//            return Result.error("风险等级不匹配");
//        else if(result == 1)
        if(result == 1L)
            return Result.error("余额不足");
        return Result.success(result);
    }

    @PostMapping("/redemption")
    public Result submitRedemption(@RequestBody RedemptionDTO redemptionDTO) {
        long result = transactionService.submitRedemption(redemptionDTO);
        if(result == 1L)
            return Result.error("份额不足");
        return Result.success(result);
    }

    @PostMapping("/cancel")
    public Result cancelTransaction(@RequestParam String transactionId) {
        return transactionService.cancelTransaction(Long.parseLong(transactionId)) ? Result.success() : Result.error("当前交易已超时");
    }

    @PostMapping("/subscriptions/valid")
    public List<SubscriptionBO> getValidSubscriptionBOs(@RequestParam Date date) {
        return transactionService.getValidSubscriptionBOs(date);
    }

    @PostMapping("/redemptions/valid")
    public List<RedemptionBO> getValidRedemptionBOs(@RequestParam Date date) {
        return transactionService.getValidRedemptionBOs(date);
    }
    //申购确认（修改holding)的方法，根据trading_account和product_id去holding插入或增加份额
    @PostMapping("/subscription/confirm-batch")
    public boolean confirmSubscriptionBatch(@RequestBody Map<Long, Double> transactionIdToShares) {
        return transactionService.confirmSubscriptionBatch(transactionIdToShares);
    }
    //处理赎回确认（修改bankcard)的方法，根据trading_account找到bankcard_number，去bankcard修改余额
    @PostMapping("/redemption/confirm-batch")
    public boolean confirmRedemptionBatch(@RequestBody Map<Long, Double> transactionIdToAmount) {
        return transactionService.confirmRedemptionBatch(transactionIdToAmount);
    }

}
