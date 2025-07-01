package com.github.JLQusername.account.service;

import com.github.JLQusername.account.domain.dto.LoginDTO;

public interface LoginService {
    String checkPassword(LoginDTO loginDTO);

    boolean isVaildPhoneNumber(String phoneNumber);

    boolean isVaildAdminPhoneNumber(String phoneNumber);

    boolean changePassword(LoginDTO loginDTO);
}
