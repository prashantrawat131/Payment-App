package com.example.mobilepaymentapp.model;

public class Transaction {
    String id;
    String senderNumber;
    String receiverNumber;
    int amount;
    long time;

    public Transaction() {
    }

    public Transaction(String id, String senderNumber, String receiverNumber, int amount, long time) {
        this.id = id;
        this.senderNumber = senderNumber;
        this.receiverNumber = receiverNumber;
        this.amount = amount;
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderNumber() {
        return senderNumber;
    }

    public void setSenderNumber(String senderNumber) {
        this.senderNumber = senderNumber;
    }

    public String getReceiverNumber() {
        return receiverNumber;
    }

    public void setReceiverNumber(String receiverNumber) {
        this.receiverNumber = receiverNumber;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
