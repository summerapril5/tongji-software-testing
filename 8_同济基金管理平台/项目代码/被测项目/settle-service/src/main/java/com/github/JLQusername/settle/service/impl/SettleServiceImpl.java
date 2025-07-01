package com.github.JLQusername.settle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.JLQusername.api.OurSystem;
import com.github.JLQusername.api.NetValue;
import com.github.JLQusername.settle.mapper.SystemMapper;
import com.github.JLQusername.settle.service.SettleService;
import com.github.JLQusername.api.client.ProductClient;
import com.github.JLQusername.api.client.TransactionClient;
import com.github.JLQusername.api.bo.SubscriptionBO;
import com.github.JLQusername.api.bo.RedemptionBO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.List;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettleServiceImpl implements SettleService {
    private final SystemMapper systemMapper;
    private final ProductClient productClient;
    private final TransactionClient transactionClient;
    private final Random random = new Random();

    @Override
    public OurSystem getSystem() {
        QueryWrapper<OurSystem> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("transaction_date").last("limit 1");
        return systemMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean initializeDay() {
        OurSystem system = getSystem();
        // 检查前一交易日是否完成清算
        if (!system.isHasExportedApplicationData()) {
            log.error("前一交易日清算未完成");
            return false;
        }

        // 更新为下一个交易日
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(system.getTransactionDate());

        // 判断当前是否是周五
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            // 如果是周五，加3天到下周一
            calendar.add(Calendar.DATE, 3);
        } else {
            // 否则加1天
            calendar.add(Calendar.DATE, 1);
        }

        // 创建新的交易日记录，而不是更新旧记录
        OurSystem newSystem = new OurSystem();
        newSystem.setTransactionDate(calendar.getTime());
        newSystem.setHasStoppedApplication(false);
        newSystem.setHasExportedApplicationData(false);
        newSystem.setHasReceivedMarketData(false);

        return systemMapper.insert(newSystem) > 0;
    }
    @Override
    public boolean receiveMarketData() {
        OurSystem system = getSystem();
        if (system.isHasReceivedMarketData()) {
            log.error("今日已接收过行情数据");
            return false;
        }

        try {
            // 获取所有产品的前一日净值并生成新净值
            List<NetValue> netValues = productClient.getLatestNetValues();
            // 用随机的方式模拟从数据中心获取行情
            for (NetValue netValue : netValues) {
                // 生成90%-110%之间的随机数
                double ratio = 0.9 + random.nextDouble() * 0.2;
                double newValue = netValue.getNetValue() * ratio;

                // 产品行情"获取"成功
                NetValue newNetValue = new NetValue();
                newNetValue.setProductId(netValue.getProductId());
                newNetValue.setNetValue(newValue);
                newNetValue.setDate(system.getTransactionDate());
                productClient.insertNetValue(newNetValue);
            }

            // 更新系统状态
            QueryWrapper<OurSystem> updateWrapper = new QueryWrapper<>();
            updateWrapper.eq("transaction_date", system.getTransactionDate());
            system.setHasReceivedMarketData(true);
            System.out.println(system.getTransactionDate());
            return systemMapper.update(system, updateWrapper) > 0;
        } catch (Exception e) {
            log.error("接收行情数据失败", e);
            return false;
        }
    }
    private Date getPreviousTradeDate() {
        QueryWrapper<OurSystem> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("transaction_date").last("limit 1 offset 1");
        OurSystem previousSystem = systemMapper.selectOne(queryWrapper);
        return previousSystem != null ? previousSystem.getTransactionDate() : null;
    }

    @Override
    public boolean confirmSubscriptions() {
        OurSystem system = getSystem();
        if (!system.isHasReceivedMarketData()) {
            log.error("今日未接收行情数据");
            return false;
        }

        try {
            // 获取前一交易日的有效申购申请BO列表
            Date previousDate = getPreviousTradeDate();
            if (previousDate == null) {
                log.error("无法获取前一交易日期");
                return false;
            }
            
            List<SubscriptionBO> subscriptionBOs = transactionClient.getValidSubscriptionBOs(previousDate);
            Map<Long, Double> transactionIdToShares = new HashMap<>();
            
            // 计算每笔申购的份额
            for (SubscriptionBO bo : subscriptionBOs) {
                double netValue = productClient.getNetValue(bo.getProductId(), system.getTransactionDate());
                double shares = bo.getAmount() / netValue;
                transactionIdToShares.put(bo.getTransactionId(), shares);
            }
            
            // 批量确认申购
            return transactionClient.confirmSubscriptionBatch(transactionIdToShares);
        } catch (Exception e) {
            log.error("申购确认失败", e);
            return false;
        }
    }
    @Override
    public boolean confirmRedemptions() {
        OurSystem system = getSystem();
        if (!system.isHasReceivedMarketData()) {
            log.error("今日未接收行情数据");
            return false;
        }

        try {
            // 获取前一交易日的有效赎回申请BO列表
            Date previousDate = getPreviousTradeDate();
            if (previousDate == null) {
                log.error("无法获取前一交易日期");
                return false;
            }
            
            List<RedemptionBO> redemptionBOs = transactionClient.getValidRedemptionBOs(previousDate);
            Map<Long, Double> transactionIdToAmount = new HashMap<>();
            
            // 计算每笔赎回的金额
            for (RedemptionBO bo : redemptionBOs) {
                double netValue = productClient.getNetValue(bo.getProductId(), system.getTransactionDate());
                double amount = bo.getRedemptionShares() * netValue;
                System.out.println("111:::"+bo.getProductId());
                System.out.println("222:::"+system.getTransactionDate());
                System.out.println("333:::"+bo.getTransactionId());
                System.out.println("amount: "+amount);
                transactionIdToAmount.put(bo.getTransactionId(), amount);
            }
            // 批量确认赎回
            return transactionClient.confirmRedemptionBatch(transactionIdToAmount);
        } catch (Exception e) {
            log.error("赎回确认失败", e);
            return false;
        }
    }

    @Override
    public boolean stopDailyApplications() {
        OurSystem system = getSystem();
        QueryWrapper<OurSystem> updateWrapper = new QueryWrapper<>();
        updateWrapper.eq("transaction_date", system.getTransactionDate());
        system.setHasStoppedApplication(true);
        return systemMapper.update(system, updateWrapper) > 0;
    }
    @Override
    public boolean exportData() {
        OurSystem system = getSystem();
        if (!system.isHasStoppedApplication()) {
            log.error("尚未停止当日申请");
            return false;
        }
        try {
            QueryWrapper<OurSystem> updateWrapper = new QueryWrapper<>();
            updateWrapper.eq("transaction_date", system.getTransactionDate());
            system.setHasExportedApplicationData(true);
            return systemMapper.update(system, updateWrapper) > 0;
        } catch (Exception e) {
            log.error("数据导出失败", e);
            return false;
        }
    }

    @Override
    public OurSystem getNetValueSystem() {
        QueryWrapper<OurSystem> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("transaction_date").last("limit 1");
        OurSystem system = systemMapper.selectOne(queryWrapper);
        // 获取前一交易日
        system.setTransactionDate(getPreviousTradeDate());
        return system;
    }
}
