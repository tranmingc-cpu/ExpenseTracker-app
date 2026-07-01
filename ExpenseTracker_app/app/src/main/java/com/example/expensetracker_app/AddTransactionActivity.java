package com.example.expensetracker_app;

import static android.content.Intent.getIntent;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.expensetracker_manager.model.request.TransactionRequest;
import com.expensetracker_manager.model.response.CategoryResponse;
import com.expensetracker_manager.model.response.TransactionResponse;
import com.expensetracker_manager.model.response.WalletResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;

import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTransactionActivity extends BaseActivity {

    private EditText etAmount, etDescription;
    private Spinner spinnerType, spinnerCategory, spinnerWallet;
    private Button btnSaveTransaction, btnOcrScan;
    private ProgressBar ocrProgressBar;
    private List<CategoryResponse> categories = new ArrayList<>();
    private List<WalletResponse> wallets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        etAmount = findViewById(R.id.etAmount);
        etDescription = findViewById(R.id.etDescription);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerWallet = findViewById(R.id.spinnerWallet);
        btnSaveTransaction = findViewById(R.id.btnSaveTransaction);
        btnOcrScan = findViewById(R.id.btnOcrScan);
        ocrProgressBar = findViewById(R.id.ocrProgressBar);

        etAmount.addTextChangedListener(new com.expensetracker_manager.utils.NumberTextWatcher(etAmount));

        setupSpinners();
        loadCategories();
        loadWallets();

        etDescription.setHint("Ghi chú / Mô tả (Không bắt buộc)");

        Intent intent = getIntent();
        if (intent != null) {
            String prefilledAmount = intent.getStringExtra("extra_amount");
            String prefilledDesc = intent.getStringExtra("extra_desc");
            if (prefilledAmount != null && !prefilledAmount.isEmpty()) {
                etAmount.setText(prefilledAmount);
            }
            if (prefilledDesc != null && !prefilledDesc.isEmpty()) {
                etDescription.setText(prefilledDesc);
            }
        }

        btnOcrScan.setOnClickListener(v -> startActivity(new Intent(AddTransactionActivity.this, BillOcrActivity.class)));
        btnSaveTransaction.setOnClickListener(v -> saveTransaction());
    }

    private void setupSpinners() {
        String[] types = { "EXPENSE", "INCOME" };
        ArrayAdapter<String> typeAdapter = createSpinnerAdapter(java.util.Arrays.asList(types));
        spinnerType.setAdapter(typeAdapter);
    }

    private void loadCategories() {
        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            List<String> names = new ArrayList<>();
            names.add("Ăn uống");
            names.add("Đi lại");
            names.add("Quần áo");
            names.add("Đi chơi");
            names.add("Y tế");
            names.add("Chuyển khoản");
            names.add("Khác");
            ArrayAdapter<String> catAdapter = createSpinnerAdapter(names);
            spinnerCategory.setAdapter(catAdapter);
            return;
        }

        RetrofitClient.getInstance().getCategoryApi().getAll()
                .enqueue(new Callback<List<CategoryResponse>>() {
                    @Override
                    public void onResponse(Call<List<CategoryResponse>> call,
                                           Response<List<CategoryResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            categories = response.body();
                            List<String> names = new ArrayList<>();
                            for (CategoryResponse cat : categories) {
                                if (!names.contains(cat.getName())) {
                                    names.add(cat.getName());
                                }
                            }
                            if (!names.contains("Chuyển khoản")) {
                                names.add("Chuyển khoản");
                            }
                            if (names.isEmpty()) {
                                names.add("Ăn uống");
                                names.add("Đi lại");
                                names.add("Quần áo");
                                names.add("Đi chơi");
                                names.add("Y tế");
                                names.add("Khác");
                            }
                            ArrayAdapter<String> catAdapter = createSpinnerAdapter(names);
                            spinnerCategory.setAdapter(catAdapter);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<CategoryResponse>> call, Throwable throwable) {

                    }

                  /* @Override
                    public void onFailure(Call<List<CategoryResponse>> call, Throwable t) {
                        // Offline fallbacks
                        List<String> names = new ArrayList<>();
                        names.add("Ăn uống");
                        names.add("Đi lại");
                        names.add("Quần áo");
                        names.add("Đi chơi");
                        names.add("Y tế");
                        names.add("Khác");
                        ArrayAdapter<String> catAdapter = createSpinnerAdapter(names);
                        spinnerCategory.setAdapter(catAdapter);
                    }*/
                });
    }

    private void loadWallets() {
        Long userId = TokenManager.getInstance(this).getUserId();
        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            List<String> names = new ArrayList<>();
            names.add("Ví tiền mặt");
            names.add("Tài khoản ngân hàng");
            ArrayAdapter<String> walletAdapter = createSpinnerAdapter(names);
            spinnerWallet.setAdapter(walletAdapter);
            return;
        }

        RetrofitClient.getInstance().getWalletApi().getWalletByUser(userId)
                .enqueue(new Callback<List<WalletResponse>>() {
                    @Override
                    public void onResponse(Call<List<WalletResponse>> call, Response<List<WalletResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            wallets = response.body();
                            List<String> names = new ArrayList<>();
                            for (WalletResponse w : wallets) {
                                names.add(w.getName() + " (Số dư: " + w.getBalance() + ")");
                            }
                            if (names.isEmpty())
                                names.add("Ví tiền mặt");
                            ArrayAdapter<String> walletAdapter = createSpinnerAdapter(names);
                            spinnerWallet.setAdapter(walletAdapter);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<WalletResponse>> call, Throwable t) {
                        // Offline fallbacks
                        List<String> names = new ArrayList<>();
                        names.add("Ví tiền mặt");
                        names.add("Tài khoản ngân hàng");
                        ArrayAdapter<String> walletAdapter = createSpinnerAdapter(names);
                        spinnerWallet.setAdapter(walletAdapter);
                    }
                });
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim().replace(".", "");
        String desc = etDescription.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        if (desc.isEmpty()) {
            desc = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "Giao dịch";
        }

        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            com.expensetracker_manager.utils.OfflineCacheManager cache = com.expensetracker_manager.utils.OfflineCacheManager.getInstance(this);
            double amt = Double.parseDouble(amountStr);

            com.expensetracker_manager.model.response.ReportSummaryResponse summary = cache.getCachedReportSummary();
            boolean isIncome = "INCOME".equalsIgnoreCase(spinnerType.getSelectedItem().toString());
            if (isIncome) {
                summary.setTotalIncome(summary.getTotalIncome() + amt);
                summary.setCurrentBalance(summary.getCurrentBalance() + amt);
            } else {
                summary.setTotalExpense(summary.getTotalExpense() + amt);
                summary.setCurrentBalance(summary.getCurrentBalance() - amt);
            }
            cache.cacheReportSummary(summary);

            List<TransactionResponse> txs = cache.getCachedTransactions();
            TransactionResponse newTx = new TransactionResponse();
            newTx.setId(System.currentTimeMillis());
            newTx.setDescription(desc);
            newTx.setAmount(amt);
            newTx.setType(spinnerType.getSelectedItem().toString());
            newTx.setCategoryName(spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "Khác");
            newTx.setTransactionDate(java.time.LocalDateTime.now().toString());
            txs.add(0, newTx);
            cache.cacheTransactions(txs);

            Toast.makeText(this, "Đã ghi chép giao dịch (Offline) thành công!", Toast.LENGTH_SHORT).show();
            com.expensetracker_manager.service.FinancialAnalysisEngine.analyze(this);
            finish();
            return;
        }

        TransactionRequest request = new TransactionRequest();
        request.setAmount(Double.parseDouble(amountStr));
        request.setDescription(desc);
        request.setType(spinnerType.getSelectedItem().toString());
        request.setTransactionDate(LocalDateTime.now().toString());
        request.setUserId(TokenManager.getInstance(this).getUserId());

        // Get Category ID
        if (!categories.isEmpty()) {
            int selectedCatPos = spinnerCategory.getSelectedItemPosition();
            if (selectedCatPos >= 0 && selectedCatPos < categories.size()) {
                request.setCategoryId(categories.get(selectedCatPos).getId());
            }
        }

        RetrofitClient.getInstance().getTransactionApi().create(request)
                .enqueue(new Callback<TransactionResponse>() {
                    @Override
                    public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AddTransactionActivity.this, "Ghi chép giao dịch thành công!",
                                    Toast.LENGTH_SHORT).show();
                            com.expensetracker_manager.service.FinancialAnalysisEngine.analyze(AddTransactionActivity.this);
                            finish();
                        } else {
                            Toast.makeText(AddTransactionActivity.this, "Không thể lưu giao dịch.", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    @Override
                    public void onFailure(Call<TransactionResponse> call, Throwable t) {
                        Toast.makeText(AddTransactionActivity.this, "Lỗi kết nối mạng: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private ArrayAdapter<String> createSpinnerAdapter(List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.widget.TextView view = (android.widget.TextView) super.getView(position, convertView, parent);
                view.setTextColor(themeColor(R.color.app_text_primary));
                view.setPadding(dp(12), 0, dp(12), 0);
                return view;
            }

            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.widget.TextView view = (android.widget.TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(themeColor(R.color.app_text_primary));
                view.setBackgroundColor(themeColor(R.color.app_surface));
                view.setPadding(dp(12), dp(12), dp(12), dp(12));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }


    private int themeColor(int colorResId) {
        return androidx.core.content.ContextCompat.getColor(this, colorResId);
    }

    private android.graphics.drawable.GradientDrawable roundedBg(int colorResId) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(themeColor(colorResId));
        bg.setCornerRadius(dp(12));
        return bg;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

}