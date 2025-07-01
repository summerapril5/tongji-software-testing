package com.github.JLQusername.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.JLQusername.product.domain.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.Date;


@Mapper
public interface ProductMapper extends  BaseMapper<Product> {


    @Select("SELECT net_value FROM net_value WHERE product_id = #{productId} AND date = #{date}")
    Double getNetValueByProductIdAndDate(@Param("productId") int productId, @Param("date") Date date);
}
