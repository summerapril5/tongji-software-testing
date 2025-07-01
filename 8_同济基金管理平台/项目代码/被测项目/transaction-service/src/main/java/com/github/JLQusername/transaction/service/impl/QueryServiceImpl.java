package com.github.JLQusername.transaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.JLQusername.api.OurSystem;
import com.github.JLQusername.api.client.SettleClient;
import com.github.JLQusername.transaction.domain.Redemption;
import com.github.JLQusername.transaction.domain.Subscription;
import com.github.JLQusername.transaction.domain.vo.TransactionVO;
import com.github.JLQusername.transaction.mapper.RedemptionMapper;
import com.github.JLQusername.transaction.mapper.SubscriptionMapper;
import com.github.JLQusername.transaction.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    private final SubscriptionMapper subscriptionMapper;
    private final RedemptionMapper redemptionMapper;
    private final SettleClient settleClient;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<TransactionVO> getTransactions(Long fundAccount) {
        List<Subscription> subscriptions = subscriptionMapper.selectList(new QueryWrapper<Subscription>().eq("fund_account", fundAccount));
        List<Redemption> redemptions = redemptionMapper.selectList(new QueryWrapper<Redemption>().eq("fund_account", fundAccount));
        OurSystem system = settleClient.getSystem();
        List<TransactionVO> transactions = subscriptions.stream().map(
                subscription -> new TransactionVO(subscription.getTransactionId().toString(), subscription.getTradingAccountId().toString(),
                        subscription.getFundAccount().toString(),subscription.getProductId(),subscription.getProductName(),
                        subscription.getApplicationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(formatter),
                        subscription.isCancel(),
                        !subscription.isCancel() && !system.isHasExportedApplicationData()
                                && system.getTransactionDate().getTime() - subscription.getApplicationTime().getTime() < 1000 * 60 * 60 * 24,
                        true,subscription.getSubscriptionAmount(),0)).collect(Collectors.toList());
        transactions.addAll(redemptions.stream().map(
                redemption -> new TransactionVO(redemption.getTransactionId().toString(), redemption.getTradingAccountId().toString(),
                        redemption.getFundAccount().toString(),redemption.getProductId(),redemption.getProductName(),
                        redemption.getApplicationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(formatter),
                        redemption.isCancel(),
                        !redemption.isCancel() && !system.isHasExportedApplicationData()
                                && system.getTransactionDate().getTime() - redemption.getApplicationTime().getTime() < 1000 * 60 * 60 * 24,
                        false,0,redemption.getRedemptionShares())).collect(Collectors.toList()));
        //根据时间排序
        transactions.sort(Comparator.comparing(TransactionVO::getApplicationTime));
        return transactions;
    }
}
