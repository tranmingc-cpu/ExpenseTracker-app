package com.example.expensetracker_app;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public class BankLookupService {

    private static final String BASE_URL = "https://api.vietqr.io/";

    // Nhập x-client-id và x-api-key từ VietQR.io vào đây nếu có
    private static final String CLIENT_ID = "202f5a6b-c741-4770-96f3-f5847e30d7bb";
    private static final String API_KEY = "d085df76-df41-40c2-9a00-50d4f3a7495b";

    private static BankLookupService instance;
    private final VietQrApi api;

    private BankLookupService() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(VietQrApi.class);
    }

    public static synchronized BankLookupService getInstance() {
        if (instance == null) {
            instance = new BankLookupService();
        }
        return instance;
    }

    public void lookupAccount(String bin, String accountNumber, Callback<BankLookupResponse> callback) {
        BankLookupRequest request = new BankLookupRequest(bin, accountNumber);
        api.lookupAccount(CLIENT_ID, API_KEY, request).enqueue(callback);
    }

    public interface VietQrApi {
        @POST("v2/lookup")
        Call<BankLookupResponse> lookupAccount(
                @Header("x-client-id") String clientId,
                @Header("x-api-key") String apiKey,
                @Body BankLookupRequest request);
    }

    public static class BankLookupRequest {
        private String bin;
        private String accountNumber;

        public BankLookupRequest(String bin, String accountNumber) {
            this.bin = bin;
            this.accountNumber = accountNumber;
        }

        public String getBin() {
            return bin;
        }

        public void setBin(String bin) {
            this.bin = bin;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }
    }

    public static class BankLookupResponse {
        private String code;
        private String desc;
        private Data data;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }

        public static class Data {
            private String accountName;

            public String getAccountName() {
                return accountName;
            }

            public void setAccountName(String accountName) {
                this.accountName = accountName;
            }
        }
    }
}
