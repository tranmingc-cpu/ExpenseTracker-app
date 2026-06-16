package com.example.expensetracker_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.expensetracker_manager.model.request.FirebaseLoginRequest;
import com.expensetracker_manager.model.response.AuthResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import com.expensetracker_manager.model.request.LoginRequest;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private CardView btnGoogleSignIn;
    private ProgressBar progressBar;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvRegisterLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        progressBar = findViewById(R.id.progressBar);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);

        mAuth = FirebaseAuth.getInstance();

        String webClientId = getString(R.string.default_web_client_id);

        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();

        if (webClientId != null && !webClientId.isEmpty()) {
            gsoBuilder.requestIdToken(webClientId);
        } else {
            Log.e(TAG, "default_web_client_id resource not found. Google Sign-In will not be able to authenticate with the backend.");
        }

        GoogleSignInOptions gso = gsoBuilder.build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLocalLogin();
            }
        });

        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            }
        });
    }

    private void handleLocalLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền Email và Mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        LoginRequest request = new LoginRequest(email, password);
        RetrofitClient.getInstance().getAuthApi().login(request)
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            AuthResponse authResponse = response.body();
                            TokenManager tokenManager = TokenManager.getInstance(LoginActivity.this);
                            tokenManager.saveToken(authResponse.getJwtToken());
                            tokenManager.saveUserInfo(
                                    authResponse.getUserId(),
                                    authResponse.getEmail(),
                                    authResponse.getFullName(),
                                    authResponse.getAvatarUrl()
                            );
                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại. Vui lòng kiểm tra lại email/mật khẩu.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signIn() {
        String webClientId = getString(R.string.default_web_client_id);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        showLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Lấy Firebase ID Token để gửi lên Backend
                            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                if (tokenTask.isSuccessful()) {
                                    String firebaseIdToken = tokenTask.getResult().getToken();
                                    sendTokenToBackend(firebaseIdToken);
                                } else {
                                    showLoading(false);
                                    Toast.makeText(LoginActivity.this, "Không lấy được Firebase Token", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, "Xác thực Firebase thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendTokenToBackend(String firebaseIdToken) {
        FirebaseLoginRequest request = new FirebaseLoginRequest(firebaseIdToken);

        RetrofitClient.getInstance().getAuthApi().firebaseLogin(request)
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            AuthResponse authResponse = response.body();

                            // Lưu JWT Token và thông tin người dùng vào SharedPreferences
                            TokenManager tokenManager = TokenManager.getInstance(LoginActivity.this);
                            tokenManager.saveToken(authResponse.getJwtToken());
                            tokenManager.saveUserInfo(
                                    authResponse.getUserId(),
                                    authResponse.getEmail(),
                                    authResponse.getFullName(),
                                    authResponse.getAvatarUrl()
                            );

                            Toast.makeText(LoginActivity.this, "Đăng nhập Backend thành công!", Toast.LENGTH_SHORT).show();

                            // Chuyển sang HomeActivity
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                            finish();
                        } else {
                            Log.e(TAG, "Backend error: " + response.code());
                            Toast.makeText(LoginActivity.this, "Backend từ chối xác thực. Mã lỗi: " + response.code(), Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "API Connection failed", t);
                        Toast.makeText(LoginActivity.this, "Không kết nối được với Server API: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                    }
                });
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnGoogleSignIn.setEnabled(false);
            btnGoogleSignIn.setAlpha(0.6f);
        } else {
            progressBar.setVisibility(View.GONE);
            btnGoogleSignIn.setEnabled(true);
            btnGoogleSignIn.setAlpha(1.0f);
        }
    }
}
