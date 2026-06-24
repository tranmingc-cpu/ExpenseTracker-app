package com.expensetracker_manager.model.response;

public class OcrResponse {
    private String amount;
    private String description;
    private String transactionTime;
    private String accountName;
    private String accountNumber;
    private String bankName;

    public String getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getTransactionTime() { return transactionTime; }
    public String getAccountName() { return accountName; }
    public String getAccountNumber() { return accountNumber; }
    public String getBankName(){return bankName;}

}
