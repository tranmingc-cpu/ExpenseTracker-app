package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.request.PaymentLinkRequest;
import com.expensetracker.expensetracker_api.dto.response.PaymentLinkResponse;
import com.expensetracker.expensetracker_api.service.PaymentIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentIntegrationService paymentIntegrationService;

    @PostMapping("/generate-links")
    public ResponseEntity<PaymentLinkResponse> generateLinks(@RequestBody PaymentLinkRequest request) {
        String momo = paymentIntegrationService.generateMomoDeeplink(
                request.getPhoneNumber(),
                request.getAmount(),
                request.getNote()
        );
        String vietQr = paymentIntegrationService.generateVietQRUrl(
                request.getBankId(),
                request.getAccountNumber(),
                request.getAmount(),
                request.getNote()
        );
        return ResponseEntity.ok(new PaymentLinkResponse(momo, vietQr));
    }
}
