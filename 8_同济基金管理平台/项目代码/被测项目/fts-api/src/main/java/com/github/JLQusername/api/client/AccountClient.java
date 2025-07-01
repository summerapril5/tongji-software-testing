package com.github.JLQusername.api.client;

import com.github.JLQusername.api.Bankcard;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient("account-service")
public interface AccountClient {
    @PostMapping("/account/bankcard")
    Bankcard getBankcard(@RequestParam long tradingAccountId);

    @PatchMapping("/account/balance")
    boolean updateBalance(@RequestBody Bankcard bankcard);

//    @GetMapping("/account/balance")
//    double getBalance(@RequestParam String bankcardNumber);
}
