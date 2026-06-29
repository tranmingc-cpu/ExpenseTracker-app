package com.example.expensetracker_app;

import android.content.res.ColorStateList;
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
import androidx.core.content.ContextCompat;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        setupListeners();
        refreshTypeButtons();

        userId = TokenManager.getInstance(requireContext()).getUserId();
        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);
        observeViewModel();

        if (userId != -1) {
            viewModel.fetchOverview(userId);
            viewModel.fetchCategories(userId, currentCategoryType);
        } else {
            Toast.makeText(requireContext(), "Không tìm thấy thông tin người dùng!", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void setupChartsInit() {
        int primaryText = color(R.color.app_text_primary);
        int secondaryText = color(R.color.app_text_secondary);
        int divider = color(R.color.app_divider);
        int surface = color(R.color.app_surface);

        barChartOverview.getDescription().setEnabled(false);
        barChartOverview.setDrawGridBackground(false);
        barChartOverview.setDrawBarShadow(false);
        barChartOverview.setBackgroundColor(Color.TRANSPARENT);
        barChartOverview.setNoDataText("Chưa có dữ liệu báo cáo");
        barChartOverview.setNoDataTextColor(secondaryText);

        barChartOverview.getLegend().setTextColor(primaryText);
        barChartOverview.getLegend().setTextSize(14f);

        XAxis xAxis = barChartOverview.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(primaryText);
        xAxis.setTextSize(13f);
        xAxis.setGranularity(1f);

        YAxis leftAxis = barChartOverview.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(divider);
        leftAxis.setTextColor(primaryText);
        leftAxis.setTextSize(13f);

        YAxis rightAxis = barChartOverview.getAxisRight();
        rightAxis.setEnabled(false);

        pieChartCategory.getDescription().setEnabled(false);
        pieChartCategory.setUsePercentValues(true);
        pieChartCategory.setHoleColor(surface);
        pieChartCategory.setTransparentCircleRadius(61f);
        pieChartCategory.setDrawEntryLabels(false);
        pieChartCategory.setNoDataText("Chưa có dữ liệu danh mục");
        pieChartCategory.setNoDataTextColor(secondaryText);

        pieChartCategory.getLegend().setTextColor(primaryText);
        pieChartCategory.getLegend().setTextSize(14f);
        pieChartCategory.getLegend().setWordWrapEnabled(true);
        pieChartCategory.getLegend().setFormToTextSpace(8f);
        pieChartCategory.getLegend().setYEntrySpace(10f);
        pieChartCategory.getLegend().setXEntrySpace(15f);
    }

    private void setupListeners() {
        btnTypeExpense.setOnClickListener(v -> {
            currentCategoryType = "EXPENSE";
            refreshTypeButtons();
            if (userId != -1) {
                viewModel.fetchCategories(userId, currentCategoryType);
            }
        });

        btnTypeIncome.setOnClickListener(v -> {
            currentCategoryType = "INCOME";
            refreshTypeButtons();
            if (userId != -1) {
                viewModel.fetchCategories(userId, currentCategoryType);
            }
        });
    }

    private void refreshTypeButtons() {
        boolean expenseSelected = "EXPENSE".equals(currentCategoryType);

        setButtonState(btnTypeExpense,
                expenseSelected,
                color(R.color.app_accent_expense));

        setButtonState(btnTypeIncome,
                !expenseSelected,
                color(R.color.app_accent_income));
    }

    private void setButtonState(Button button, boolean selected, int selectedColor) {
        int bg = selected ? selectedColor : color(R.color.app_button_secondary);
        int text = selected ? color(R.color.app_button_text) : color(R.color.app_text_secondary);

        button.setBackgroundTintList(ColorStateList.valueOf(bg));
        button.setTextColor(text);
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading ->
                progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE));

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
            barChartOverview.invalidate();
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

        int primaryText = color(R.color.app_text_primary);

        BarDataSet setIncome = new BarDataSet(incomeEntries, "Thu nhập");
        setIncome.setColor(color(R.color.app_accent_income));
        setIncome.setValueTextColor(primaryText);
        setIncome.setValueTextSize(12f);

        BarDataSet setExpense = new BarDataSet(expenseEntries, "Chi tiêu");
        setExpense.setColor(color(R.color.app_accent_expense));
        setExpense.setValueTextColor(primaryText);
        setExpense.setValueTextSize(12f);

        BarData barData = new BarData(setIncome, setExpense);
        barData.setBarWidth(0.3f);
        barChartOverview.setData(barData);
        barChartOverview.getXAxis().setValueFormatter(new IndexAxisValueFormatter(months));

        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        barChartOverview.groupBars(0f, groupSpace, barSpace);
        barChartOverview.getXAxis().setAxisMinimum(0f);
        barChartOverview.getXAxis().setAxisMaximum(data.size());
        barChartOverview.invalidate();
    }

    private void updatePieChart(List<CategoryReportResponse> data) {
        if (data == null || data.isEmpty()) {
            pieChartCategory.clear();
            pieChartCategory.invalidate();
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (CategoryReportResponse item : data) {
            String label = String.format(Locale.US, "%s (%.1f%%)", item.getCategoryName(), item.getPercentage());
            entries.add(new PieEntry((float) item.getPercentage(), label));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(getChartPalette());
        dataSet.setSliceSpace(4f);
        dataSet.setValueLinePart1OffsetPercentage(80.f);
        dataSet.setValueLinePart1Length(0.2f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChartCategory));
        pieData.setValueTextSize(14f);
        pieData.setValueTextColor(color(R.color.app_text_primary));

        pieChartCategory.setData(pieData);
        pieChartCategory.invalidate();
    }

    private ArrayList<Integer> getChartPalette() {
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
        return colors;
    }

    private int color(int resId) {
        return ContextCompat.getColor(requireContext(), resId);
    }
}