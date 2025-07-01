package com.github.JLQusername.account.domain.vo;

import com.github.JLQusername.account.domain.TradingAccount;
import lombok.Data;

@Data
public class BankcardVO {
    private Long tradingAccount;
    private String bankcardNumber;

    public BankcardVO(TradingAccount tradingAccount) {
        this.tradingAccount = tradingAccount.getTradingAccountId();
        this.bankcardNumber = tradingAccount.getBankcardNumber();
    }
}
