package com.expensetracker_manager.model.request;

public class PaymentLinkRequest {
    private String phoneNumber;
    private String bankId;
    private String accountNumber;
    private Double amount;
    private String note;

    public PaymentLinkRequest() {}

    public PaymentLinkRequest(String phoneNumber, String bankId, String accountNumber, Double amount, String note) {
    private Object amount;
    private String note;

    public PaymentLinkRequest(String phoneNumber, String bankId, String accountNumber, String amount, String note) {
        this.phoneNumber = phoneNumber;
        this.bankId = bankId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.note = note;
    }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getBankId() { return bankId; }
    public void setBankId(String bankId) { this.bankId = bankId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
    public PaymentLinkRequest(String phoneNumber, String bankId, String accountNumber, int amount, String note) {
        this.phoneNumber = phoneNumber;
        this.bankId = bankId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.note = note;
    }

