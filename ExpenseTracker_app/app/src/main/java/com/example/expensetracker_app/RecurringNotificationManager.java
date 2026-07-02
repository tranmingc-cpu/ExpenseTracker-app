package com.example.expensetracker_app;

import android.content.Context;
import android.content.SharedPreferences;

import com.expensetracker_manager.model.response.RecurringTransactionResponse;
import com.expensetracker_manager.utils.TokenManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RecurringNotificationManager {

    private static final String PREFS_NAME = "recurring_notification_cache";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_READ_ALERTS = "read_alerts";
    private static final String KEY_DELETED_ALERTS = "deleted_alerts";
    public static final int UPCOMING_DAYS = 3;

    private RecurringNotificationManager() {
    }

    public static final class AlertItem {
        private final RecurringTransactionResponse transaction;
        private final LocalDate executionDate;
        private final long daysUntil;

        AlertItem(RecurringTransactionResponse transaction, LocalDate executionDate, long daysUntil) {
            this.transaction = transaction;
            this.executionDate = executionDate;
            this.daysUntil = daysUntil;
        }

        public RecurringTransactionResponse getTransaction() {
            return transaction;
        }

        public LocalDate getExecutionDate() {
            return executionDate;
        }

        public long getDaysUntil() {
            return daysUntil;
        }

        public boolean isOverdue() {
            return daysUntil < 0;
        }

        public boolean isDueToday() {
            return daysUntil == 0;
        }

        public String getStatusText() {
            if (isOverdue()) {
                return "Quá hạn " + Math.abs(daysUntil) + " ngày";
            }
            if (isDueToday()) {
                return "Đến hạn hôm nay";
            }
            return "Còn " + daysUntil + " ngày";
        }
    }

    public static void saveItems(Context context, List<RecurringTransactionResponse> items) {
        List<RecurringTransactionResponse> safeItems = items == null
                ? Collections.emptyList()
                : new ArrayList<>(items);

        prefs(context)
                .edit()
                .putString(KEY_ITEMS, new Gson().toJson(safeItems))
                .apply();
    }

    public static List<RecurringTransactionResponse> getCachedItems(Context context) {
        String json = prefs(context).getString(KEY_ITEMS, "");
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Type type = new TypeToken<List<RecurringTransactionResponse>>() { }.getType();
            List<RecurringTransactionResponse> items = new Gson().fromJson(json, type);
            return items == null ? new ArrayList<>() : items;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    public static List<AlertItem> getActiveAlerts(List<RecurringTransactionResponse> items) {
        List<AlertItem> alerts = new ArrayList<>();
        if (items == null) {
            return alerts;
        }

        LocalDate today = LocalDate.now();
        for (RecurringTransactionResponse item : items) {
            LocalDate executionDate = parseDate(item == null ? null : item.getNextExecutionDate());
            if (item == null || executionDate == null) {
                continue;
            }

            long daysUntil = ChronoUnit.DAYS.between(today, executionDate);
            if (daysUntil <= UPCOMING_DAYS) {
                alerts.add(new AlertItem(item, executionDate, daysUntil));
            }
        }

        alerts.sort(Comparator.comparingLong(AlertItem::getDaysUntil));
        return alerts;
    }

    public static List<AlertItem> getVisibleAlerts(Context context,
                                                   List<RecurringTransactionResponse> items) {
        Set<String> deletedKeys = getStringSet(context, KEY_DELETED_ALERTS);
        List<AlertItem> visible = new ArrayList<>();

        for (AlertItem alert : getActiveAlerts(items)) {
            if (!deletedKeys.contains(getAlertKey(context, alert))) {
                visible.add(alert);
            }
        }
        return visible;
    }

    public static List<AlertItem> getUnreadAlerts(Context context,
                                                  List<RecurringTransactionResponse> items) {
        Set<String> readKeys = getStringSet(context, KEY_READ_ALERTS);
        List<AlertItem> unread = new ArrayList<>();

        for (AlertItem alert : getVisibleAlerts(context, items)) {
            if (!readKeys.contains(getAlertKey(context, alert))) {
                unread.add(alert);
            }
        }
        return unread;
    }

    public static boolean isRead(Context context, AlertItem alert) {
        return getStringSet(context, KEY_READ_ALERTS)
                .contains(getAlertKey(context, alert));
    }

    public static void markAsRead(Context context, AlertItem alert) {
        updateStringSet(context, KEY_READ_ALERTS, getAlertKey(context, alert), true);
    }

    public static void deleteAlert(Context context, AlertItem alert) {
        String key = getAlertKey(context, alert);
        updateStringSet(context, KEY_DELETED_ALERTS, key, true);
        updateStringSet(context, KEY_READ_ALERTS, key, true);
    }

    public static int getUnreadAlertCount(Context context,
                                          List<RecurringTransactionResponse> items) {
        return getUnreadAlerts(context, items).size();
    }

    public static int getActiveAlertCount(List<RecurringTransactionResponse> items) {
        return getActiveAlerts(items).size();
    }

    public static String safeDescription(RecurringTransactionResponse item) {
        if (item == null || item.getDescription() == null || item.getDescription().trim().isEmpty()) {
            return "Khoản thanh toán định kỳ";
        }
        return item.getDescription().trim();
    }

    public static double safeAmount(RecurringTransactionResponse item) {
        BigDecimal amount = item == null ? null : item.getAmount();
        return amount == null ? 0d : amount.doubleValue();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static Set<String> getStringSet(Context context, String key) {
        Set<String> stored = prefs(context).getStringSet(key, Collections.emptySet());
        return stored == null ? new HashSet<>() : new HashSet<>(stored);
    }

    private static void updateStringSet(Context context, String preferenceKey,
                                        String alertKey, boolean add) {
        Set<String> values = getStringSet(context, preferenceKey);
        if (add) {
            values.add(alertKey);
        } else {
            values.remove(alertKey);
        }
        prefs(context).edit().putStringSet(preferenceKey, values).apply();
    }

    private static String getAlertKey(Context context, AlertItem alert) {
        RecurringTransactionResponse item = alert.getTransaction();
        Long userId = TokenManager.getInstance(context).getUserId();

        String transactionPart;
        if (item != null && item.getId() != null) {
            transactionPart = String.valueOf(item.getId());
        } else {
            transactionPart = safeDescription(item) + "|" + safeAmount(item);
        }

        return userId + "|" + transactionPart + "|" + alert.getExecutionDate();
    }

    private static LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return null;
        }

        String value = rawDate.trim();
        try {
            return LocalDateTime.parse(value).toLocalDate();
        } catch (Exception ignored) {
        }

        try {
            int tIndex = value.indexOf('T');
            return LocalDate.parse(tIndex >= 0 ? value.substring(0, tIndex) : value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
