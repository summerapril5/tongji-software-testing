package com.github.JLQusername.account.domain;

import lombok.Data;

@Data
public abstract class User {
    protected String phoneNumber;
    protected String password;
}
