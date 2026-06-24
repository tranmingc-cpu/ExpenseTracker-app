package com.expensetracker_manager.model.request;

public class PaymentLinkRequest {
    private String phoneNumber;
    private String bankId;
    private String accountNumber;
    private Double amount;
    private String note;

    public PaymentLinkRequest() {
    }

    public PaymentLinkRequest(String phoneNumber, String bankId, String accountNumber, Double amount, String note) {
        this.phoneNumber = phoneNumber;
        this.bankId = bankId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.note = note;
    }

    public PaymentLinkRequest(String phoneNumber, String bankId, String accountNumber, String amount, String note) {
        this.phoneNumber = phoneNumber;
        this.bankId = bankId;
        this.accountNumber = accountNumber;
        try {
            this.amount = Double.parseDouble(amount);
        } catch (Exception e) {
            this.amount = 0.0;
        }
        this.note = note;
    }

    public PaymentLinkRequest(String phoneNumber, String bankId, String accountNumber, int amount, String note) {
        this.phoneNumber = phoneNumber;
        this.bankId = bankId;
        this.accountNumber = accountNumber;
        this.amount = (double) amount;
        this.note = note;
    }

    public PaymentLinkRequest(String phoneNumber, String bankId, String accountNumber, long amount, String note) {
        this.phoneNumber = phoneNumber;
        this.bankId = bankId;
        this.accountNumber = accountNumber;
        this.amount = (double) amount;
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

    public Double getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }
}
