package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.request.SyncRequest;
import com.expensetracker.expensetracker_api.dto.response.TransactionResponse;
import com.expensetracker.expensetracker_api.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final TransactionService transactionService;

    @PostMapping("/momo")
    public List<TransactionResponse> syncMomo(@RequestBody SyncRequest request) {
        return transactionService.syncMomo(request);
    }
}
