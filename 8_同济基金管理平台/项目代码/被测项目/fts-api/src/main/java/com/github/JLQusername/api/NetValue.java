package com.github.JLQusername.api;

import lombok.Data;
import java.util.Date;

@Data
public class NetValue {
    private Long id;
    private Integer productId;
    private Double netValue;
    private Date date;
}
