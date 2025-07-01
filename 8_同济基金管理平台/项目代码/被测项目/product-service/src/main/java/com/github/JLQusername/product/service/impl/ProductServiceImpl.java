package com.github.JLQusername.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.JLQusername.api.OurSystem;
import com.github.JLQusername.api.client.SettleClient;
import com.github.JLQusername.api.NetValue;
import com.github.JLQusername.common.domain.Result;
import com.github.JLQusername.product.domain.Product;
import com.github.JLQusername.product.mapper.NetValueMapper;
import com.github.JLQusername.product.mapper.ProductMapper;
import com.github.JLQusername.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;



@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {



    @Autowired
    private SettleClient settleClient;

    private final NetValueMapper netValueMapper;
    @Override
    public List<Product> list() {
        return super.list(null); // 使用 MyBatis Plus 提供的默认 list 方法，null 表示没有查询条件
    }

    @Override
    public List<Product> searchByKeyword(String keyword) {
        // 使用 MyBatis Plus 的 QueryWrapper 构建查询条件
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("product_name", keyword).or().like("product_status", keyword); // 假设 Product 表中有 name 和 description 字段
        return list(queryWrapper);
    }

    @Override
    public Double getNetValueByProductIdAndDate(int productId, Date date) {
        QueryWrapper<NetValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId)
                .apply("DATE(date) = DATE({0})", date);

        NetValue netValue = netValueMapper.selectOne(queryWrapper);
        return netValue != null ? netValue.getNetValue() : null;
    }

//    @Override
//    public boolean saveProduct(Product product) {
//        save(product);
//        // 获取 OurSystem 对象
//        OurSystem ourSystem = settleClient.getSystem();
//        // 创建 NetValue 对象并设置属性
//        NetValue netValue = new NetValue();
//        netValue.setProductId(product.getProductId());
//        netValue.setDate(ourSystem.getTransactionDate()); // 假设 setDate 接受 LocalDate 类型
//        netValue.setNetValue(1.0); // 默认净值为1
//
//        try {
//            // 保存 NetValue 对象到数据库
//            return netValueMapper.insert(netValue) > 0;
//        } catch (Exception e) {
//            // 处理可能发生的异常（例如数据库操作失败）
//            e.printStackTrace();
//            return false;
//        }
//    }
@Override
public boolean saveProduct(Product product) {
    // 检查产品名称是否为空，保存失败
    if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
        return false;
    }

    // 保存产品
    boolean saveSuccess = save(product);
    if (!saveSuccess) {
        return false;
    }

    // 获取交易日
    OurSystem ourSystem = settleClient.getSystem();
    Date transactionDate = ourSystem.getTransactionDate();

    // 如果交易日为空，不能继续保存净值
    if (transactionDate == null) {
        return false;
    }

    // 创建并保存净值
    NetValue netValue = new NetValue();
    netValue.setProductId(product.getProductId());
    netValue.setDate(transactionDate);
    netValue.setNetValue(1.0);

    try {
        return netValueMapper.insert(netValue) > 0;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}




//        @Override
//    public void updateProduct(Product product) {
//        baseMapper.updateById(product);
//    }
@Override
public void updateProduct(Product product) {
    if (product.getProductId() == null) {
        return;
    }
    baseMapper.updateById(product);
}


    @Override
    public List<NetValue> getLatestNetValues() {
        QueryWrapper<NetValue> maxDateQuery = new QueryWrapper<>();
        maxDateQuery.orderByDesc("date").last("limit 1");
        Date maxDate = netValueMapper.selectOne(maxDateQuery).getDate();

        if (maxDate != null) {
            QueryWrapper<NetValue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("date", maxDate);
            return netValueMapper.selectList(queryWrapper);
        }
        log.error("No latest net values found.");
        return new ArrayList<>();

    }
    @Override
    public boolean insertNetValue(NetValue netValue) {
        return netValueMapper.insert(netValue) > 0;
    }
}
