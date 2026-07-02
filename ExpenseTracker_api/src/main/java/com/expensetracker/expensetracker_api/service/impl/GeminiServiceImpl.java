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
import org.springframework.cache.annotation.Cacheable;
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

    private static final int MAX_RETRIES = 3;

    @Override
    @Cacheable(value = "budgetAnalysis",
            key = "#userId + '-' + (#month != null ? #month : T(java.time.LocalDate).now().getMonthValue()) + '-' + (#year != null ? #year : T(java.time.LocalDate).now().getYear())")
    public AiBudgetAnalysisDTO analyzeBudget(Long userId, Integer month, Integer year) {
        LocalDate today = LocalDate.now();
        int currentMonth = (month != null) ? month : today.getMonthValue();
        int currentYear  = (year  != null) ? year  : today.getYear();

        // Calculate month progress
        int daysInMonth;
        int daysPassed;
        int daysRemaining;
        double monthProgressPercentage;

        if (currentYear == today.getYear() && currentMonth == today.getMonthValue()) {
            daysInMonth             = today.lengthOfMonth();
            daysPassed              = today.getDayOfMonth();
            daysRemaining           = daysInMonth - daysPassed;
            monthProgressPercentage = ((double) daysPassed / daysInMonth) * 100;
        } else {
            LocalDate targetDate = LocalDate.of(currentYear, currentMonth, 1);
            daysInMonth = targetDate.lengthOfMonth();
            if (targetDate.isBefore(today)) {
                daysPassed              = daysInMonth;
                daysRemaining           = 0;
                monthProgressPercentage = 100.0;
            } else {
                daysPassed              = 1;
                daysRemaining           = daysInMonth;
                monthProgressPercentage = 0.0;
            }
        }

        // Fetch budgets for the target month
        List<BudgetEntity> budgets = budgetRepository.findByUserId(userId).stream()
                .filter(b -> b.getMonth() == currentMonth && b.getYear() == currentYear)
                .collect(Collectors.toList());
        log.info("[AI Budget] userId={} month={} year={} => found {} budget(s)", userId, currentMonth, currentYear, budgets.size());

        // Fetch current month transactions
        LocalDate targetStart   = LocalDate.of(currentYear, currentMonth, 1);
        LocalDateTime startOfMonth = targetStart.atStartOfDay();
        LocalDateTime endOfMonth   = targetStart.withDayOfMonth(daysInMonth).atTime(23, 59, 59);
        List<TransactionEntity> currentMonthTxs = transactionRepository
                .findByUserIdAndTransactionDateBetween(userId, startOfMonth, endOfMonth);

        // Compute current-month spending per category
        Map<String, BigDecimal> currentCategorySpent = new HashMap<>();
        for (TransactionEntity tx : currentMonthTxs) {
            if ("EXPENSE".equalsIgnoreCase(tx.getType()) && tx.getCategory() != null) {
                String catName = tx.getCategory().getName();
                currentCategorySpent.merge(catName, tx.getAmount(), BigDecimal::add);
            }
        }

        // Fetch past 3 months history
        LocalDateTime startOfHistory = startOfMonth.minusMonths(3);
        LocalDateTime endOfHistory   = startOfMonth.minusNanos(1);
        List<TransactionEntity> historyTxs = transactionRepository
                .findByUserIdAndTransactionDateBetween(userId, startOfHistory, endOfHistory);

        Map<String, BigDecimal> historyCategorySpent = new HashMap<>();
        for (TransactionEntity tx : historyTxs) {
            if ("EXPENSE".equalsIgnoreCase(tx.getType()) && tx.getCategory() != null) {
                String catName = tx.getCategory().getName();
                historyCategorySpent.merge(catName, tx.getAmount(), BigDecimal::add);
            }
        }

        // Build budget map for quick lookup
        Map<String, BudgetEntity> budgetByCategory = new HashMap<>();
        for (BudgetEntity b : budgets) {
            budgetByCategory.put(b.getCategory().getName(), b);
        }


        List<Map<String, Object>> budgetedStatsList = new ArrayList<>();
        String overallRisk = "LOW_RISK";
        boolean hasMedium  = false;
        boolean hasHigh    = false;

        for (Map.Entry<String, BudgetEntity> entry : budgetByCategory.entrySet()) {
            String catName = entry.getKey();
            BigDecimal limit = entry.getValue().getAmount();
            if (limit == null || limit.compareTo(BigDecimal.ZERO) <= 0) continue; // skip zero-limit

            BigDecimal spent = currentCategorySpent.getOrDefault(catName, BigDecimal.ZERO);

            double usePercentage = spent.multiply(BigDecimal.valueOf(100))
                    .divide(limit, 2, RoundingMode.HALF_UP).doubleValue();

            BigDecimal dailyAvgSpent = daysPassed > 0
                    ? spent.divide(BigDecimal.valueOf(daysPassed), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal projectedSpent = dailyAvgSpent.multiply(BigDecimal.valueOf(daysInMonth));

            BigDecimal historyTotal      = historyCategorySpent.getOrDefault(catName, BigDecimal.ZERO);
            BigDecimal historyAvgMonthly = historyTotal.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

            String risk = "LOW";
            if (spent.compareTo(limit) > 0 || projectedSpent.compareTo(limit.multiply(BigDecimal.valueOf(1.1))) > 0) {
                risk    = "HIGH";
                hasHigh = true;
            } else if (usePercentage > monthProgressPercentage + 15 || usePercentage > 80) {
                risk      = "MEDIUM";
                hasMedium = true;
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("category",      catName);
            stats.put("budgetLimit",   limit);
            stats.put("spentSoFar",    spent);
            stats.put("usePercentage", usePercentage);
            stats.put("projectedSpent", projectedSpent);
            stats.put("past3MonthAvg", historyAvgMonthly);
            stats.put("risk",          risk);
            budgetedStatsList.add(stats);
        }

        if (hasHigh)        overallRisk = "HIGH_RISK";
        else if (hasMedium) overallRisk = "MEDIUM_RISK";

        log.info("[AI Budget] Budgeted categories (for warnings): {}", budgetedStatsList.size());

        // ── LUỒNG 2: TẤT CẢ category có chi tiêu lịch sử (kể cả không có budget) ──
        // Union: spending in current month OR in past 3 months
        Set<String> allSpendingCategories = new HashSet<>();
        allSpendingCategories.addAll(currentCategorySpent.keySet());
        allSpendingCategories.addAll(historyCategorySpent.keySet());

        List<Map<String, Object>> suggestionStatsList = new ArrayList<>();
        for (String catName : allSpendingCategories) {
            BigDecimal spent        = currentCategorySpent.getOrDefault(catName, BigDecimal.ZERO);
            BigDecimal historyTotal = historyCategorySpent.getOrDefault(catName, BigDecimal.ZERO);
            BigDecimal historyAvg   = historyTotal.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

            Map<String, Object> stats = new HashMap<>();
            stats.put("category",      catName);
            stats.put("spentSoFar",    spent);
            stats.put("past3MonthAvg", historyAvg);
            stats.put("hasBudget",     budgetByCategory.containsKey(catName));
            suggestionStatsList.add(stats);
        }

        log.info("[AI Budget] All spending categories (for suggestions): {}", suggestionStatsList.size());

        // If absolutely no data, return empty
        if (budgetedStatsList.isEmpty() && suggestionStatsList.isEmpty()) {
            log.warn("[AI Budget] No budget or spending data for userId={} month={} year={}", userId, currentMonth, currentYear);
            return new AiBudgetAnalysisDTO(overallRisk, new ArrayList<>(), new ArrayList<>());
        }

        // Use Gemini if API key is available
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Gemini API key is not configured. Falling back to local rules-based insights.");
            return generateLocalFallbackInsights(overallRisk, budgetedStatsList, suggestionStatsList, monthProgressPercentage);
        }

        log.info("[AI Budget] Calling Gemini API. budgetedCats={}, suggestionCats={}", budgetedStatsList.size(), suggestionStatsList.size());
        try {
            AiBudgetAnalysisDTO result = queryGeminiForInsights(
                    overallRisk, budgetedStatsList, suggestionStatsList,
                    monthProgressPercentage, daysPassed, daysRemaining);
            log.info("[AI Budget] Gemini returned {} insights, {} suggestions",
                    result != null && result.getInsights() != null ? result.getInsights().size() : 0,
                    result != null && result.getBudgetSuggestions() != null ? result.getBudgetSuggestions().size() : 0);
            return result;
        } catch (Exception e) {
            log.error("[AI Budget] Failed to query Gemini API (after retries): {}", e.getMessage());
            return generateLocalFallbackInsights(overallRisk, budgetedStatsList, suggestionStatsList, monthProgressPercentage);
        }
    }


    private AiBudgetAnalysisDTO generateLocalFallbackInsights(
            String overallRisk,
            List<Map<String, Object>> budgetedStatsList,
            List<Map<String, Object>> suggestionStatsList,
            double monthProgress) {

        // Luồng 1: cảnh báo + khuynế nghị hành động cụ thể cho category có budget
        List<AiBudgetAnalysisDTO.Insight> insights = new ArrayList<>();
        for (Map<String, Object> stats : budgetedStatsList) {
            String     cat       = (String)     stats.get("category");
            String     risk      = (String)     stats.get("risk");
            double     pct       = (double)     stats.get("usePercentage");
            BigDecimal limit     = (BigDecimal) stats.get("budgetLimit");
            BigDecimal projected = (BigDecimal) stats.get("projectedSpent");
            BigDecimal histAvg   = (BigDecimal) stats.get("past3MonthAvg");
            BigDecimal spent     = (BigDecimal) stats.get("spentSoFar");

            String message;
            double recLimit;

            if ("HIGH".equals(risk)) {
                BigDecimal exceeded = projected.subtract(limit).max(BigDecimal.ZERO);
                BigDecimal canCut = spent.subtract(limit.multiply(BigDecimal.valueOf(0.8))).max(BigDecimal.ZERO);
                message = String.format(
                    "⚠️ **Hạn chế ngay %s!** Bạn đã dùng %.0f%% ngân sách trong khi tháng mới đi qua %.0f%%. " +
                    "Dự kiến vượt %,.0fđ. " +
                    "➡️ Đề nghị giảm ít nhất %,.0fđ trong các kểhoản còn lại của tháng này.",
                    cat, pct, monthProgress, exceeded.doubleValue(), canCut.doubleValue());
                recLimit = histAvg.compareTo(BigDecimal.ZERO) > 0
                    ? histAvg.multiply(BigDecimal.valueOf(0.85)).doubleValue()
                    : limit.multiply(BigDecimal.valueOf(0.8)).doubleValue();

            } else if ("MEDIUM".equals(risk)) {
                BigDecimal saveAmount = projected.subtract(limit).max(BigDecimal.ZERO);
                if (saveAmount.compareTo(BigDecimal.ZERO) > 0) {
                    message = String.format(
                        "⚠️ **Thận trọng với %s.** Chi tiêu đang ở mức %.0f%% ngân sách. " +
                        "➡️ Hãy giảm bớt các khoản không cần thiết trong danh mục này để không vượt hạn mức.",
                        cat, pct);
                } else {
                    message = String.format(
                        "⚠️ **Kiểm soát %s.** Bạn đã dùng %.0f%% ngân sách. " +
                        "➡️ Nếu có khoản nào không cần thiết, hãy bỏ qua trong thời gian này.",
                        cat, pct);
                }
                recLimit = histAvg.compareTo(BigDecimal.ZERO) > 0
                    ? histAvg.multiply(BigDecimal.valueOf(0.95)).doubleValue()
                    : limit.doubleValue();

            } else {
                // LOW risk — vẫn khuyến khích duy trì + gợi ý tiết kiệm thêm
                BigDecimal surplus = limit.subtract(spent);
                if (surplus.compareTo(BigDecimal.valueOf(50000)) > 0) {
                    message = String.format(
                        "💡 **%s đang tốt!** Chi tiêu ở mức an toàn (%.0f%%). " +
                        "➡️ Bạn có thể để dành %,.0fđ còn thừa sang mục tiêu tiết kiệm.",
                        cat, pct, surplus.doubleValue());
                } else {
                    message = String.format(
                        "💡 **%s đang ỏ mức an toàn** (%.0f%% ngân sách). Hãy duy trì thói quen này!",
                        cat, pct);
                }
                recLimit = limit.doubleValue();
            }

            if (recLimit <= 0) recLimit = limit.doubleValue();
            insights.add(AiBudgetAnalysisDTO.Insight.builder()
                    .category(cat).risk(risk).message(message).recommendedLimit(recLimit).build());
        }

        // Luồng 2: gợi ý hạn mức cho tất cả category có chi tiêu
        List<AiBudgetAnalysisDTO.BudgetSuggestion> suggestions = new ArrayList<>();
        for (Map<String, Object> stats : suggestionStatsList) {
            String     cat      = (String)     stats.get("category");
            BigDecimal spent    = (BigDecimal) stats.get("spentSoFar");
            BigDecimal histAvg  = (BigDecimal) stats.get("past3MonthAvg");

            // Đề xuất = max(chi tiêu hiện tại, trung bình 3 tháng) * 1.05 để có buffer nhỏ
            BigDecimal base = spent.max(histAvg);
            if (base.compareTo(BigDecimal.ZERO) <= 0) continue;
            double recLimit = base.multiply(BigDecimal.valueOf(1.05))
                    .setScale(0, RoundingMode.CEILING).doubleValue();

            String reason = String.format("Dựa trên chi tiêu trung bình %,.0fđ/tháng (3 tháng qua).", histAvg.doubleValue());

            suggestions.add(AiBudgetAnalysisDTO.BudgetSuggestion.builder()
                    .category(cat)
                    .recommendedLimit(recLimit)
                    .reason(reason)
                    .spentSoFar(spent.doubleValue())
                    .build());
        }

        return new AiBudgetAnalysisDTO(overallRisk, insights, suggestions);
    }


    private AiBudgetAnalysisDTO queryGeminiForInsights(
            String overallRisk,
            List<Map<String, Object>> budgetedStatsList,
            List<Map<String, Object>> suggestionStatsList,
            double monthProgress, int daysPassed, int daysRemaining) throws Exception {

        // Build prompt JSON
        JSONObject promptJson = new JSONObject();
        promptJson.put("daysPassed",              daysPassed);
        promptJson.put("daysRemaining",           daysRemaining);
        promptJson.put("monthProgressPercentage", monthProgress);

        // budgetedCategories → for warnings
        JSONArray budgetedArr = new JSONArray();
        for (Map<String, Object> stats : budgetedStatsList) {
            JSONObject obj = new JSONObject();
            obj.put("categoryName",   stats.get("category"));
            obj.put("budgetLimit",    ((BigDecimal) stats.get("budgetLimit")).toPlainString());
            obj.put("spentSoFar",     ((BigDecimal) stats.get("spentSoFar")).toPlainString());
            obj.put("usePercentage",  stats.get("usePercentage"));
            obj.put("projectedSpent", ((BigDecimal) stats.get("projectedSpent")).toPlainString());
            obj.put("past3MonthAvg",  ((BigDecimal) stats.get("past3MonthAvg")).toPlainString());
            obj.put("calculatedRisk", stats.get("risk"));
            budgetedArr.put(obj);
        }
        promptJson.put("budgetedCategories", budgetedArr);

        // allSpendingCategories → for suggestions
        JSONArray suggestArr = new JSONArray();
        for (Map<String, Object> stats : suggestionStatsList) {
            JSONObject obj = new JSONObject();
            obj.put("categoryName",  stats.get("category"));
            obj.put("spentSoFar",    ((BigDecimal) stats.get("spentSoFar")).toPlainString());
            obj.put("past3MonthAvg", ((BigDecimal) stats.get("past3MonthAvg")).toPlainString());
            obj.put("hasBudget",     stats.get("hasBudget"));
            suggestArr.put(obj);
        }
        promptJson.put("allSpendingCategories", suggestArr);

        String systemInstruction =
                "Bạn là chuyên gia tài chính cá nhân thông thái trong app ExpenseTracker. Trả lời bằng Tiếng Việt.\n" +
                "Bạn nhận dữ liệu gồm 2 nhóm:\n" +
                "1. budgetedCategories: danh mục ĐÃ CÓ hạn mức ngân sách → sinh KHUYẾN NGHỊ HÀNH ĐỘNG cụ thể (insights).\n" +
                "2. allSpendingCategories: TẤT CẢ danh mục có chi tiêu → sinh gợi ý hạn mức (budgetSuggestions).\n\n" +
                "Trả về JSON hợp lệ theo đúng cấu trúc sau (KHÔNG markdown block):\n" +
                "{\n" +
                "  \"insights\": [\n" +
                "    {\n" +
                "      \"category\": \"Tên danh mục\",\n" +
                "      \"risk\": \"LOW|MEDIUM|HIGH\",\n" +
                "      \"message\": \"Câu khuyến nghị hành động cụ thể (xem hướng dẫn bên dưới)\",\n" +
                "      \"recommendedLimit\": 1500000\n" +
                "    }\n" +
                "  ],\n" +
                "  \"budgetSuggestions\": [\n" +
                "    {\n" +
                "      \"category\": \"Tên danh mục\",\n" +
                "      \"recommendedLimit\": 800000,\n" +
                "      \"reason\": \"Giải thích ngắn tại sao đề xuất mức này\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n" +
                "HƯỚNG DẪN VIẾT message trong insights (BẮT BUỘC theo mức rủi ro):\n" +
                "- HIGH: Phải có ⚠️, nêu tên danh mục, % đã dùng, ĐỀ NGHỊ GIẢM CỤ THỂ + ➡️ hành động:\n" +
                "  Ví dụ: '⚠️ **Hạn chế ngay Ăn uống!** Bạn đã dùng 95% ngân sách. Nếu giữ đà này sẽ vượt 250.000đ. ➡️ Cắt giảm 1-2 bữa ăn ngoài/tuần hoặc chuyển sang nấu ở nhà.'\n" +
                "- MEDIUM: Phải có ⚠️, ĐỀ NGHỊ KIỂM SOÁT hoặc HUỶ khoản không cần thiết + ➡️:\n" +
                "  Ví dụ: '⚠️ **Kiểm soát Giải trí.** Đã dùng 72%, còn 15 ngày. ➡️ Hoãn subscription không dùng, giảm tần suất đi chơi cuối tuần.'\n" +
                "- LOW: Có 💡, khen ngợi + gợi ý TIẾT KIỆM thêm hoặc chuyển vào quỹ dự phòng + ➡️:\n" +
                "  Ví dụ: '💡 **Di chuyển đang tốt!** Chi tiêu hợp lý. ➡️ Bạn còn dư 180.000đ — hãy chuyển vào quỹ tiết kiệm khẩn cấp.'\n\n" +
                "QUAN TRỌNG:\n" +
                "- insights: CHỈ từ budgetedCategories. KHÔNG sinh cảnh báo cho danh mục không có budget.\n" +
                "- message PHẢI chứa ➡️ và hành động cụ thể (giảm gì, hủy cái gì, chuyển vào đâu). Không chung chung.\n" +
                "- budgetSuggestions: từ allSpendingCategories. recommendedLimit dựa trên past3MonthAvg và spentSoFar.\n" +
                "  + HIGH/chi quá nhiều: giảm 15-20% so với avg. MEDIUM: giảm 5%. LOW/chưa có budget: avg * 1.1.\n" +
                "- recommendedLimit PHẢI là số nguyên dương (VND), không có ký tự đặc biệt.\n" +
                "- Chỉ trả về chuỗi JSON thuần túy, không có text thừa.";

        String userPrompt = "Phân tích dữ liệu và trả về JSON:\n" + promptJson;


        // Build Gemini request body
        JSONObject requestBody = new JSONObject();
        JSONArray  contents    = new JSONArray();
        JSONObject part        = new JSONObject();
        part.put("text", userPrompt);
        JSONObject contentObj = new JSONObject();
        contentObj.put("parts", new JSONArray().put(part));
        contents.put(contentObj);
        requestBody.put("contents", contents);

        JSONObject systemInstructionObj = new JSONObject();
        JSONObject systemPart           = new JSONObject();
        systemPart.put("text", systemInstruction);
        systemInstructionObj.put("parts", new JSONArray().put(systemPart));
        requestBody.put("systemInstruction", systemInstructionObj);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);

        byte[] bodyBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);

        // Retry loop for 429
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes, 0, bodyBytes.length);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == 429) {
                // Parse retry delay for logging only — DO NOT sleep here.
                // Sleeping 57s blocks a Tomcat thread and causes Android client timeout.
                StringBuilder errBody = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) errBody.append(line);
                }
                long waitSeconds = parseRetryDelay(errBody.toString());
                conn.disconnect();
                log.warn("[AI Budget] Gemini quota exceeded (429). RetryAfter={}s. Falling back to local insights.", waitSeconds);
                throw new RuntimeException("Gemini quota exceeded (429). RetryAfter=" + waitSeconds + "s");
            }

            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) response.append(line);
            }

            if (responseCode >= 400) {
                throw new RuntimeException("HTTP error code: " + responseCode + ", details: " + response);
            }

            // Parse Gemini response
            JSONObject geminiRes = new JSONObject(response.toString());
            String textResponse  = geminiRes.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

            JSONObject parsedJson = new JSONObject(textResponse.trim());

            // Parse insights (budgeted categories)
            List<AiBudgetAnalysisDTO.Insight> insights = new ArrayList<>();
            if (parsedJson.has("insights")) {
                JSONArray insightsArr = parsedJson.getJSONArray("insights");
                for (int i = 0; i < insightsArr.length(); i++) {
                    JSONObject item   = insightsArr.getJSONObject(i);
                    Double    recLim  = item.has("recommendedLimit") ? item.getDouble("recommendedLimit") : null;
                    insights.add(AiBudgetAnalysisDTO.Insight.builder()
                            .category(item.getString("category"))
                            .risk(item.getString("risk"))
                            .message(item.getString("message"))
                            .recommendedLimit(recLim)
                            .build());
                }
            }

            // Parse budgetSuggestions (all spending categories)
            List<AiBudgetAnalysisDTO.BudgetSuggestion> suggestions = new ArrayList<>();
            if (parsedJson.has("budgetSuggestions")) {
                JSONArray suggestArr2 = parsedJson.getJSONArray("budgetSuggestions");
                for (int i = 0; i < suggestArr2.length(); i++) {
                    JSONObject item  = suggestArr2.getJSONObject(i);
                    String catName   = item.getString("category");
                    Double recLim    = item.has("recommendedLimit") ? item.getDouble("recommendedLimit") : null;
                    String reason    = item.optString("reason", "");
                    // Lấy spentSoFar từ suggestionStatsList theo tên category
                    Double spentVal  = suggestionStatsList.stream()
                            .filter(s -> catName.equals(s.get("category")))
                            .map(s -> ((BigDecimal) s.get("spentSoFar")).doubleValue())
                            .findFirst()
                            .orElse(0.0);
                    suggestions.add(AiBudgetAnalysisDTO.BudgetSuggestion.builder()
                            .category(catName)
                            .recommendedLimit(recLim)
                            .reason(reason)
                            .spentSoFar(spentVal)
                            .build());
                }
            }

            conn.disconnect();
            return new AiBudgetAnalysisDTO(overallRisk, insights, suggestions);
        }

        // All retries exhausted
        log.warn("[AI Budget] Falling back to local insights after quota exhaustion.");
        throw (lastException != null) ? new Exception(lastException) : new Exception("Gemini query failed");
    }

    /** Parse retryDelay (e.g. "57s") from Gemini 429 error JSON. Returns 60 as default. */
    private long parseRetryDelay(String errorBody) {
        try {
            JSONObject err     = new JSONObject(errorBody);
            JSONArray  details = err.optJSONObject("error") != null
                    ? err.getJSONObject("error").optJSONArray("details")
                    : null;
            if (details != null) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject d = details.getJSONObject(i);
                    if (d.has("retryDelay")) {
                        String delay = d.getString("retryDelay"); // e.g. "57s"
                        return Long.parseLong(delay.replace("s", "").trim());
                    }
                }
            }
        } catch (Exception ignored) {}
        return 60L;
    }
}
