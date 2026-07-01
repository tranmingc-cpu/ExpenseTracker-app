package com.expensetracker.expensetracker_api.service.impl;

import com.expensetracker.expensetracker_api.dto.response.AiBudgetAnalysisDTO;
import com.expensetracker.expensetracker_api.entity.BudgetEntity;
import com.expensetracker.expensetracker_api.entity.TransactionEntity;
import com.expensetracker.expensetracker_api.repository.BudgetRepository;
import com.expensetracker.expensetracker_api.repository.TransactionRepository;
import com.expensetracker.expensetracker_api.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Override
    public AiBudgetAnalysisDTO analyzeBudget(Long userId, Integer month, Integer year) {
        LocalDate today = LocalDate.now();
        int currentMonth = (month != null) ? month : today.getMonthValue();
        int currentYear = (year != null) ? year : today.getYear();

        // If analyzing a past month, we consider it "completed" (100% progress)
        // If analyzing a future month, 0% progress.
        // If current month, calculate based on today.
        int daysInMonth;
        int daysPassed;
        int daysRemaining;
        double monthProgressPercentage;

        if (currentYear == today.getYear() && currentMonth == today.getMonthValue()) {
            daysInMonth = today.lengthOfMonth();
            daysPassed = today.getDayOfMonth();
            daysRemaining = daysInMonth - daysPassed;
            monthProgressPercentage = ((double) daysPassed / daysInMonth) * 100;
        } else {
            LocalDate targetDate = LocalDate.of(currentYear, currentMonth, 1);
            daysInMonth = targetDate.lengthOfMonth();
            if (targetDate.isBefore(today)) {
                daysPassed = daysInMonth;
                daysRemaining = 0;
                monthProgressPercentage = 100.0;
            } else {
                daysPassed = 1; // Avoid division by zero
                daysRemaining = daysInMonth;
                monthProgressPercentage = 0.0;
            }
        }

        // Variables computed above

        // Fetch User's Budgets for the current month
        List<BudgetEntity> budgets = budgetRepository.findByUserId(userId).stream()
                .filter(b -> b.getMonth() == currentMonth && b.getYear() == currentYear)
                .collect(Collectors.toList());

        //  Fetch current month's transactions
        LocalDate targetStart = LocalDate.of(currentYear, currentMonth, 1);
        LocalDateTime startOfMonth = targetStart.atStartOfDay();
        LocalDateTime endOfMonth = targetStart.withDayOfMonth(daysInMonth).atTime(23, 59, 59);
        List<TransactionEntity> currentMonthTxs = transactionRepository.findByUserIdAndTransactionDateBetween(
                userId, startOfMonth, endOfMonth);

        // Compute current month actual spent per category
        Map<String, BigDecimal> currentCategorySpent = new HashMap<>();
        for (TransactionEntity tx : currentMonthTxs) {
            if ("EXPENSE".equalsIgnoreCase(tx.getType()) && tx.getCategory() != null) {
                String catName = tx.getCategory().getName();
                currentCategorySpent.put(catName, currentCategorySpent.getOrDefault(catName, BigDecimal.ZERO).add(tx.getAmount()));
            }
        }

        //  Fetch past 3 months' transaction history for computing averages
        LocalDateTime startOfHistory = startOfMonth.minusMonths(3);
        LocalDateTime endOfHistory = startOfMonth.minusNanos(1); // right before this month
        List<TransactionEntity> historyTxs = transactionRepository.findByUserIdAndTransactionDateBetween(
                userId, startOfHistory, endOfHistory);

        Map<String, BigDecimal> historyCategorySpent = new HashMap<>();
        for (TransactionEntity tx : historyTxs) {
            if ("EXPENSE".equalsIgnoreCase(tx.getType()) && tx.getCategory() != null) {
                String catName = tx.getCategory().getName();
                historyCategorySpent.put(catName, historyCategorySpent.getOrDefault(catName, BigDecimal.ZERO).add(tx.getAmount()));
            }
        }

        // Build list of stats per category
        List<Map<String, Object>> categoryStatsList = new ArrayList<>();
        String overallRisk = "LOW_RISK";
        boolean hasMedium = false;
        boolean hasHigh = false;

        List<AiBudgetAnalysisDTO.Insight> localInsights = new ArrayList<>();

        for (BudgetEntity b : budgets) {
            String catName = b.getCategory().getName();
            BigDecimal limit = b.getAmount();
            BigDecimal spent = currentCategorySpent.getOrDefault(catName, BigDecimal.ZERO);

            double usePercentage = 0;
            if (limit.compareTo(BigDecimal.ZERO) > 0) {
                usePercentage = spent.multiply(BigDecimal.valueOf(100))
                        .divide(limit, 2, RoundingMode.HALF_UP).doubleValue();
            }

            // Daily average spent this month
            BigDecimal dailyAvgSpent = spent.divide(BigDecimal.valueOf(daysPassed), 2, RoundingMode.HALF_UP);
            // Projection for end of month: daily average * total days in month
            BigDecimal projectedSpent = dailyAvgSpent.multiply(BigDecimal.valueOf(daysInMonth));

            // Averages in previous 3 months
            BigDecimal historyTotal = historyCategorySpent.getOrDefault(catName, BigDecimal.ZERO);
            BigDecimal historyAvgMonthly = historyTotal.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

            // Risk evaluation
            String risk = "LOW";
            if (spent.compareTo(limit) > 0 || projectedSpent.compareTo(limit.multiply(BigDecimal.valueOf(1.1))) > 0) {
                risk = "HIGH";
                hasHigh = true;
            } else if (usePercentage > monthProgressPercentage + 15 || usePercentage > 80) {
                risk = "MEDIUM";
                hasMedium = true;
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("category", catName);
            stats.put("budgetLimit", limit);
            stats.put("spentSoFar", spent);
            stats.put("usePercentage", usePercentage);
            stats.put("dailyAvgSpent", dailyAvgSpent);
            stats.put("projectedSpent", projectedSpent);
            stats.put("past3MonthAvg", historyAvgMonthly);
            stats.put("risk", risk);
            categoryStatsList.add(stats);
        }

        if (hasHigh) {
            overallRisk = "HIGH_RISK";
        } else if (hasMedium) {
            overallRisk = "MEDIUM_RISK";
        }

        // Call Gemini to get smart insights & savings recommendations
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Gemini API key is not configured. Falling back to local rules-based insights.");
            return generateLocalFallbackInsights(overallRisk, categoryStatsList, monthProgressPercentage);
        }

        try {
            return queryGeminiForInsights(overallRisk, categoryStatsList, monthProgressPercentage, daysPassed, daysRemaining);
        } catch (Exception e) {
            log.error("Failed to query Gemini API", e);
            return generateLocalFallbackInsights(overallRisk, categoryStatsList, monthProgressPercentage);
        }
    }

    private AiBudgetAnalysisDTO generateLocalFallbackInsights(String overallRisk, List<Map<String, Object>> statsList, double monthProgress) {
        List<AiBudgetAnalysisDTO.Insight> insights = new ArrayList<>();
        for (Map<String, Object> stats : statsList) {
            String cat = (String) stats.get("category");
            String risk = (String) stats.get("risk");
            double pct = (double) stats.get("usePercentage");
            BigDecimal limit = (BigDecimal) stats.get("budgetLimit");
            BigDecimal projected = (BigDecimal) stats.get("projectedSpent");

            String message;
            if ("HIGH".equals(risk)) {
                BigDecimal exceeded = projected.subtract(limit);
                message = String.format("⚠️ %s đã sử dụng %.0f%% ngân sách khi tháng mới đi qua %.0f%%. Dự kiến vượt khoảng %,.0fđ vào cuối tháng.",
                        cat, pct, monthProgress, exceeded.max(BigDecimal.ZERO).doubleValue());
            } else if ("MEDIUM".equals(risk)) {
                message = String.format("⚠️ Thận trọng: %s đã dùng %.0f%% ngân sách. Hãy điều tiết chi tiêu hợp lý.", cat, pct);
            } else {
                message = String.format("💡 %s đang chi tiêu ở mức an toàn (%.0f%% ngân sách). Hãy tiếp tục duy trì!", cat, pct);
            }
            insights.add(new AiBudgetAnalysisDTO.Insight(cat, risk, message));
        }
        return new AiBudgetAnalysisDTO.Insight[] {}.length == 0 ?
                new AiBudgetAnalysisDTO(overallRisk, insights) : null;
    }

    private AiBudgetAnalysisDTO queryGeminiForInsights(String overallRisk, List<Map<String, Object>> statsList,
                                                      double monthProgress, int daysPassed, int daysRemaining) throws Exception {
        JSONObject promptJson = new JSONObject();
        promptJson.put("daysPassed", daysPassed);
        promptJson.put("daysRemaining", daysRemaining);
        promptJson.put("monthProgressPercentage", monthProgress);
        
        JSONArray categoriesArray = new JSONArray();
        for (Map<String, Object> stats : statsList) {
            JSONObject catObj = new JSONObject();
            catObj.put("categoryName", stats.get("category"));
            catObj.put("budgetLimit", stats.get("budgetLimit"));
            catObj.put("spentSoFar", stats.get("spentSoFar"));
            catObj.put("usePercentage", stats.get("usePercentage"));
            catObj.put("projectedSpent", stats.get("projectedSpent"));
            catObj.put("past3MonthAvg", stats.get("past3MonthAvg"));
            catObj.put("calculatedRisk", stats.get("risk"));
            categoriesArray.put(catObj);
        }
        promptJson.put("categoriesStats", categoriesArray);

        String systemInstruction = "Bạn là trợ lý tài chính AI thông thái tích hợp trong app ExpenseTracker. " +
                "Nhiệm vụ của bạn là phân tích dữ liệu ngân sách và chi tiêu của người dùng, đưa ra các nhận xét cụ thể và đề xuất hữu ích bằng Tiếng Việt. " +
                "Bạn PHẢI trả về kết quả dưới dạng JSON hợp lệ tuân theo cấu trúc sau:\n" +
                "{\n" +
                "  \"insights\": [\n" +
                "    {\n" +
                "      \"category\": \"Tên danh mục\",\n" +
                "      \"risk\": \"LOW/MEDIUM/HIGH\",\n" +
                "      \"message\": \"Câu phân tích sắc bén chứa cả ⚠️ nếu nguy cơ cao/trung bình hoặc 💡 kèm đề xuất tiết kiệm/điều chỉnh ngân sách (Ví dụ: Ăn uống đã sử dụng 78% ngân sách khi tháng mới đi qua 52%. Dự kiến vượt khoảng 650.000đ vào cuối tháng. Hãy ưu tiên nấu ăn tại nhà)\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "Chú ý: Chỉ trả về chuỗi JSON thô, không bọc trong markdown block ```json.";

        String userPrompt = "Hãy phân tích dữ liệu này và trả về JSON:\n" + promptJson.toString();

        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Build the request body for Gemini API v1beta
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", userPrompt);
        JSONObject contentObj = new JSONObject();
        contentObj.put("parts", new JSONArray().put(part));
        contents.put(contentObj);
        requestBody.put("contents", contents);

        JSONObject systemInstructionObj = new JSONObject();
        JSONObject systemPart = new JSONObject();
        systemPart.put("text", systemInstruction);
        systemInstructionObj.put("parts", new JSONArray().put(systemPart));
        requestBody.put("systemInstruction", systemInstructionObj);

        // Optional: configure response mime type to json
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        if (conn.getResponseCode() >= 400) {
            throw new RuntimeException("HTTP error code: " + conn.getResponseCode() + ", details: " + response.toString());
        }

        JSONObject geminiRes = new JSONObject(response.toString());
        String textResponse = geminiRes.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        // Parse structured output from Gemini
        JSONObject parsedJson = new JSONObject(textResponse.trim());
        JSONArray insightsArr = parsedJson.getJSONArray("insights");

        List<AiBudgetAnalysisDTO.Insight> insights = new ArrayList<>();
        for (int i = 0; i < insightsArr.length(); i++) {
            JSONObject item = insightsArr.getJSONObject(i);
            insights.add(new AiBudgetAnalysisDTO.Insight(
                    item.getString("category"),
                    item.getString("risk"),
                    item.getString("message")
            ));
        }

        return new AiBudgetAnalysisDTO(overallRisk, insights);
    }
}
