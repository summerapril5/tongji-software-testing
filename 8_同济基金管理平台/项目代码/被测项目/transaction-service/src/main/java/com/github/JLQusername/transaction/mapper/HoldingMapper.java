package com.github.JLQusername.transaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.JLQusername.transaction.domain.Holding;
import org.apache.ibatis.annotations.Mapper;
import org.reactivestreams.Publisher;

@Mapper
public interface HoldingMapper extends BaseMapper<Holding> {
    Iterable<? extends Publisher<?>> getHolding(long l, int i);
}
