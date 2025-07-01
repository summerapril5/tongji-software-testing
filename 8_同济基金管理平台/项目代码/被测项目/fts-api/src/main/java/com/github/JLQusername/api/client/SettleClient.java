package com.github.JLQusername.api.client;

import com.github.JLQusername.api.OurSystem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient("settle-service")
public interface SettleClient {
    @PostMapping("/settle/system")
    OurSystem getSystem();
}
