package com.github.JLQusername.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.JLQusername.api.Bankcard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BankcardMapper extends BaseMapper<Bankcard> {

    @Select("SELECT b.bankcard_number, b.balance " +
            "FROM bankcard b " +
            "INNER JOIN trading_account ta ON b.bankcard_number = ta.bankcard_number " +
            "WHERE ta.trading_account_id = #{tradingAccountId} AND ta.is_deleted = 0")
    Bankcard getBankcardByTradingAccountId(long tradingAccountId);

}
