package com.github.JLQusername.api.bo;

import lombok.Data;

@Data
public class BankcardBO {
    private long tradingAccountId;
    private String bankcardNumber;
    private double balance;
}
