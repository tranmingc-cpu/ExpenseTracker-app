package com.expensetracker.expensetracker_api.service;

import com.expensetracker.expensetracker_api.dto.response.OcrResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OcrServiceTest {

    private final OcrService ocrService = new OcrService();

    @Test
    public void testExtractBillDetails_Vietcombank() {
        String receiptText = "GIAO DỊCH THÀNH CÔNG\n" +
                "Số tiền: 500,000 VND\n" +
                "Thời gian: 21/06/2026 16:00:00\n" +
                "Tên người nhận: NGUYEN VAN A\n" +
                "Số tài khoản: 0123456789\n" +
                "Tại ngân hàng: Vietcombank\n" +
                "Nội dung: Chuyen tien an trua";

        OcrResponse response = ocrService.extractBillDetails(receiptText);

        assertEquals("500000", response.getAmount());
        assertEquals("Chuyen tien an trua", response.getDescription());
        assertTrue(response.getTransactionTime().contains("21/06/2026"));
        assertEquals("NGUYEN VAN A", response.getAccountName());
        assertEquals("0123456789", response.getAccountNumber());
        assertEquals("Vietcombank", response.getBankName());
    }

    @Test
    public void testExtractBillDetails_MBBank() {
        String receiptText = "MB Bank\n" +
                "Chuyển tiền thành công đến\n" +
                "NGUYEN THI B\n" +
                "STK: 970422123456789\n" +
                "Số tiền: 150.000 VND\n" +
                "Ngày thực hiện: 20-06-2026\n" +
                "Nội dung chuyển khoản: Tra tien ca phe";

        OcrResponse response = ocrService.extractBillDetails(receiptText);

        assertEquals("150000", response.getAmount());
        assertEquals("Tra tien ca phe", response.getDescription());
        assertTrue(response.getTransactionTime().contains("20-06-2026"));
        assertEquals("NGUYEN THI B", response.getAccountName());
        assertEquals("970422123456789", response.getAccountNumber());
        assertEquals("MB Bank", response.getBankName());
    }

    @Test
    public void testExtractBillDetails_Techcombank() {
        String receiptText = "Techcombank\n" +
                "Giao dịch thành công\n" +
                "Số tiền: 2,500,000 đ\n" +
                "Tài khoản thụ hưởng: 19033456789012\n" +
                "Tên người hưởng: CONG TY TNHH ABC\n" +
                "Thời gian: 15/06/2026\n" +
                "Nội dung: Thanh toan hoa don mua hang";

        OcrResponse response = ocrService.extractBillDetails(receiptText);

        assertEquals("2500000", response.getAmount());
        assertEquals("Thanh toan hoa don mua hang", response.getDescription());
        assertTrue(response.getTransactionTime().contains("15/06/2026"));
        assertEquals("CONG TY TNHH ABC", response.getAccountName());
        assertEquals("19033456789012", response.getAccountNumber());
        assertEquals("Techcombank", response.getBankName());
    }
}
