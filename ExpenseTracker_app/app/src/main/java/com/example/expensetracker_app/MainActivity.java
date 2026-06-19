package com.example.expensetracker_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    // Khai báo ID kênh và ID thông báo
    private static final String CHANNEL_ID = "expense_reminder_channel";
    private static final int NOTIFICATION_ID = 101;
    private static final int REQUEST_PERMISSION_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Đảm bảo đúng file layout XML của bạn

        // 1. Tạo kênh thông báo ngay khi mở app
        createNotificationChannel();

        // 2. Xin quyền từ người dùng (Bắt buộc từ Android 13+)
        checkAndRequestPermission();

        // 3. ĐOẠN DÙNG ĐỂ TEST: Tự động bắn thông báo sau khi mở app 3 giây để kiểm tra
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showExpenseReminder();
            }
        }, 3000);
    }

    // --- HÀM 1: TẠO KÊNH THÔNG BÁO (NOTIFICATION CHANNEL) ---
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Nhắc nhở chi tiêu";
            String description = "Kênh gửi thông báo nhắc nhở nhập chi tiêu hàng ngày";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // --- HÀM 2: XIN QUYỀN HIỂN THỊ THÔNG BÁO ---
    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_PERMISSION_CODE
                );
            }
        }
    }

    // --- HÀM 3: KHỞI TẠO VÀ HIỂN THỊ THÔNG BÁO ---
    public void showExpenseReminder() {
        // Intent để khi click vào thông báo sẽ mở lại app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Cấu hình giao diện thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // Sử dụng tạm icon mặc định của App để không bị crash
                .setContentTitle("Ví của bạn đang khóc kìa! 💸")
                .setContentText("Cuối ngày rồi, đừng quên nhập các khoản chi tiêu hôm nay vào app nhé!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // Click vào tự mất thông báo

        // Hiển thị thông báo
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}