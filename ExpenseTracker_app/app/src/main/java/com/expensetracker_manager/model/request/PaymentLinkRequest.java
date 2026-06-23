package com.expensetracker_manager.model.request;

public class PaymentLinkRequest {
    private String phoneNumber;
    private String bankId;
    private String accountNumber;
    private Object amount;
    private String note;

    public PaymentLinkRequest(String phoneNumber, String bankId, String accountNumber, String amount, String note) {
        this.phoneNumber = phoneNumber;
        this.bankId = bankId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.note = note;
    }

    public PaymentLinkRequest(String phoneNumber, String bankId, String accountNumber, double amount, String note) {
        this.phoneNumber = phoneNumber;
        this.bankId = bankId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.note = note;
    }

    public PaymentLinkRequest(String phoneNumber, String bankId, String accountNumber, int amount, String note) {
        this.phoneNumber = phoneNumber;
        this.bankId = bankId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.note = note;
    }

    public PaymentLinkRequest(String phoneNumber, String bankId, String accountNumber, long amount, String note) {
        this.phoneNumber = phoneNumber;
        this.bankId = bankId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.note = note;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getBankId() {
        return bankId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Object getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }
}