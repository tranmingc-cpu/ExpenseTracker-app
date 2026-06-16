package com.expensetracker_manager.model.response;

import com.google.gson.annotations.SerializedName;

public class WalletResponse {

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("type")
    private String type;

    @SerializedName("balance")
    private double balance;

    @SerializedName("description")
    private String description;

    @SerializedName("userId")
    private long userId;

    @SerializedName("userName")
    private String userName;

    public WalletResponse() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}
