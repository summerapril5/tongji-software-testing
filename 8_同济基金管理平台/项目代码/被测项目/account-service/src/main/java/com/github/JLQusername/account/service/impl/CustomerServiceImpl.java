package com.github.JLQusername.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.JLQusername.account.domain.Customer;
import com.github.JLQusername.account.domain.dto.CustomerDTO;
import com.github.JLQusername.account.domain.dto.UpdateInfoDTO;
import com.github.JLQusername.account.domain.vo.CustomerVO;
import com.github.JLQusername.account.mapper.CustomerMapper;
import com.github.JLQusername.account.service.CustomerService;
import com.github.JLQusername.common.utils.Md5Util;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    @Autowired
    private CustomerMapper customerMapper;

    @Override
    public Long createAccount(CustomerDTO customerDTO) {
        //查询phoneNumber或idNumber是否已在数据库存在
        QueryWrapper<Customer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone_number", customerDTO.getPhoneNumber())
                .or().eq("id_number", customerDTO.getIdNumber());
        if (count(queryWrapper) > 0)
            return 0L;
        customerDTO.setPassword(Md5Util.getMD5String(customerDTO.getPassword()));
        Customer customer = new Customer();
        BeanUtils.copyProperties(customerDTO, customer);
        save(customer);
        return customer.getFundAccount();
    }

    @Override
    public boolean updateRiskLevel(Long fundAccount, int riskLevel) {
        UpdateWrapper<Customer> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("fund_account", fundAccount)
                .set("risk_level", riskLevel);
        return update(updateWrapper);
    }

    @Override
    public boolean updateInfo(UpdateInfoDTO updateInfoDTO) {
        //查询phoneNumber或idNumber是否已在数据库存在
        QueryWrapper<Customer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone_number", updateInfoDTO.getPhoneNumber());
        if (count(queryWrapper) > 0)
            return false;
        UpdateWrapper<Customer> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("fund_account", updateInfoDTO.getFundAccount())
                .set("phone_number", updateInfoDTO.getPhoneNumber())
                .set("risk_level", updateInfoDTO.getRiskLevel());
        return update(updateWrapper);
    }

    @Override
    public List<CustomerVO> getCustomers(int pageNum, int pageSize,String key) {
        Page<Customer> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Customer> queryWrapper = null;
        if (key != null && !key.isEmpty() && !key.isBlank()) {
            queryWrapper = new QueryWrapper<>();
            queryWrapper.like("name", key);
        }
        IPage<Customer> customerPage = customerMapper.selectPage(page, queryWrapper);
        return customerPage.getRecords().stream().map(CustomerVO::new).collect(Collectors.toList());
    }

    @NotNull
    private static UpdateWrapper<Customer> getCustomerUpdateWrapper(Customer customer) {
        UpdateWrapper<Customer> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("fund_account", customer.getFundAccount());
        if (customer.getPhoneNumber() != null)
            updateWrapper.set("phone_number", customer.getPhoneNumber());
        if (customer.getIdNumber() != null)
            updateWrapper.set("id_number", customer.getIdNumber());
        if (customer.getName() != null)
            updateWrapper.set("name", customer.getName());
        if (customer.getPassword() != null)
            updateWrapper.set("password", customer.getPassword());
        return updateWrapper;
    }

}
