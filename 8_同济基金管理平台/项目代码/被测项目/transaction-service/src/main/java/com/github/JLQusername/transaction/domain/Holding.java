package com.github.JLQusername.transaction.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Holding {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private int productId;
    private double shares;
    private long tradingAccountId;
}
