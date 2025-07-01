package com.github.JLQusername.settle.service;

import com.github.JLQusername.api.OurSystem;

public interface SettleService {
    OurSystem getSystem();
    boolean initializeDay();
    boolean receiveMarketData();
    boolean confirmSubscriptions();
    boolean confirmRedemptions();
    boolean stopDailyApplications();
    boolean exportData();

    OurSystem getNetValueSystem();
}
