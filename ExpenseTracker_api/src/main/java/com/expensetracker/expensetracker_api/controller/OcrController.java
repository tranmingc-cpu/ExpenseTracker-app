package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.response.OcrResponse;
import com.expensetracker.expensetracker_api.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/transactions")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @PostMapping("/analyze-bill")
    public ResponseEntity<OcrResponse> analyzeBill(@RequestParam("file") MultipartFile file) {
        String fullText = ocrService.parseImage(file);

        System.out.println("Text bóc tách từ OCR.space: " + fullText);

        OcrResponse response = ocrService.extractBillDetails(fullText);

        return ResponseEntity.ok(response);
    }
}