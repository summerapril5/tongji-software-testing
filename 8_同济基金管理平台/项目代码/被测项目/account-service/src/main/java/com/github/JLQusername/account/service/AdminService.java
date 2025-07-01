package com.github.JLQusername.account.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.JLQusername.account.domain.Admin;
import com.github.JLQusername.account.domain.User;

public interface AdminService extends IService<Admin> {
    boolean createAdmin(Admin admin);
}
