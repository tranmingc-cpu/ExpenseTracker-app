package com.example.expensetracker_app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.expensetracker_manager.model.response.RecurringTransactionResponse;

import java.util.Calendar;
import java.util.List;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_DAILY_REMINDER =
            "com.example.expensetracker_app.DAILY_REMINDER";
    public static final String ACTION_RECURRING_PAYMENT_CHECK =
            "com.example.expensetracker_app.RECURRING_PAYMENT_CHECK";

    private static final String DAILY_CHANNEL_ID = "expense_reminder_channel_v3";
    private static final String DAILY_CHANNEL_NAME = "Nhắc nhở ghi chép";
    private static final String RECURRING_CHANNEL_ID = "recurring_payment_channel_v1";
    private static final String RECURRING_CHANNEL_NAME = "Nhắc thanh toán định kỳ";

    private static final String SETTINGS_PREFS = "settings";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";

    private static final int DAILY_REMINDER_REQUEST_CODE = 100;
    private static final int OPEN_APP_REQUEST_CODE = 101;
    private static final int RECURRING_CHECK_REQUEST_CODE = 102;
    private static final int OPEN_NOTIFICATIONS_REQUEST_CODE = 103;
    private static final int DAILY_NOTIFICATION_ID = 200;
    private static final int RECURRING_NOTIFICATION_ID = 201;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();

        if (ACTION_RECURRING_PAYMENT_CHECK.equals(action)) {
            showRecurringPaymentNotification(context);
            scheduleRecurringPaymentCheck(context.getApplicationContext());
            return;
        }

        showDailyReminderNotification(context);

        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_REMINDER_ENABLED, false)) {
            int hour = prefs.getInt(KEY_REMINDER_HOUR, 21);
            int minute = prefs.getInt(KEY_REMINDER_MINUTE, 0);
            scheduleReminder(context.getApplicationContext(), hour, minute);
        }
    }

    public static void scheduleReminder(Context context, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (!calendar.after(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        scheduleAlarm(alarmManager, calendar.getTimeInMillis(), createDailyReminderPendingIntent(context));
    }

    public static void scheduleRecurringPaymentCheck(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (!calendar.after(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        scheduleAlarm(alarmManager, calendar.getTimeInMillis(), createRecurringCheckPendingIntent(context));
    }

    public static void cancelReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(createDailyReminderPendingIntent(context));
        }
    }

    private static void scheduleAlarm(AlarmManager alarmManager, long triggerAtMillis,
                                      PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    private static PendingIntent createDailyReminderPendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_DAILY_REMINDER);
        return PendingIntent.getBroadcast(
                context,
                DAILY_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent createRecurringCheckPendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_RECURRING_PAYMENT_CHECK);
        return PendingIntent.getBroadcast(
                context,
                RECURRING_CHECK_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent createOpenAppPendingIntent(Context context) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launchIntent == null) {
            launchIntent = new Intent(context, SplashActivity.class);
        }
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return PendingIntent.getActivity(
                context,
                OPEN_APP_REQUEST_CODE,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent createOpenNotificationsPendingIntent(Context context) {
        Intent intent = new Intent(context, NotificationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                OPEN_NOTIFICATIONS_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void showDailyReminderNotification(Context context) {
        if (!canPostNotifications(context)) {
            return;
        }

        NotificationManager notificationManager = getNotificationManager(context);
        if (notificationManager == null) {
            return;
        }

        createChannelIfNeeded(
                notificationManager,
                DAILY_CHANNEL_ID,
                DAILY_CHANNEL_NAME,
                "Nhắc bạn ghi lại thu chi hằng ngày"
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DAILY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_24)
                .setContentTitle("Nhắc nhở ghi chép")
                .setContentText("Đến giờ ghi lại các khoản thu chi hôm nay rồi.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Đến giờ ghi lại các khoản thu chi hôm nay rồi. Ghi ngay để không quên giao dịch trong ngày nhé."))
                .setContentIntent(createOpenAppPendingIntent(context))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        notificationManager.notify(DAILY_NOTIFICATION_ID, builder.build());
    }

    private void showRecurringPaymentNotification(Context context) {
        if (!canPostNotifications(context)) {
            return;
        }

        List<RecurringTransactionResponse> cachedItems =
                RecurringNotificationManager.getCachedItems(context);
        List<RecurringNotificationManager.AlertItem> alerts =
                RecurringNotificationManager.getActiveAlerts(cachedItems);

        if (alerts.isEmpty()) {
            return;
        }

        int urgentCount = 0;
        for (RecurringNotificationManager.AlertItem alert : alerts) {
            if (alert.isOverdue() || alert.isDueToday()) {
                urgentCount++;
            }
        }

        RecurringNotificationManager.AlertItem first = alerts.get(0);
        String firstName = RecurringNotificationManager.safeDescription(first.getTransaction());
        String title = urgentCount > 0
                ? "Có " + urgentCount + " khoản đã hoặc đang đến hạn"
                : "Có " + alerts.size() + " khoản sắp đến hạn";
        String content = firstName + " - " + first.getStatusText();

        NotificationManager notificationManager = getNotificationManager(context);
        if (notificationManager == null) {
            return;
        }

        createChannelIfNeeded(
                notificationManager,
                RECURRING_CHANNEL_ID,
                RECURRING_CHANNEL_NAME,
                "Thông báo khoản thanh toán định kỳ sắp đến hạn hoặc quá hạn"
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RECURRING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_24)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        content + (alerts.size() > 1
                                ? ". Còn " + (alerts.size() - 1) + " khoản khác cần kiểm tra."
                                : ".")))
                .setContentIntent(createOpenNotificationsPendingIntent(context))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setNumber(alerts.size());

        notificationManager.notify(RECURRING_NOTIFICATION_ID, builder.build());
    }

    private boolean canPostNotifications(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void createChannelIfNeeded(NotificationManager notificationManager,
                                       String channelId,
                                       String channelName,
                                       String description) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
