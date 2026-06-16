package com.expensetracker.expensetracker_api.service;

import com.expensetracker.expensetracker_api.dto.response.CategoryReportResponse;
import com.expensetracker.expensetracker_api.dto.response.ReportSummaryRes;

import java.time.LocalDate;

import java.util.List;

public interface ReportService {

    ReportSummaryRes getSummary(
            Long userId,
            LocalDate startDate,
            LocalDate endDate);

    List<CategoryReportResponse> getExpenseByCategory(
            Long userId,
            LocalDate startDate,
            LocalDate endDate);
}