package com.example.expensetracker_app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.expensetracker_manager.model.response.RecurringTransactionResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.NetworkUtils;
import com.expensetracker_manager.utils.TokenManager;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends BaseActivity {

    private LinearLayout layoutNotificationContainer;
    private TextView tvNotificationSummary;
    private TextView tvEmptyNotifications;
    private ProgressBar progressBarNotifications;
    private List<RecurringTransactionResponse> currentRecurringItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        layoutNotificationContainer = findViewById(R.id.layoutNotificationContainer);
        tvNotificationSummary = findViewById(R.id.tvNotificationSummary);
        tvEmptyNotifications = findViewById(R.id.tvEmptyNotifications);
        progressBarNotifications = findViewById(R.id.progressBarNotifications);

        findViewById(R.id.btnBackNotifications).setOnClickListener(v -> finish());
        findViewById(R.id.btnOpenRecurring).setOnClickListener(v -> openRecurringPayments());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void loadNotifications() {
        Long userId = TokenManager.getInstance(this).getUserId();
        if (userId == -1L) {
            renderNotifications(RecurringNotificationManager.getCachedItems(this));
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            renderNotifications(RecurringNotificationManager.getCachedItems(this));
            return;
        }

        setLoading(true);
        RetrofitClient.getInstance().getRecurringTransactionApi().getByUser(userId)
                .enqueue(new Callback<List<RecurringTransactionResponse>>() {
                    @Override
                    public void onResponse(Call<List<RecurringTransactionResponse>> call,
                                           Response<List<RecurringTransactionResponse>> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            RecurringNotificationManager.saveItems(NotificationsActivity.this, response.body());
                            renderNotifications(response.body());
                        } else {
                            renderNotifications(RecurringNotificationManager.getCachedItems(NotificationsActivity.this));
                            Toast.makeText(NotificationsActivity.this,
                                    "Không thể tải thông báo mới nhất.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<RecurringTransactionResponse>> call, Throwable t) {
                        setLoading(false);
                        renderNotifications(RecurringNotificationManager.getCachedItems(NotificationsActivity.this));
                    }
                });
    }

    private void renderNotifications(List<RecurringTransactionResponse> recurringItems) {
        currentRecurringItems = recurringItems == null
                ? new ArrayList<>()
                : new ArrayList<>(recurringItems);

        List<RecurringNotificationManager.AlertItem> alerts =
                RecurringNotificationManager.getVisibleAlerts(this, currentRecurringItems);
        int unreadCount = RecurringNotificationManager.getUnreadAlertCount(this, currentRecurringItems);

        layoutNotificationContainer.removeAllViews();
        boolean empty = alerts.isEmpty();
        tvEmptyNotifications.setVisibility(empty ? View.VISIBLE : View.GONE);

        if (empty) {
            tvNotificationSummary.setText("Hiện chưa có khoản nào đến hạn trong 3 ngày tới.");
        } else if (unreadCount == 0) {
            tvNotificationSummary.setText("Bạn đã đọc tất cả " + alerts.size() + " thông báo.");
        } else {
            tvNotificationSummary.setText(unreadCount + " chưa đọc • "
                    + alerts.size() + " thông báo đang hiển thị");
        }

        for (RecurringNotificationManager.AlertItem alert : alerts) {
            layoutNotificationContainer.addView(createNotificationCard(alert));
        }
    }

    private View createNotificationCard(RecurringNotificationManager.AlertItem alert) {
        boolean isRead = RecurringNotificationManager.isRead(this, alert);

        CardView card = new CardView(this);
        card.setRadius(dp(14));
        card.setCardElevation(isRead ? dp(1) : dp(2));
        card.setCardBackgroundColor(color(isRead ? R.color.app_surface_alt : R.color.app_surface));
        card.setUseCompatPadding(false);
        card.setClickable(true);
        card.setFocusable(true);
        card.setForeground(ContextCompat.getDrawable(this, android.R.drawable.list_selector_background));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(12), dp(14), dp(10));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(RecurringNotificationManager.safeDescription(alert.getTransaction()));
        title.setTextColor(color(R.color.app_text_primary));
        title.setTextSize(15);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setAlpha(isRead ? 0.72f : 1f);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        titleRow.addView(title);

        TextView status = new TextView(this);
        status.setText(alert.getStatusText());
        status.setTextSize(11);
        status.setTypeface(null, android.graphics.Typeface.BOLD);
        status.setTextColor(alert.isOverdue() || alert.isDueToday()
                ? color(R.color.app_accent_expense)
                : color(R.color.app_accent_warning));
        status.setPadding(dp(8), dp(4), dp(8), dp(4));
        titleRow.addView(status);

        TextView amount = new TextView(this);
        amount.setText("Số tiền: " + formatVND(RecurringNotificationManager.safeAmount(alert.getTransaction())));
        amount.setTextColor(color(R.color.app_text_secondary));
        amount.setTextSize(13);
        amount.setPadding(0, dp(6), 0, 0);

        TextView date = new TextView(this);
        date.setText("Ngày thanh toán: " + alert.getExecutionDate()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        date.setTextColor(color(R.color.app_text_muted));
        date.setTextSize(12);
        date.setPadding(0, dp(3), 0, 0);

        TextView action = new TextView(this);
        action.setText("Nhấn vào thẻ để mở thanh toán định kỳ");
        action.setTextColor(color(R.color.app_accent_income));
        action.setTextSize(12);
        action.setTypeface(null, android.graphics.Typeface.BOLD);
        action.setPadding(0, dp(8), 0, 0);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setPadding(0, dp(10), 0, 0);

        Button btnRead = createActionButton(
                isRead ? "Đã đọc" : "Đánh dấu đã đọc",
                R.color.app_accent_income
        );
        btnRead.setEnabled(!isRead);
        btnRead.setAlpha(isRead ? 0.5f : 1f);
        btnRead.setOnClickListener(v -> {
            RecurringNotificationManager.markAsRead(this, alert);
            renderNotifications(currentRecurringItems);
        });

        Button btnDelete = createActionButton("Xóa", R.color.app_accent_expense);
        LinearLayout.LayoutParams deleteParams = (LinearLayout.LayoutParams) btnDelete.getLayoutParams();
        deleteParams.setMargins(dp(10), 0, 0, 0);
        btnDelete.setLayoutParams(deleteParams);
        btnDelete.setOnClickListener(v -> confirmDeleteNotification(alert));

        buttonRow.addView(btnRead);
        buttonRow.addView(btnDelete);

        body.addView(titleRow);
        body.addView(amount);
        body.addView(date);
        body.addView(action);
        body.addView(buttonRow);
        card.addView(body);

        card.setOnClickListener(v -> {
            RecurringNotificationManager.markAsRead(this, alert);
            openRecurringPayments();
        });
        return card;
    }

    private Button createActionButton(String text, int backgroundColorRes) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(11);
        button.setAllCaps(false);
        button.setTextColor(color(R.color.app_button_text));
        button.setBackgroundTintList(ColorStateList.valueOf(color(backgroundColorRes)));
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setLayoutParams(new LinearLayout.LayoutParams(0, dp(38), 1f));
        return button;
    }

    private void confirmDeleteNotification(RecurringNotificationManager.AlertItem alert) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa thông báo?")
                .setMessage("Thông báo này sẽ được ẩn. Khoản thanh toán định kỳ vẫn được giữ nguyên.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    RecurringNotificationManager.deleteAlert(this, alert);
                    renderNotifications(currentRecurringItems);
                    Toast.makeText(this, "Đã xóa thông báo.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void openRecurringPayments() {
        startActivity(new Intent(this, RecurringActivity.class));
    }

    private void setLoading(boolean loading) {
        progressBarNotifications.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private int color(int colorResId) {
        return ContextCompat.getColor(this, colorResId);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
