package com.github.JLQusername.account.domain.dto;

import lombok.Data;

@Data
public class CustomerDTO {
    private String name;
    private String phoneNumber;
    private String password;
    private String idNumber;
    private int riskLevel;
}
