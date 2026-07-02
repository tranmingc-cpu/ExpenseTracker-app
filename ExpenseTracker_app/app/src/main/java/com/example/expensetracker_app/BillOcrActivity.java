package com.example.expensetracker_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.expensetracker_manager.model.request.TransactionRequest;
import com.expensetracker_manager.model.response.CategoryResponse;
import com.expensetracker_manager.model.response.OcrResponse;
import com.expensetracker_manager.model.response.TransactionResponse;
import com.expensetracker_manager.model.response.WalletResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BillOcrActivity extends BaseActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_PICK = 102;
    private TextView tvQrBank, tvQrAccountNumber, tvQrRecipientName;
    private EditText etQrAmount, etQrDescription;
    private Spinner spinnerQrCategory, spinnerQrWallet;
    private Button btnSaveQrTransaction;
    private ProgressBar ocrProgressBar;

    private List<CategoryResponse> categories = new ArrayList<>();
    private List<WalletResponse> wallets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_transaction);

        tvQrBank = findViewById(R.id.tvQrBank);
        tvQrAccountNumber = findViewById(R.id.tvQrAccountNumber);
        tvQrRecipientName = findViewById(R.id.tvQrRecipientName);
        etQrAmount = findViewById(R.id.etQrAmount);
        etQrDescription = findViewById(R.id.etQrDescription);
        spinnerQrCategory = findViewById(R.id.spinnerQrCategory);
        spinnerQrWallet = findViewById(R.id.spinnerQrWallet);
        btnSaveQrTransaction = findViewById(R.id.btnSaveQrTransaction);
        ocrProgressBar = findViewById(R.id.ocrProgressBar);
        
        etQrAmount.addTextChangedListener(new com.expensetracker_manager.utils.NumberTextWatcher(etQrAmount));

        loadCategories();
        loadWallets();

        btnSaveQrTransaction.setOnClickListener(v -> saveTransaction());

        // Mở hộp thoại chọn nguồn ảnh ngay khi vào activity
        showImageSourceDialog();
    }
    private void showImageSourceDialog() {
        String[] options = { "Chụp ảnh (Camera)", "Chọn ảnh từ thư viện (Gallery)" };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Quét Hóa Đơn (OCR)")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        //gọi cam từ android
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        try {
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                        } catch (Exception e) {
                            Toast.makeText(this, "Không thể mở ứng dụng Camera.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // gọi thư viện ảnh
                        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Đã hủy quét hóa đơn.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                Bitmap bitmap = (Bitmap) extras.get("data");
                if (bitmap != null) {
                    processBitmapOcr(bitmap);
                } else {
                    Toast.makeText(this, "Không thể lấy dữ liệu ảnh từ Camera.", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    processUriOcr(selectedImage);
                } else {
                    Toast.makeText(this, "Không thể lấy dữ liệu ảnh từ thư viện.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "Không có hình ảnh nào được chọn.", Toast.LENGTH_SHORT).show();
        }
    }
    private void processBitmapOcr(Bitmap bitmap) {
        try {
            File cacheFile = new File(getCacheDir(), "bill_temp.jpg");
            FileOutputStream fos = new FileOutputStream(cacheFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            uploadImageFile(cacheFile);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi xử lý hình ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processUriOcr(Uri selectedImage) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImage);
            File cacheFile = new File(getCacheDir(), "bill_temp.jpg");
            FileOutputStream fos = new FileOutputStream(cacheFile);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
            fos.close();
            inputStream.close();

            uploadImageFile(cacheFile);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi đọc file ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    // tạo request và gửi xuống cho backend xử lý
    private void uploadImageFile(File file) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        if (ocrProgressBar != null) {
            ocrProgressBar.setVisibility(View.VISIBLE);
        }
        RetrofitClient.getInstance().getTransactionApi().analyzeBill(body)
                .enqueue(new Callback<OcrResponse>() {
                    @Override
                    public void onResponse(Call<OcrResponse> call, Response<OcrResponse> response) {
                        if (ocrProgressBar != null) {
                            ocrProgressBar.setVisibility(View.GONE);
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            OcrResponse ocr = response.body();
                            String bankName = ocr.getBankName();
                            String accountNumber = ocr.getAccountNumber();
                            String accountName = ocr.getAccountName();
                            String amount = ocr.getAmount();
                            String description = ocr.getDescription();

                            tvQrBank.setText(bankName != null && !bankName.trim().isEmpty() ? bankName : "Không xác định");
                            tvQrAccountNumber.setText(accountNumber != null && !accountNumber.trim().isEmpty() ? accountNumber : "Không xác định");
                            tvQrRecipientName.setText(accountName != null && !accountName.trim().isEmpty() ? accountName : "Không xác định");

                            if (amount != null && !amount.trim().isEmpty()) {
                                etQrAmount.setText(amount);
                            } else {
                                etQrAmount.setText("");
                                Toast.makeText(BillOcrActivity.this, "Không tự động tìm thấy số tiền, vui lòng nhập tay.", Toast.LENGTH_SHORT).show();
                            }

                            if (description != null && !description.trim().isEmpty()) {
                                etQrDescription.setText(description);
                            } else {
                                etQrDescription.setText("Chi tiêu từ ảnh biên lai");
                            }
                            Toast.makeText(BillOcrActivity.this, "Đọc hóa đơn thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(BillOcrActivity.this, "Phân tích hóa đơn thất bại từ máy chủ.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<OcrResponse> call, Throwable t) {
                        if (ocrProgressBar != null) {
                            ocrProgressBar.setVisibility(View.GONE);
                        }
                        Toast.makeText(BillOcrActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadCategories() {
        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            loadLocalCategoriesFallback();
            return;
        }

        RetrofitClient.getInstance().getCategoryApi().getAll()
                .enqueue(new Callback<List<CategoryResponse>>() {
                    @Override
                    public void onResponse(Call<List<CategoryResponse>> call, Response<List<CategoryResponse>> response) {
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
                                ArrayAdapter<String> catAdapter = new ArrayAdapter<>(BillOcrActivity.this,
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
                                ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(BillOcrActivity.this,
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
            desc = spinnerQrCategory.getSelectedItem() != null ? spinnerQrCategory.getSelectedItem().toString() : "Giao dịch OCR";
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
            com.expensetracker_manager.utils.OfflineCacheManager cache = com.expensetracker_manager.utils.OfflineCacheManager.getInstance(this);

            com.expensetracker_manager.model.response.ReportSummaryResponse summary = cache.getCachedReportSummary();
            if (summary != null) {
                summary.setTotalExpense(summary.getTotalExpense() + amt);
                summary.setCurrentBalance(summary.getCurrentBalance() - amt);
                cache.cacheReportSummary(summary);
            }

            List<TransactionResponse> txs = cache.getCachedTransactions();
            if (txs == null) txs = new ArrayList<>();

            TransactionResponse newTx = new TransactionResponse();
            newTx.setId(System.currentTimeMillis());
            newTx.setDescription(desc);
            newTx.setAmount(amt);
            newTx.setType("EXPENSE");
            newTx.setCategoryName(spinnerQrCategory.getSelectedItem() != null ? spinnerQrCategory.getSelectedItem().toString() : "Khác");
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
                            Toast.makeText(BillOcrActivity.this, "Ghi chép giao dịch thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(BillOcrActivity.this, "Không thể lưu giao dịch từ máy chủ.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<TransactionResponse> call, Throwable t) {
                        Toast.makeText(BillOcrActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
