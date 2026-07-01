package com.expensetracker_manager.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.expensetracker_manager.model.response.CategoryReportResponse;
import com.expensetracker_manager.model.response.FinancialOverviewResponse;
import com.expensetracker_manager.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalyticsViewModel extends ViewModel {

    private final MutableLiveData<List<FinancialOverviewResponse>> overviewLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryReportResponse>> categoryReportLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public LiveData<List<FinancialOverviewResponse>> getOverviewLiveData() {
        return overviewLiveData;
    }

    public LiveData<List<CategoryReportResponse>> getCategoryReportLiveData() {
        return categoryReportLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void fetchOverview(Long userId) {
        isLoading.setValue(true);
        RetrofitClient.getInstance().getAnalyticsApi().getOverview(userId)
                .enqueue(new Callback<List<FinancialOverviewResponse>>() {
                    @Override
                    public void onResponse(Call<List<FinancialOverviewResponse>> call, Response<List<FinancialOverviewResponse>> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            overviewLiveData.setValue(response.body());
                        } else {
                            errorLiveData.setValue("Failed to load overview data");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<FinancialOverviewResponse>> call, Throwable t) {
                        isLoading.setValue(false);
                        errorLiveData.setValue(t.getMessage());
                    }
                });
    }

    public void fetchCategories(Long userId, String type, Integer month, Integer year) {
        isLoading.setValue(true);
        RetrofitClient.getInstance().getAnalyticsApi().getCategories(userId, type, month, year)
                .enqueue(new Callback<List<CategoryReportResponse>>() {
                    @Override
                    public void onResponse(Call<List<CategoryReportResponse>> call, Response<List<CategoryReportResponse>> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            categoryReportLiveData.setValue(response.body());
                        } else {
                            errorLiveData.setValue("Failed to load category distribution");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<CategoryReportResponse>> call, Throwable t) {
                        isLoading.setValue(false);
                        errorLiveData.setValue(t.getMessage());
                    }
                });
    }
}
