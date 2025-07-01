package com.github.JLQusername.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.JLQusername.api.NetValue;
import com.github.JLQusername.product.domain.Product;
import java.util.Date;
import java.util.List;



public interface ProductService extends IService<Product> {
    List<Product> list();
    List<Product> searchByKeyword(String keyword);

    Double getNetValueByProductIdAndDate(int productId, Date date);

    boolean saveProduct(Product product);

    void updateProduct(Product product);
    List<NetValue> getLatestNetValues();
    boolean insertNetValue(NetValue netValue);

}
