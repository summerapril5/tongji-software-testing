package com.github.JLQusername.settle.controller;

import com.github.JLQusername.api.OurSystem;
import com.github.JLQusername.api.client.SettleClient;
import com.github.JLQusername.settle.service.SettleService;
import lombok.RequiredArgsConstructor;
import com.github.JLQusername.common.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@RestController
@RequestMapping("/settle")
@RequiredArgsConstructor
public class SettleController {
    private final SettleService settleService;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private SettleClient settleClient;

    @PostMapping("/system")
    public OurSystem getSystem() {
        return settleService.getSystem();
    }
    @PostMapping("/init")
    public Result initializeDay() {
        return settleService.initializeDay() ? Result.success() : Result.error("日初始化失败");
    }

    @GetMapping
    public Result getFrontSystem() {
        return Result.success(settleService.getSystem());
    }

    @PostMapping("/market")
    public Result receiveMarketData() {
        return settleService.receiveMarketData() ? Result.success() : Result.error("接收行情失败");
    }

    @PostMapping("/subscription")
    public Result confirmSubscriptions() {
        return settleService.confirmSubscriptions() ? Result.success() : Result.error("申购确认失败");
    }

    @PostMapping("/redemption")
    public Result confirmRedemptions() {
        return settleService.confirmRedemptions() ? Result.success() : Result.error("赎回确认失败");
    }

    @PostMapping("/stop")
    public Result stopDailyApplications() {
        return settleService.stopDailyApplications() ? Result.success() : Result.error("停止申请失败");
    }

    @PostMapping("/export")
    public Result exportData() {
        return settleService.exportData() ? Result.success() : Result.error("数据导出失败");
    }

    @GetMapping("/system/transaction-date")
    public Result getTransactionDate() {
        try {
            // 获取 OurSystem 对象
            OurSystem ourSystem = settleService.getNetValueSystem();
            String rawDateStr = ourSystem.getTransactionDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(formatter);
            return Result.success(rawDateStr);
        } catch (DateTimeParseException e) {
            return Result.error("Failed to parse date: " + e.getMessage());
        } catch (Exception e) {
            return Result.error("An error occurred while fetching the transaction date: " + e.getMessage());
        }

    }
}
