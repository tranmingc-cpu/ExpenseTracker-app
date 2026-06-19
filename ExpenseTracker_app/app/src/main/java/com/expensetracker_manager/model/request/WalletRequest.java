package com.expensetracker_manager.model.request;

import com.google.gson.annotations.SerializedName;

public class WalletRequest {

    @SerializedName("name")
    private String name;

    @SerializedName("balance")
    private double balance;

    @SerializedName("userId")
    private long userId;

    @SerializedName("description")
    private String description;

    @SerializedName("type")
    private String type;

    public WalletRequest() {}

    public WalletRequest(String name, double balance, long userId, String description, String type) {
        this.name = name;
        this.balance = balance;
        this.userId = userId;
        this.description = description;
        this.type = type;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
