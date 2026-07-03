package com.expensetracker_manager.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class RecurringTransactionResponse {

    @SerializedName("id")
    private Long id;

    @SerializedName("amount")
    private BigDecimal amount;

    @SerializedName("description")
    private String description;

    @SerializedName("type")
    private String type;

    @SerializedName("frequency")
    private String frequency;

    @SerializedName("nextExecutionDate")
    private String nextExecutionDate;

    @SerializedName("lastPaidYear")
    private Integer lastPaidYear;

    @SerializedName("lastPaidMonth")
    private Integer lastPaidMonth;

    @SerializedName("lastPaidAt")
    private String lastPaidAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getNextExecutionDate() {
        return nextExecutionDate;
    }

    public void setNextExecutionDate(String nextExecutionDate) {
        this.nextExecutionDate = nextExecutionDate;
    }

    public Integer getLastPaidYear() {
        return lastPaidYear;
    }

    public void setLastPaidYear(Integer lastPaidYear) {
        this.lastPaidYear = lastPaidYear;
    }

    public Integer getLastPaidMonth() {
        return lastPaidMonth;
    }

    public void setLastPaidMonth(Integer lastPaidMonth) {
        this.lastPaidMonth = lastPaidMonth;
    }

    public String getLastPaidAt() {
        return lastPaidAt;
    }

    public void setLastPaidAt(String lastPaidAt) {
        this.lastPaidAt = lastPaidAt;
    }
}
