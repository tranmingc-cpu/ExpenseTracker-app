package com.expensetracker.expensetracker_api.service;

import org.springframework.stereotype.Service;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;

@Service
public class PaymentIntegrationService {

    public String generateMomoDeeplink(String phoneNumber, BigDecimal amount, String note) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            phoneNumber = "";
        }
        String amountStr = amount != null ? amount.toPlainString() : "";
        String encodedNote = "";
        try {
            if (note != null) {
                encodedNote = URLEncoder.encode(note, StandardCharsets.UTF_8.name());
            }
        } catch (UnsupportedEncodingException e) {
            encodedNote = note != null ? note : "";
        }
        return "momo://app/transfer?phone=" + phoneNumber + "&amount=" + amountStr + "&note=" + encodedNote;
    }

    public String generateVietQRUrl(String bankId, String accountNumber, BigDecimal amount, String note) {
        if (bankId == null || bankId.trim().isEmpty()) {
            bankId = "unknown";
        }
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            accountNumber = "unknown";
        }
        String amountStr = amount != null ? amount.toPlainString() : "";
        String encodedNote = "";
        try {
            if (note != null) {
                encodedNote = URLEncoder.encode(note, StandardCharsets.UTF_8.name());
            }
        } catch (UnsupportedEncodingException e) {
            encodedNote = note != null ? note : "";
        }

        // Standard VietQR template format: compact2
        return "https://img.vietqr.io/image/" + bankId + "-" + accountNumber + "-compact2.png?amount=" + amountStr + "&addInfo=" + encodedNote;
    }
}
