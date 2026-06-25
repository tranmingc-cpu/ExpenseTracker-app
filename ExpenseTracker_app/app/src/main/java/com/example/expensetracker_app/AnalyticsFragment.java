package com.example.expensetracker_app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.expensetracker_manager.model.response.CategoryReportResponse;
import com.expensetracker_manager.model.response.FinancialOverviewResponse;
import com.expensetracker_manager.utils.TokenManager;
import com.expensetracker_manager.viewmodel.AnalyticsViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsFragment extends Fragment {

    private AnalyticsViewModel viewModel;
    private BarChart barChartOverview;
    private PieChart pieChartCategory;
    private Button btnTypeExpense, btnTypeIncome;
    private ProgressBar progressLoading;
    
    private Long userId;
    private String currentCategoryType = "EXPENSE";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        barChartOverview = view.findViewById(R.id.barChartOverview);
        pieChartCategory = view.findViewById(R.id.pieChartCategory);
        btnTypeExpense = view.findViewById(R.id.btnTypeExpense);
        btnTypeIncome = view.findViewById(R.id.btnTypeIncome);
        progressLoading = view.findViewById(R.id.progressLoading);

        setupChartsInit();

        userId = TokenManager.getInstance(requireContext()).getUserId();

        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);

        observeViewModel();

        // Initial load
        if (userId != -1) {
            viewModel.fetchOverview(userId);
            viewModel.fetchCategories(userId, currentCategoryType);
        } else {
            Toast.makeText(requireContext(), "Không tìm thấy thông tin User!", Toast.LENGTH_SHORT).show();
        }

        setupListeners();

        return view;
    }

    private void setupChartsInit() {
        // Bar Chart Styling
        barChartOverview.getDescription().setEnabled(false);
        barChartOverview.setDrawGridBackground(false);
        barChartOverview.setDrawBarShadow(false);

        // TĂNG CỠ CHỮ CHÚ THÍCH (Thu Nhập / Chi Tiêu) lên 14f
        barChartOverview.getLegend().setTextColor(Color.WHITE);
        barChartOverview.getLegend().setTextSize(14f);

        XAxis xAxis = barChartOverview.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(13f);
        xAxis.setGranularity(1f);

        YAxis leftAxis = barChartOverview.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#2A2A3E"));
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setTextSize(13f);

        YAxis rightAxis = barChartOverview.getAxisRight();
        rightAxis.setEnabled(false);

        // Pie Chart Styling
        pieChartCategory.getDescription().setEnabled(false);
        pieChartCategory.setUsePercentValues(true);
        pieChartCategory.setHoleColor(Color.parseColor("#1F1F35"));
        pieChartCategory.setTransparentCircleRadius(61f);

        pieChartCategory.setDrawEntryLabels(false);

        pieChartCategory.getLegend().setTextColor(Color.WHITE);
        pieChartCategory.getLegend().setTextSize(14f);
        pieChartCategory.getLegend().setWordWrapEnabled(true);
    }

    private void setupListeners() {
        btnTypeExpense.setOnClickListener(v -> {
            currentCategoryType = "EXPENSE";
            btnTypeExpense.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F")));
            btnTypeExpense.setTextColor(Color.WHITE);
            btnTypeIncome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2A2A3E")));
            btnTypeIncome.setTextColor(Color.parseColor("#8A8A9E"));
            if (userId != -1) {
                viewModel.fetchCategories(userId, currentCategoryType);
            }
        });

        btnTypeIncome.setOnClickListener(v -> {
            currentCategoryType = "INCOME";
            btnTypeIncome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D32")));
            btnTypeIncome.setTextColor(Color.WHITE);
            btnTypeExpense.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2A2A3E")));
            btnTypeExpense.setTextColor(Color.parseColor("#8A8A9E"));
            if (userId != -1) {
                viewModel.fetchCategories(userId, currentCategoryType);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getOverviewLiveData().observe(getViewLifecycleOwner(), this::updateBarChart);

        viewModel.getCategoryReportLiveData().observe(getViewLifecycleOwner(), this::updatePieChart);
    }

    private void updateBarChart(List<FinancialOverviewResponse> data) {
        if (data == null || data.isEmpty()) {
            barChartOverview.clear();
            return;
        }

        ArrayList<BarEntry> incomeEntries = new ArrayList<>();
        ArrayList<BarEntry> expenseEntries = new ArrayList<>();
        final ArrayList<String> months = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            FinancialOverviewResponse item = data.get(i);
            incomeEntries.add(new BarEntry(i, (float) item.getTotalIncome()));
            expenseEntries.add(new BarEntry(i, (float) item.getTotalExpense()));
            months.add(item.getMonth());
        }

        BarDataSet setIncome = new BarDataSet(incomeEntries, "Thu Nhập");
        setIncome.setColor(Color.parseColor("#00FF66"));
        setIncome.setValueTextColor(Color.WHITE);
        setIncome.setValueTextSize(12f);

        BarDataSet setExpense = new BarDataSet(expenseEntries, "Chi Tiêu");
        setExpense.setColor(Color.parseColor("#FF4081"));
        setExpense.setValueTextColor(Color.WHITE);
        setExpense.setValueTextSize(12f);
        BarData barData = new BarData(setIncome, setExpense);
        barChartOverview.setData(barData);

        barChartOverview.getXAxis().setValueFormatter(new IndexAxisValueFormatter(months));

        // Group configuration
        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        float barWidth = 0.3f;

        barData.setBarWidth(barWidth);
        barChartOverview.groupBars(0f, groupSpace, barSpace);
        barChartOverview.getXAxis().setAxisMinimum(0f);
        barChartOverview.getXAxis().setAxisMaximum(data.size());
        
        barChartOverview.invalidate();
    }

    private void updatePieChart(List<CategoryReportResponse> data) {
        if (data == null || data.isEmpty()) {
            pieChartCategory.clear();
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (CategoryReportResponse item : data) {
            entries.add(new PieEntry((float) item.getPercentage(), item.getCategoryName()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Danh mục");
        
        // Premium colors
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#29B6F6"));
        colors.add(Color.parseColor("#66BB6A"));
        colors.add(Color.parseColor("#FFA726"));
        colors.add(Color.parseColor("#AB47BC"));
        colors.add(Color.parseColor("#EC407A"));
        colors.add(Color.parseColor("#26A69A"));
        colors.add(Color.parseColor("#EF5350"));
        colors.add(Color.parseColor("#5C6BC0"));
        colors.add(Color.parseColor("#8D6E63"));
        dataSet.setColors(colors);
        
        dataSet.setValueLinePart1OffsetPercentage(80.f);
        dataSet.setValueLinePart1Length(0.2f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChartCategory));
        pieData.setValueTextSize(14f);
        pieData.setValueTextColor(Color.WHITE);

        pieChartCategory.setData(pieData);
        pieChartCategory.invalidate();
    }
}
