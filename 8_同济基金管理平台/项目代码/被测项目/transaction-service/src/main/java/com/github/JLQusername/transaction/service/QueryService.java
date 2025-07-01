package com.github.JLQusername.transaction.service;

import com.github.JLQusername.transaction.domain.vo.TransactionVO;

import java.util.List;

public interface QueryService {
    List<TransactionVO> getTransactions(Long fundAccount);
}