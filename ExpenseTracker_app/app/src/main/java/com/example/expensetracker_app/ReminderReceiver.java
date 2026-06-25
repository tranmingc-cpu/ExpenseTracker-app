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

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "expense_reminder_channel_v3";
    private static final String CHANNEL_NAME = "Nhắc nhở ghi chép";

    private static final String SETTINGS_PREFS = "settings";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";

    private static final int REMINDER_REQUEST_CODE = 100;
    private static final int OPEN_APP_REQUEST_CODE = 101;
    private static final int NOTIFICATION_ID = 200;

    @Override
    public void onReceive(Context context, Intent intent) {
        showReminderNotification(context);

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

        PendingIntent pendingIntent = createReminderPendingIntent(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    public static void cancelReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(createReminderPendingIntent(context));
        }
    }

    private static PendingIntent createReminderPendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("com.example.expensetracker_app.DAILY_REMINDER");
        return PendingIntent.getBroadcast(
                context,
                REMINDER_REQUEST_CODE,
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

    private void showReminderNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Nhắc bạn ghi lại thu chi hằng ngày");
            notificationManager.createNotificationChannel(channel);
        }

        PendingIntent openAppIntent = createOpenAppPendingIntent(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Nhắc nhở ghi chép")
                .setContentText("Đến giờ ghi lại các khoản thu chi hôm nay rồi.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Đến giờ ghi lại các khoản thu chi hôm nay rồi. Ghi ngay để không quên giao dịch trong ngày nhé."))
                .setContentIntent(openAppIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}