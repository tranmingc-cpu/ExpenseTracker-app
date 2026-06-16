package com.example.expensetracker_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.expensetracker_manager.model.request.TransactionRequest;
import com.expensetracker_manager.model.response.CategoryResponse;
import com.expensetracker_manager.model.response.TransactionResponse;
import com.expensetracker_manager.model.response.WalletResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;

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

        setupSpinners();
        loadCategories();
        loadWallets();

        etDescription.setHint("Ghi chú / Mô tả (Không bắt buộc)");

        btnOcrScan.setOnClickListener(v -> runMockOcrScan());
        btnSaveTransaction.setOnClickListener(v -> saveTransaction());
    }

    private void setupSpinners() {
        String[] types = { "EXPENSE", "INCOME" };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                types);
        spinnerType.setAdapter(typeAdapter);
    }

    private void loadCategories() {
        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            // Offline fallback for categories
            List<String> names = new ArrayList<>();
            names.add("Ăn uống");
            names.add("Đi lại");
            names.add("Quần áo");
            names.add("Đi chơi");
            names.add("Y tế");
            names.add("Khác");
            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(AddTransactionActivity.this,
                    android.R.layout.simple_spinner_dropdown_item, names);
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
                                names.add(cat.getName());
                            }
                            if (names.isEmpty()) {
                                names.add("Ăn uống");
                                names.add("Đi lại");
                                names.add("Quần áo");
                                names.add("Đi chơi");
                                names.add("Y tế");
                                names.add("Khác");
                            }
                            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(AddTransactionActivity.this,
                                     android.R.layout.simple_spinner_dropdown_item, names);
                            spinnerCategory.setAdapter(catAdapter);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<CategoryResponse>> call, Throwable t) {
                        // Offline fallbacks
                        List<String> names = new ArrayList<>();
                        names.add("Ăn uống");
                        names.add("Đi lại");
                        names.add("Quần áo");
                        names.add("Đi chơi");
                        names.add("Y tế");
                        names.add("Khác");
                        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(AddTransactionActivity.this,
                                android.R.layout.simple_spinner_dropdown_item, names);
                        spinnerCategory.setAdapter(catAdapter);
                    }
                });
    }

    private void loadWallets() {
        Long userId = TokenManager.getInstance(this).getUserId();
        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            List<String> names = new ArrayList<>();
            names.add("Ví tiền mặt");
            names.add("Tài khoản ngân hàng");
            ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(AddTransactionActivity.this,
                    android.R.layout.simple_spinner_dropdown_item, names);
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
                            ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(AddTransactionActivity.this,
                                     android.R.layout.simple_spinner_dropdown_item, names);
                            spinnerWallet.setAdapter(walletAdapter);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<WalletResponse>> call, Throwable t) {
                        // Offline fallbacks
                        List<String> names = new ArrayList<>();
                        names.add("Ví tiền mặt");
                        names.add("Tài khoản ngân hàng");
                        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(AddTransactionActivity.this,
                                android.R.layout.simple_spinner_dropdown_item, names);
                        spinnerWallet.setAdapter(walletAdapter);
                    }
                });
    }

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_PICK = 102;

    private void runMockOcrScan() {
        String[] options = { "Chụp ảnh (Camera)", "Chọn ảnh từ thư viện (Gallery)" };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Quét Hóa Đơn AI (OCR)")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Launch Camera Intent
                        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                        } else {
                            try {
                                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                            } catch (Exception e) {
                                Toast.makeText(this, "Không thể mở ứng dụng Camera.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        // Launch Gallery Intent
                        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && (requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_PICK)) {
            // Start mock OCR processing
            btnOcrScan.setEnabled(false);
            ocrProgressBar.setVisibility(View.VISIBLE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                ocrProgressBar.setVisibility(View.GONE);
                btnOcrScan.setEnabled(true);

                // Populate fields with mock scanned details - ONLY amount is set automatically
                etAmount.setText("185000");

                Toast.makeText(AddTransactionActivity.this,
                        "Xử lý OCR hóa đơn thành công! Đã nhận dạng số tiền: 185.000đ. Vui lòng tự nhập các thông tin khác.", Toast.LENGTH_LONG).show();
            }, 2000);
        }
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
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
}
