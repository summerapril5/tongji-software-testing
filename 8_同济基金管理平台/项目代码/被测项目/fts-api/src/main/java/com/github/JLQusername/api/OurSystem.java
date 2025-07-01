package com.github.JLQusername.api;

import lombok.Data;

import java.util.Date;

@Data
public class OurSystem {
    private Date transactionDate;
    private boolean hasStoppedApplication;
    private boolean hasExportedApplicationData;
    private boolean hasReceivedMarketData;
}
