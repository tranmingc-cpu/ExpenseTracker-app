package com.example.expensetracker_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    private static final String SETTINGS_PREFS = "settings";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        ReminderReceiver.scheduleRecurringPaymentCheck(context.getApplicationContext());

        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_REMINDER_ENABLED, false)) {
            int hour = prefs.getInt(KEY_REMINDER_HOUR, 21);
            int minute = prefs.getInt(KEY_REMINDER_MINUTE, 0);
            ReminderReceiver.scheduleReminder(context.getApplicationContext(), hour, minute);
        }
    }
}
