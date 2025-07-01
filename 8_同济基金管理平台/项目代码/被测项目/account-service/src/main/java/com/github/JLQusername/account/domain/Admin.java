package com.github.JLQusername.account.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Admin extends User{
    @TableId(type = IdType.ASSIGN_ID)
    private Long adminAccount;
}
