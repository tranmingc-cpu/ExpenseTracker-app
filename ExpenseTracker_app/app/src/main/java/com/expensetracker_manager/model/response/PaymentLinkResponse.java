package com.expensetracker_manager.model.response;

public class PaymentLinkResponse {
    private String momoDeeplink;
    private String vietQrUrl;

    public PaymentLinkResponse() {}

    public PaymentLinkResponse(String momoDeeplink, String vietQrUrl) {
        this.momoDeeplink = momoDeeplink;
        this.vietQrUrl = vietQrUrl;
    }

    public String getMomoDeeplink() { return momoDeeplink; }
    public void setMomoDeeplink(String momoDeeplink) { this.momoDeeplink = momoDeeplink; }

    public String getVietQrUrl() { return vietQrUrl; }
    public void setVietQrUrl(String vietQrUrl) { this.vietQrUrl = vietQrUrl; }
}
