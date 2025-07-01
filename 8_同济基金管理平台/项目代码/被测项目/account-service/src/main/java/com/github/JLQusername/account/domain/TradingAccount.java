package com.github.JLQusername.account.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class TradingAccount {
    @TableId(type = IdType.ASSIGN_ID)
    private Long TradingAccountId;
    private long fundAccount;
    private String bankcardNumber;
    private boolean isDeleted;
}
