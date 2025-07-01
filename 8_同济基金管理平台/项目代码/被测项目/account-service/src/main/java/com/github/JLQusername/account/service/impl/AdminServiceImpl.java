package com.github.JLQusername.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.JLQusername.account.domain.Admin;
import com.github.JLQusername.account.mapper.AdminMapper;
import com.github.JLQusername.account.service.AdminService;
import com.github.JLQusername.common.utils.Md5Util;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin>  implements AdminService  {
    @Override
    public boolean createAdmin(Admin admin) {
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone_number", admin.getPhoneNumber());
        if (count(queryWrapper) > 0)
            return false;
        admin.setPassword(Md5Util.getMD5String(admin.getPassword()));
        return save(admin);
    }
}
