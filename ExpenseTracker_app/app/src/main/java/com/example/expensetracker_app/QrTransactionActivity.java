package com.example.expensetracker_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.expensetracker_manager.model.request.PaymentLinkRequest;
import com.expensetracker_manager.model.request.TransactionRequest;
import com.expensetracker_manager.model.response.CategoryResponse;
import com.expensetracker_manager.model.response.PaymentLinkResponse;
import com.expensetracker_manager.model.response.TransactionResponse;
import com.expensetracker_manager.model.response.WalletResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QrTransactionActivity extends BaseActivity {

    private Spinner spinnerQrBank;
    private EditText etQrAccountNumber, etQrRecipientName;
    private EditText etQrAmount, etQrDescription;
    private Spinner spinnerQrCategory, spinnerQrWallet;
    private Button btnMomoPayment, btnVietQrPayment, btnSaveQrTransaction;
    private List<CategoryResponse> categories = new ArrayList<>();
    private List<WalletResponse> wallets = new ArrayList<>();

    private final String[] bankNames = {
            "Vietcombank", "Techcombank", "MB Bank", "BIDV", "Agribank",
            "VietinBank", "TPBank", "VPBank", "ACB", "Sacombank",
            "VIB", "OCB", "SHB", "MSB", "HDBank"
    };
    private final String[] bankIds = {
            "VCB", "TCB", "MB", "BIDV", "VBA",
            "ICB", "TPB", "VPB", "ACB", "STB",
            "VIB", "OCB", "SHB", "MSB", "HDB"
    };
    private final String[] bankBins = {
            "970436", // Vietcombank
            "970407", // Techcombank
            "970422", // MB Bank
            "970418", // BIDV
            "970405", // Agribank
            "970415", // VietinBank
            "970423", // TPBank
            "970432", // VPBank
            "970416", // ACB
            "970403", // Sacombank
            "970441", // VIB
            "970448", // OCB
            "970443", // SHB
            "970426", // MSB
            "970437" // HDBank
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_payment_form);

        spinnerQrBank = findViewById(R.id.spinnerQrBank);
        etQrAccountNumber = findViewById(R.id.etQrAccountNumber);
        etQrRecipientName = findViewById(R.id.etQrRecipientName);
        etQrAmount = findViewById(R.id.etQrAmount);
        etQrDescription = findViewById(R.id.etQrDescription);
        spinnerQrCategory = findViewById(R.id.spinnerQrCategory);
        spinnerQrWallet = findViewById(R.id.spinnerQrWallet);
        btnMomoPayment = findViewById(R.id.btnMomoPayment);
        btnVietQrPayment = findViewById(R.id.btnVietQrPayment);
        btnSaveQrTransaction = findViewById(R.id.btnSaveQrTransaction);
        
        etQrAmount.addTextChangedListener(new com.expensetracker_manager.utils.NumberTextWatcher(etQrAmount));

        // Thiết lập danh sách Ngân hàng
        ArrayAdapter<String> bankAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, bankNames);
        spinnerQrBank.setAdapter(bankAdapter);

        loadIntentData();
        loadCategories();
        loadWallets();

        btnMomoPayment.setOnClickListener(v -> handleMomoPayment());
        btnVietQrPayment.setOnClickListener(v -> handleVietQrPayment());
        btnSaveQrTransaction.setOnClickListener(v -> saveTransaction());

        // Thiết lập các trình lắng nghe để tra cứu tự động
        spinnerQrBank.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                triggerAccountLookup();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        etQrAccountNumber.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                triggerAccountLookup();
            }
        });
    }

    private void triggerAccountLookup() {
        int selectedPos = spinnerQrBank.getSelectedItemPosition();
        if (selectedPos < 0 || selectedPos >= bankBins.length) {
            return;
        }
        String accountNumber = etQrAccountNumber.getText().toString().trim();
        if (accountNumber.length() <= 5) {
            return;
        }
        String bin = bankBins[selectedPos];

        etQrRecipientName.setHint("Đang tra cứu...");

        BankLookupService.getInstance().lookupAccount(bin, accountNumber,
                new retrofit2.Callback<BankLookupService.BankLookupResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<BankLookupService.BankLookupResponse> call,
                            retrofit2.Response<BankLookupService.BankLookupResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BankLookupService.BankLookupResponse res = response.body();
                            if ("00".equals(res.getCode()) && res.getData() != null
                                    && res.getData().getAccountName() != null) {
                                String accountName = res.getData().getAccountName();
                                etQrRecipientName.setText(accountName);
                                Toast.makeText(QrTransactionActivity.this, "Tra cứu thành công: " + accountName,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                etQrRecipientName.setText("");
                                etQrRecipientName.setHint("Không tìm thấy tài khoản");
                                Toast.makeText(QrTransactionActivity.this, "Không tìm thấy tài khoản: " + res.getDesc(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            etQrRecipientName.setText("");
                            etQrRecipientName.setHint("Lỗi API tra cứu");
                            Toast.makeText(QrTransactionActivity.this, "Lỗi API tra cứu tài khoản", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<BankLookupService.BankLookupResponse> call, Throwable t) {
                        etQrRecipientName.setText("");
                        etQrRecipientName.setHint("Lỗi kết nối");
                        Toast.makeText(QrTransactionActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private void loadIntentData() {
        Intent intent = getIntent();
        if (intent == null)
            return;

        String bankName = intent.getStringExtra("bankName");
        String accountNumber = intent.getStringExtra("accountNumber");
        String recipientName = intent.getStringExtra("recipientName");
        String amount = intent.getStringExtra("amount");
        String memo = intent.getStringExtra("memo");

        etQrAccountNumber.setText(accountNumber == null ? "" : accountNumber);
        etQrRecipientName.setText(recipientName == null ? "" : recipientName);

        if (bankName != null && !bankName.isEmpty()) {
            for (int i = 0; i < bankNames.length; i++) {
                if (bankNames[i].equalsIgnoreCase(bankName) || bankIds[i].equalsIgnoreCase(bankName)) {
                    spinnerQrBank.setSelection(i);
                    break;
                }
            }
        }

        if (amount != null && !amount.isEmpty()) {
            etQrAmount.setText(amount);
        }

        StringBuilder defaultDesc = new StringBuilder("Thanh toán QR");
        if (bankName != null && !bankName.isEmpty()) {
            defaultDesc.append(" - ").append(bankName);
        }
        if (memo != null && !memo.isEmpty()) {
            defaultDesc.append(" (").append(memo).append(")");
        }
        etQrDescription.setText(defaultDesc.toString());
    }

    private void handleMomoPayment() {
        String phoneNumber = etQrAccountNumber.getText().toString().trim();
        String amountStr = etQrAmount.getText().toString().trim().replace(".", "");
        String note = etQrDescription.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        PaymentLinkRequest req = new PaymentLinkRequest(phoneNumber, "", phoneNumber, amount, note);

        Toast.makeText(this, "Đang tạo Deeplink MoMo...", Toast.LENGTH_SHORT).show();
        RetrofitClient.getInstance().getPaymentApi().generateLinks(req).enqueue(new Callback<PaymentLinkResponse>() {
                    @Override
                    public void onResponse(Call<PaymentLinkResponse> call, Response<PaymentLinkResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String momoDeeplink = response.body().getMomoDeeplink();
                            try {
                                Intent momoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(momoDeeplink));
                                startActivity(momoIntent);
                                showMomoConfirmationDialog(amount);
                            } catch (Exception e) {
                                Toast.makeText(QrTransactionActivity.this,
                                        "Không thể mở ứng dụng MoMo. Vui lòng cài đặt MoMo.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(QrTransactionActivity.this, "Tạo link MoMo thất bại.", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    @Override
                    public void onFailure(Call<PaymentLinkResponse> call, Throwable t) {
                        Toast.makeText(QrTransactionActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }
    private void showMomoConfirmationDialog(double amount) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận thanh toán")
                .setMessage("Bạn đã hoàn tất thanh toán số tiền " + amount + "đ trên MoMo chưa?")
                .setPositiveButton("Đã thanh toán", (dialog, which) -> saveTransaction())
                .setNegativeButton("Chưa/Hủy", null)
                .setCancelable(false)
                .show();
    }

    private void handleVietQrPayment() {
        int selectedBankPos = spinnerQrBank.getSelectedItemPosition();
        if (selectedBankPos < 0) {
            Toast.makeText(this, "Vui lòng chọn ngân hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        String bankId = bankIds[selectedBankPos];
        String accountNumber = etQrAccountNumber.getText().toString().trim();
        String amountStr = etQrAmount.getText().toString().trim().replace(".", "");
        String note = etQrDescription.getText().toString().trim();

        if (accountNumber.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount = Double.parseDouble(amountStr);
        PaymentLinkRequest req = new PaymentLinkRequest("", bankId, accountNumber, amount, note);
        Toast.makeText(this, "Đang tạo mã VietQR...", Toast.LENGTH_SHORT).show();

        RetrofitClient.getInstance().getPaymentApi().generateLinks(req)
                .enqueue(new Callback<PaymentLinkResponse>() {
                    @Override
                    public void onResponse(Call<PaymentLinkResponse> call, Response<PaymentLinkResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String vietQrUrl = response.body().getVietQrUrl();
                            showVietQrDialog(vietQrUrl);
                        } else {
                            Toast.makeText(QrTransactionActivity.this, "Tạo mã VietQR thất bại.", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    @Override
                    public void onFailure(Call<PaymentLinkResponse> call, Throwable t) {
                        Toast.makeText(QrTransactionActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private void showVietQrDialog(String vietQrUrl) {
        View dialogView = getLayoutInflater().inflate(android.R.layout.activity_list_item, null);
        // Tạo view dialog tùy chỉnh bằng code để tránh thêm file XML
        // và giữ cho code sạch sẽ.
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        ProgressBar prg = new ProgressBar(this);
        prg.setIndeterminate(true);
        layout.addView(prg);

        ImageView img = new ImageView(this);
        img.setLayoutParams(new android.widget.LinearLayout.LayoutParams(600, 600));
        img.setVisibility(View.GONE);
        layout.addView(img);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Mã chuyển khoản VietQR")
                .setView(layout)
                .setPositiveButton("Xác nhận đã chuyển khoản", (d, w) -> saveTransaction())
                .setNegativeButton("Đóng", null)
                .create();

        dialog.show();

        // Tải hình ảnh trong nền
        new Thread(() -> {
            try {
                java.io.InputStream in = new java.net.URL(vietQrUrl).openStream();
                final Bitmap bmp = BitmapFactory.decodeStream(in);
                runOnUiThread(() -> {
                    prg.setVisibility(View.GONE);
                    img.setImageBitmap(bmp);
                    img.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    prg.setVisibility(View.GONE);
                    Toast.makeText(QrTransactionActivity.this, "Không thể tải ảnh QR", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    private void loadCategories() {
        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            loadLocalCategoriesFallback();
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
                                loadLocalCategoriesFallback();
                            } else {
                                ArrayAdapter<String> catAdapter = new ArrayAdapter<>(QrTransactionActivity.this,
                                        android.R.layout.simple_spinner_dropdown_item, names);
                                spinnerQrCategory.setAdapter(catAdapter);
                            }
                        } else {
                            loadLocalCategoriesFallback();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<CategoryResponse>> call, Throwable t) {
                        loadLocalCategoriesFallback();
                    }
                });
    }
    private void loadLocalCategoriesFallback() {
        categories.clear();
        List<String> names = new ArrayList<>();
        names.add("Ăn uống");
        names.add("Đi lại");
        names.add("Quần áo");
        names.add("Đi chơi");
        names.add("Y tế");
        names.add("Chuyển khoản");
        names.add("Khác");
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names);
        spinnerQrCategory.setAdapter(catAdapter);
    }

    private void loadWallets() {
        Long userId = TokenManager.getInstance(this).getUserId();
        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            loadLocalWalletsFallback();
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
                            if (names.isEmpty()) {
                                loadLocalWalletsFallback();
                            } else {
                                ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(QrTransactionActivity.this,
                                        android.R.layout.simple_spinner_dropdown_item, names);
                                spinnerQrWallet.setAdapter(walletAdapter);
                            }
                        } else {
                            loadLocalWalletsFallback();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<WalletResponse>> call, Throwable t) {
                        loadLocalWalletsFallback();
                    }
                });
    }

    private void loadLocalWalletsFallback() {
        wallets.clear();
        List<String> names = new ArrayList<>();
        names.add("Ví tiền mặt");
        names.add("Tài khoản ngân hàng");
        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names);
        spinnerQrWallet.setAdapter(walletAdapter);
    }

    private void saveTransaction() {
        String amountStr = etQrAmount.getText().toString().trim();
        String desc = etQrDescription.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        if (desc.isEmpty()) {
            desc = spinnerQrCategory.getSelectedItem() != null ? spinnerQrCategory.getSelectedItem().toString()
                    : "Giao dịch QR";
        }

        double amt = 0;
        try {
            String cleanAmount = amountStr.replace(".", "");
            amt = Double.parseDouble(cleanAmount);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ, vui lòng kiểm tra lại", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            com.expensetracker_manager.utils.OfflineCacheManager cache = com.expensetracker_manager.utils.OfflineCacheManager
                    .getInstance(this);

            com.expensetracker_manager.model.response.ReportSummaryResponse summary = cache.getCachedReportSummary();
            if (summary != null) {
                summary.setTotalExpense(summary.getTotalExpense() + amt);
                summary.setCurrentBalance(summary.getCurrentBalance() - amt);
                cache.cacheReportSummary(summary);
            }

            List<TransactionResponse> txs = cache.getCachedTransactions();
            if (txs == null)
                txs = new ArrayList<>();

            TransactionResponse newTx = new TransactionResponse();
            newTx.setId(System.currentTimeMillis());
            newTx.setDescription(desc);
            newTx.setAmount(amt);
            newTx.setType("EXPENSE");
            newTx.setCategoryName(
                    spinnerQrCategory.getSelectedItem() != null ? spinnerQrCategory.getSelectedItem().toString()
                            : "Khác");
            newTx.setTransactionDate(LocalDateTime.now().toString());
            txs.add(0, newTx);
            cache.cacheTransactions(txs);

            Toast.makeText(this, "Đã ghi chép giao dịch (Offline) thành công!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TransactionRequest request = new TransactionRequest();
        request.setAmount(amt);
        request.setDescription(desc);
        request.setType("EXPENSE");
        request.setTransactionDate(LocalDateTime.now().toString());
        request.setUserId(TokenManager.getInstance(this).getUserId());

        int selectedCatPos = spinnerQrCategory.getSelectedItemPosition();
        if (selectedCatPos >= 0 && selectedCatPos < categories.size()) {
            request.setCategoryId(categories.get(selectedCatPos).getId());
        }

        RetrofitClient.getInstance().getTransactionApi().create(request)
                .enqueue(new Callback<TransactionResponse>() {
                    @Override
                    public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(QrTransactionActivity.this, "Ghi chép giao dịch thành công!",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(QrTransactionActivity.this, "Không thể lưu giao dịch từ máy chủ.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<TransactionResponse> call, Throwable t) {
                        Toast.makeText(QrTransactionActivity.this, "Lỗi kết nối mạng: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}