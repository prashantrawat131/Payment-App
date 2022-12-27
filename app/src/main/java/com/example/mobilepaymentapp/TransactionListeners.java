package com.example.mobilepaymentapp;

public interface TransactionListeners {
    void insufficientAmount();

    void error(String message);

    void success();
}
