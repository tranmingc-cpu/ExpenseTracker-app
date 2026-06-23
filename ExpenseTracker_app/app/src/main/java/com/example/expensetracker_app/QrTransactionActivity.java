package com.example.expensetracker_app;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class QrTransactionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setText("Chức năng thanh toán QR đang được hoàn thiện.");
        textView.setTextSize(18);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(32, 32, 32, 32);

        setContentView(textView);
    }
}
