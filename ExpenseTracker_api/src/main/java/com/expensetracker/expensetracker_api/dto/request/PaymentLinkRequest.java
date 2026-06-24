package com.expensetracker.expensetracker_api.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentLinkRequest {
    private String phoneNumber;
    private String bankId;
    private String accountNumber;
    private BigDecimal amount;
    private String note;
}
