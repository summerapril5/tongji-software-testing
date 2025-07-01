package com.github.JLQusername.product.controller;

import com.github.JLQusername.api.OurSystem;
import com.github.JLQusername.common.domain.Result;
import com.github.JLQusername.api.NetValue;
import com.github.JLQusername.product.service.ProductService;
import com.github.JLQusername.api.client.SettleClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.github.JLQusername.product.domain.Product;

import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @Autowired
    private SettleClient settleClient;

    @PostMapping("/level")
    public int getLevel(Integer productId) {
        return productService.getById(productId).getRiskLevel();
    }

    /*获取所有产品列表*/
    @GetMapping("/list")
    public Result getAllProducts() {
        return Result.success(productService.list());
    }

    //传入keyword获取搜索后的产品列表
    @GetMapping("/search/{keyword}")
    public Result searchProducts(@PathVariable String keyword) {
        List<Product> products = productService.searchByKeyword(keyword);
        return Result.success(products);
    }

    //传入productId和date获取产品净值
    @GetMapping("/netvalue/{productId}/{date}")
    public Result getNetValueByProductIdAndDate(
            @PathVariable int productId,
            @PathVariable String date) {
            // 将日期字符串转换为Date对象，假设格式为yyyy-MM-dd
            java.util.Date parsedDate = java.sql.Date.valueOf(date);
            Double netValue = productService.getNetValueByProductIdAndDate(productId, parsedDate);
            if (netValue == null) {
                return Result.error("No net value found for the given product and date.");
            }
            return Result.success(netValue);
    }

    @PostMapping("/add")
    public Result addProduct(@RequestBody Product product) {
        boolean res =  productService.saveProduct(product);
        return res ? Result.success() : Result.error("添加产品失败");
    }

    @PutMapping("/{productId}")
    public Result updateProduct(@PathVariable Integer productId, @RequestBody Product product) {
        product.setProductId(productId);
        productService.updateProduct(product);
        return Result.success(product);
    }

    @PostMapping("/net_values")
    public List<NetValue> getLatestNetValues() {
        return productService.getLatestNetValues();
    }

    @PostMapping("/net_value/insert")
    public boolean insertNetValue(@RequestBody NetValue netValue){
        return productService.insertNetValue(netValue);

    }
    @PostMapping("/net_value/{productId}/{date}")
    public Double getNetValue(@PathVariable("productId") Integer productId,
                       @PathVariable("date") Date date){
        return productService.getNetValueByProductIdAndDate(productId,date);
    };

}
