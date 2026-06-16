package com.expensetracker_manager.model.request;

import com.google.gson.annotations.SerializedName;

public class CategoryRequest {

    @SerializedName("name")
    private String name;

    @SerializedName("type")
    private String type;

    @SerializedName("icon")
    private String icon;

    @SerializedName("color")
    private String color;

    @SerializedName("userId")
    private long userId;

    public CategoryRequest() {}

    public CategoryRequest(String name, String type, String icon, String color, long userId) {
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.color = color;
        this.userId = userId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
}
