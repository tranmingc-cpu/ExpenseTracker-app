package com.example.expensetracker_app;

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
// lấy camera từ máy
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap bitmap = null;
            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                bitmap = (Bitmap) extras.get("data");
            }
            else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                Uri selectedImage = data.getData();
                try {
                    InputStream imageStream = getContentResolver().openInputStream(selectedImage);
                    bitmap = BitmapFactory.decodeStream(imageStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            //  Tiến hành quét mã QR từ tấm ảnh mã Bitmap thu được
            if (bitmap != null) {
                processBillOcr(bitmap);
                Toast.makeText(this, "Quét thông tin thành công", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không thể xử lý hình ảnh này.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private String scanQRFromBitmap(Bitmap bitmap) {
        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

        MultiFormatReader reader = new MultiFormatReader();
        try {
            Result result = reader.decode(binaryBitmap);
            return result.getText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private void parseAndFillData(String qrContent) {
        if (qrContent == null || qrContent.trim().isEmpty()) {
            return;
        }

        String cleanContent = qrContent.trim();

        if (cleanContent.matches("\\d+")) {
            etAmount.setText(cleanContent);
            etDescription.setText("Thanh toán qua QR");
            return;
        }

        if (cleanContent.contains("|")) {
            try {
                String[] parts = cleanContent.split("\\|");
                for (String part : parts) {
                    if (part.toLowerCase().startsWith("số tiền :") || part.toLowerCase().startsWith("amount:")) {
                        etAmount.setText(part.substring(part.indexOf(":") + 1).trim());
                    } else if (part.toLowerCase().startsWith("nội dung :") || part.toLowerCase().startsWith("note:")) {
                        etDescription.setText(part.substring(part.indexOf(":") + 1).trim());
                    }
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (cleanContent.startsWith("000201")) {
            try {
                String amountPattern = "54";
                if (cleanContent.contains(amountPattern)) {
                    int index = cleanContent.indexOf(amountPattern);
                    int length = Integer.parseInt(cleanContent.substring(index + 2, index + 4));
                    String amount = cleanContent.substring(index + 4, index + 4 + length);
                    etAmount.setText(amount);
                }

                String infoPattern = "62";
                if (cleanContent.contains(infoPattern)) {
                    int index = cleanContent.indexOf(infoPattern);
                    int length = Integer.parseInt(cleanContent.substring(index + 2, index + 4));
                    String subContent = cleanContent.substring(index + 4, index + 4 + length);
                    if (subContent.contains("08")) {
                        int subIndex = subContent.indexOf("08");
                        int subLength = Integer.parseInt(subContent.substring(subIndex + 2, subIndex + 4));
                        String note = subContent.substring(subIndex + 4, subIndex + 4 + subLength);
                        etDescription.setText(note);
                    } else {
                        etDescription.setText("Quét mã VietQR");
                    }
                } else {
                    etDescription.setText("Quét mã VietQR");
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        etDescription.setText(cleanContent);
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
    private void processBillOcr(Bitmap bitmap) {
        if (bitmap == null) return;

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    // Toàn bộ chữ viết đọc được trên tờ bill sẽ nằm ở đây
                    String fullText = visionText.getText();
                    // Gọi hàm bóc tách thông minh để tìm số tiền và nội dung
                    extractBillDetails(visionText);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Không thể đọc được chữ từ ảnh bill này.", Toast.LENGTH_SHORT).show();
                });
    }

    private void extractBillDetails(Text visionText) {
        String foundAmount = "";
        String foundNote = "";
        List<Text.Line> allLines = new ArrayList<>();

        // Duyệt qua từng đoạn văn bản đọc được trên tờ bill
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            allLines.addAll(block.getLines());
        }
        for (int i = 0; i < allLines.size(); i++) {
            String lineText = allLines.get(i).getText().trim();
            String lowerText = lineText.toLowerCase();
            if (lowerText.contains("số tiền")
                    || lowerText.contains("chuyển tiền")
                    || lowerText.contains("giao dịch")
                    || lowerText.contains("thành công")
                    || lowerText.contains("amount")) {
                // Thử lấy số trên chính dòng hiện tại
                String digits = lineText
                        .replace(".", "")
                        .replace(",", "")
                        .replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    foundAmount = digits;
                }

                // Nếu chưa có thì thử dòng kế tiếp
                if (foundAmount.isEmpty() && i + 1 < allLines.size()) {
                    String nextLine = allLines.get(i + 1).getText();
                    digits = nextLine
                            .replace(".", "")
                            .replace(",", "")
                            .replace(" ", "")
                            .replaceAll("[^0-9]", "");
                    if (!digits.isEmpty()) {
                        foundAmount = digits;
                    }
                }
            }
            else if ((lowerText.contains("vnd")
                    || lowerText.contains("vnđ")
                    || lowerText.contains("đ"))
                    && foundAmount.isEmpty()) {

                String digits = lineText
                        .replace(".", "")
                        .replace(",", "")
                        .replace(" ", "")
                        .replaceAll("[^0-9]", "");
                if (digits.length() >= 3 && !digits.matches("^0+$")) {
                    foundAmount = digits;
                }
            }
            if (lowerText.contains("nội dung")
                    || lowerText.contains("lời nhắn")
                    || lowerText.contains("ghi chú")
                    || lowerText.contains("ndck")) {
                if (lineText.contains(":")) {
                    foundNote = lineText.substring(lineText.indexOf(":") + 1).trim();
                } else {
                    foundNote = lineText
                            .replaceFirst("(?i)nội dung", "")
                            .replaceFirst("(?i)lời nhắn", "")
                            .replaceFirst("(?i)ghi chú", "")
                            .replaceFirst("(?i)ndck", "")
                            .trim();
                    // Nếu sau từ khóa không còn gì thì lấy dòng kế tiếp
                    if (foundNote.isEmpty() && i + 1 < allLines.size()) {
                        foundNote = allLines.get(i + 1).getText().trim();
                    }
                }
            }

            Log.d("OCR_LINE", lineText);
        }

        Log.d("OCR_AMOUNT", foundAmount);
        Log.d("OCR_NOTE", foundNote);
        // Đổ dữ liệu tìm được lên giao diện
        if (!foundAmount.isEmpty()) {
            etAmount.setText(foundAmount);
        } else {
            Toast.makeText(this, "Không tự động tìm thấy số tiền, vui lòng nhập tay.", Toast.LENGTH_SHORT).show();
        }
        if (!foundNote.isEmpty()) {
            etDescription.setText(foundNote);
        } else {
            etDescription.setText("Chi tiêu từ ảnh biên lai");
        }
    }
    private void startQRScanner() {
        // Cấu hình chỉ quét mã QR (bỏ qua các loại mã vạch khác để tăng tốc độ)
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom() // Tự động phóng to nếu mã ở xa
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);

        // Bắt đầu quét
        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    // Khi quét THÀNH CÔNG, thông tin chứa trong mã QR sẽ nằm ở đây
                    String qrContent = barcode.getRawValue();

                    // Hiển thị hoặc xử lý thông tin lấy được
                    Toast.makeText(this, "Nội dung QR: " + qrContent, Toast.LENGTH_LONG).show();

                    // TODO: Phân tích chuỗi qrContent để tự động điền vào ô Số tiền, Ghi chú...
                })
                .addOnFailureListener(e -> {
                    // Khi quét THẤT BẠI hoặc người dùng bấm nút Back thoát ra
                    Toast.makeText(this, "Quét mã thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
