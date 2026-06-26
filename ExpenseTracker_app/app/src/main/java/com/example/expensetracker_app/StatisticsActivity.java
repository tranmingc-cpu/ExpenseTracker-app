package com.example.expensetracker_app;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker_app.adapter.TopCategoryAdapter;
import com.example.expensetracker_app.model.TopCategory;
import com.expensetracker_manager.model.response.TransactionResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsActivity extends AppCompatActivity {

    private TextView tvIncome;
    private TextView tvExpense;
    private TextView tvBalance;
    private TextView tvInsight;

    private ProgressBar progressIncome;
    private ProgressBar progressExpense;

    private RecyclerView rvTopCategory;

    private TopCategoryAdapter adapter;
    private final List<TopCategory> topCategories = new ArrayList<>();

    private final NumberFormat moneyFormat =
            NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);
        tvBalance = findViewById(R.id.tvBalance);
        tvInsight = findViewById(R.id.tvInsight);

        progressIncome = findViewById(R.id.progressIncome);
        progressExpense = findViewById(R.id.progressExpense);

        rvTopCategory = findViewById(R.id.rvTopCategory);

        rvTopCategory.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TopCategoryAdapter(topCategories);
        rvTopCategory.setAdapter(adapter);

        loadStatistics();
    }

    private void loadStatistics() {

        Long userId = TokenManager.getInstance(this).getUserId();

        if (userId == -1L) {
            Toast.makeText(this, "Không tìm thấy User", Toast.LENGTH_SHORT).show();
            return;
        }

        RetrofitClient.getInstance()
                .getTransactionApi()
                .getByUser(userId)
                .enqueue(new Callback<List<TransactionResponse>>() {

                    @Override
                    public void onResponse(Call<List<TransactionResponse>> call,
                                           Response<List<TransactionResponse>> response) {

                        if (response.isSuccessful() && response.body() != null) {
                            calculateStatistics(response.body());
                        } else {
                            Toast.makeText(StatisticsActivity.this,
                                    "Không có dữ liệu.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<TransactionResponse>> call, Throwable t) {

                        Toast.makeText(
                                StatisticsActivity.this,
                                "Lỗi: " + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    }
                });

    }

    private void calculateStatistics(List<TransactionResponse> list) {

        double income = 0;
        double expense = 0;

        Map<String, Double> categoryMap = new HashMap<>();

        for (TransactionResponse tr : list) {

            if ("INCOME".equalsIgnoreCase(tr.getType())) {

                income += tr.getAmount();

            } else {

                expense += tr.getAmount();

                String category = tr.getCategoryName();

                if (category == null || category.isEmpty()) {
                    category = "Khác";
                }

                double old = categoryMap.getOrDefault(category, 0.0);

                categoryMap.put(category, old + tr.getAmount());

            }

        }

        double balance = income - expense;

        tvIncome.setText(moneyFormat.format(income));
        tvExpense.setText(moneyFormat.format(expense));
        tvBalance.setText(moneyFormat.format(balance));

        int total = (int) (income + expense);

        if (total > 0) {

            progressIncome.setProgress((int) (income * 100 / total));

            progressExpense.setProgress((int) (expense * 100 / total));

        } else {

            progressIncome.setProgress(0);
            progressExpense.setProgress(0);

        }

        topCategories.clear();

        for (Map.Entry<String, Double> item : categoryMap.entrySet()) {

            topCategories.add(
                    new TopCategory(
                            item.getKey(),
                            item.getValue()
                    )
            );

        }

        Collections.sort(topCategories, new Comparator<TopCategory>() {
            @Override
            public int compare(TopCategory o1, TopCategory o2) {
                return Double.compare(o2.getAmount(), o1.getAmount());
            }
        });

        if (topCategories.size() > 5) {
            topCategories.subList(5, topCategories.size()).clear();
        }

        adapter.notifyDataSetChanged();

        generateInsight(income, expense);

    }

    private void generateInsight(double income, double expense) {

        if (income == 0) {

            tvInsight.setText("Chưa có dữ liệu thu nhập.");

            return;

        }

        double percent = expense / income * 100;

        String text;

        if (percent >= 90) {

            text = "⚠ Bạn đã sử dụng hơn 90% thu nhập. Hãy hạn chế chi tiêu.";

        } else if (percent >= 70) {

            text = "⚠ Chi tiêu đang ở mức cao (" + (int) percent + "%).";

        } else if (percent >= 50) {

            text = "✓ Chi tiêu ở mức trung bình.";

        } else {

            text = "🎉 Bạn đang quản lý tài chính rất tốt.";

        }

        tvInsight.setText(text);

    }

}
