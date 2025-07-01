package com.github.JLQusername.account.domain.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String phoneNumber;
    private String password;
    private int userType; // 1: customer, 2: admin
}
