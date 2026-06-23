package com.expensetracker_manager.model.response;

public class PaymentLinkResponse {
    private boolean success;
    private String message;
    private String paymentUrl;
    private String paymentLink;
    private String qrCode;
    private String qrData;
    private String link;
    private String url;
    private String momoDeeplink;
    private String vietQrUrl;

    public boolean isSuccess() {
        return success;
    }

    public boolean getSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getPaymentUrl() {
        if (paymentUrl != null) return paymentUrl;
        if (paymentLink != null) return paymentLink;
        if (link != null) return link;
        return url;
    }

    public String getPaymentLink() {
        if (paymentLink != null) return paymentLink;
        if (paymentUrl != null) return paymentUrl;
        if (link != null) return link;
        return url;
    }

    public String getQrCode() {
        return qrCode;
    }

    public String getQrData() {
        return qrData;
    }

    public String getLink() {
        if (link != null) return link;
        return getPaymentUrl();
    }

    public String getUrl() {
        if (url != null) return url;
        return getPaymentUrl();
    }

    public String getMomoDeeplink() {
        if (momoDeeplink != null) return momoDeeplink;
        if (paymentUrl != null) return paymentUrl;
        if (paymentLink != null) return paymentLink;
        if (link != null) return link;
        return url;
    }

    public String getVietQrUrl() {
        if (vietQrUrl != null) return vietQrUrl;
        if (qrData != null) return qrData;
        if (qrCode != null) return qrCode;
        if (paymentUrl != null) return paymentUrl;
        if (paymentLink != null) return paymentLink;
        if (link != null) return link;
        return url;
    }
}