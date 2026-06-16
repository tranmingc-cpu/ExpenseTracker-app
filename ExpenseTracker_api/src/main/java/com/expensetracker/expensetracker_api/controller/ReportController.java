package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.response.CategoryReportResponse;
import com.expensetracker.expensetracker_api.dto.response.ReportSummaryRes;
import com.expensetracker.expensetracker_api.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ReportSummaryRes getSummary(

            @RequestParam Long userId,

            @RequestParam LocalDate startDate,

            @RequestParam LocalDate endDate) {

        return reportService.getSummary(userId, startDate,endDate);
    }

    @GetMapping("/category-expense")
    public List<CategoryReportResponse> getExpenseByCategory(

            @RequestParam Long userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return reportService.getExpenseByCategory(userId, startDate, endDate);
    }
}