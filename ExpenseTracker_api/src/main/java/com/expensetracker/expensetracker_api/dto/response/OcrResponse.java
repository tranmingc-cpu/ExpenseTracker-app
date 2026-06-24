package com.expensetracker.expensetracker_api.dto.response;

public class OcrResponse {
    private String amount;
    private String description;
    private String transactionTime;
    private String accountName;
    private String accountNumber;
    private String bankName;

    public OcrResponse(String amount, String description, String transactionTime, String accountName, String accountNumber, String bankName) {
        this.amount = amount;
        this.description = description;
        this.transactionTime = transactionTime;
        this.accountName = accountName;
        this.accountNumber = accountNumber;
        this.bankName = bankName;
    }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTransactionTime() { return transactionTime; }
    public void setTransactionTime(String transactionTime) { this.transactionTime = transactionTime; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
}
