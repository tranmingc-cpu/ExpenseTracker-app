package com.example.expensetracker_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.Drawable;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;

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
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        progressBar = findViewById(R.id.progressBar);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        setupPasswordToggle(etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);

        mAuth = FirebaseAuth.getInstance();

        String emailFromRegister = getIntent().getStringExtra("email");
        if (emailFromRegister != null && !emailFromRegister.trim().isEmpty()) {
            etEmail.setText(emailFromRegister.trim());
            etPassword.requestFocus();
        }

        String webClientId = getDefaultWebClientId();

        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();

        if (webClientId != null && !webClientId.isEmpty()) {
            gsoBuilder.requestIdToken(webClientId);
        } else {
            Log.e(TAG, "default_web_client_id resource not found. Google Sign-In is not configured for this Firebase project.");
        }

        GoogleSignInOptions gso = gsoBuilder.build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleSignIn.setOnClickListener(v -> signIn());
        btnLogin.setOnClickListener(v -> handleLocalLogin());
        tvRegisterLink.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));
    }

    private void handleLocalLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email và mật khẩu.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            if (!user.isEmailVerified()) {
                                showLoading(false);
                                Toast.makeText(
                                        LoginActivity.this,
                                        "Tài khoản của bạn chưa được xác thực email. Vui lòng kiểm tra hộp thư để xác thực trước khi đăng nhập.",
                                        Toast.LENGTH_LONG
                                ).show();
                                mAuth.signOut();
                                return;
                            }
                            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                if (tokenTask.isSuccessful()) {
                                    String firebaseIdToken = tokenTask.getResult().getToken();
                                    sendTokenToBackend(firebaseIdToken);
                                } else {
                                    showLoading(false);
                                    Log.e(TAG, "Cannot get Firebase token", tokenTask.getException());
                                    Toast.makeText(
                                            LoginActivity.this,
                                            "Đăng nhập chưa thành công. Vui lòng thử lại sau.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                        } else {
                            showLoading(false);
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Đăng nhập chưa thành công. Vui lòng thử lại sau.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    } else {
                        showLoading(false);
                        Log.e(TAG, "Firebase email/password login failed", task.getException());
                        Toast.makeText(
                                LoginActivity.this,
                                getVietnameseLoginError(task.getException()),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private String getVietnameseLoginError(Exception exception) {
        if (exception == null || exception.getMessage() == null) {
            return "Đăng nhập thất bại. Vui lòng thử lại.";
        }

        String error = exception.getMessage().toLowerCase();

        if (error.contains("auth credential")
                || error.contains("password is invalid")
                || error.contains("invalid login credentials")
                || error.contains("malformed")
                || error.contains("expired")
                || error.contains("supplied auth credential")) {
            return "Email hoặc mật khẩu không đúng.";
        }

        if (error.contains("no user record")
                || error.contains("user-not-found")
                || error.contains("user may have been deleted")) {
            return "Tài khoản không tồn tại.";
        }

        if (error.contains("badly formatted")
                || error.contains("invalid email")) {
            return "Email không đúng định dạng.";
        }

        if (error.contains("network")
                || error.contains("timeout")
                || error.contains("connection")) {
            return "Không thể kết nối mạng. Vui lòng thử lại sau.";
        }

        if (error.contains("too many")
                || error.contains("blocked")) {
            return "Bạn đăng nhập sai quá nhiều lần. Vui lòng thử lại sau.";
        }

        return "Đăng nhập thất bại. Vui lòng kiểm tra email hoặc mật khẩu.";
    }

    private void signIn() {
        String webClientId = getDefaultWebClientId();

        if (webClientId == null || webClientId.isEmpty()) {
            Toast.makeText(
                    this,
                    "Đăng nhập Google chưa sẵn sàng. Vui lòng thử lại sau.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private String getDefaultWebClientId() {
        int resId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        if (resId == 0) {
            return "";
        }
        return getString(resId);
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
                Toast.makeText(this, "Đăng nhập Google không thành công. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            Toast.makeText(this, "Đăng nhập Google không thành công. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                if (tokenTask.isSuccessful()) {
                                    String firebaseIdToken = tokenTask.getResult().getToken();
                                    sendTokenToBackend(firebaseIdToken);
                                } else {
                                    showLoading(false);
                                    Log.e(TAG, "Cannot get Firebase token after Google login", tokenTask.getException());
                                    Toast.makeText(LoginActivity.this, "Đăng nhập chưa thành công. Vui lòng thử lại sau.", Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {
                            showLoading(false);
                            Toast.makeText(LoginActivity.this, "Đăng nhập chưa thành công. Vui lòng thử lại sau.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        showLoading(false);
                        Log.e(TAG, "Firebase Google auth failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Đăng nhập Google không thành công. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
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

                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                            // Chuyển sang HomeActivity
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                            finish();
                        } else {
                            Log.e(TAG, "Backend error: " + response.code());
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Đăng nhập chưa thành công. Vui lòng thử lại sau.",
                                    Toast.LENGTH_LONG
                            ).show();
                            mAuth.signOut();
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "API Connection failed", t);
                        Toast.makeText(
                                LoginActivity.this,
                                "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng và thử lại sau.",
                                Toast.LENGTH_LONG
                        ).show();
                        mAuth.signOut();
                    }
                });
    }

    private void setupPasswordToggle(EditText editText) {
        final boolean[] isVisible = {false};

        editText.setMinHeight(0);
        editText.setGravity(android.view.Gravity.CENTER_VERTICAL);
        updatePasswordToggleIcon(editText, isVisible[0]);

        editText.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = editText.getCompoundDrawables()[2];

                if (drawableEnd != null) {
                    int iconStart = editText.getWidth()
                            - editText.getPaddingRight()
                            - drawableEnd.getBounds().width()
                            - dp(12);

                    if (event.getX() >= iconStart) {
                        isVisible[0] = !isVisible[0];

                        int cursorPosition = editText.getSelectionStart();
                        if (isVisible[0]) {
                            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        } else {
                            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        }

                        updatePasswordToggleIcon(editText, isVisible[0]);
                        editText.setSelection(Math.max(0, Math.min(cursorPosition, editText.length())));
                        event.setAction(MotionEvent.ACTION_CANCEL);
                        return true;
                    }
                }
            }
            return false;
        });
    }



    private void updatePasswordToggleIcon(EditText editText, boolean isVisible) {
        int iconRes = isVisible ? R.drawable.ic_visibility_24 : R.drawable.ic_visibility_off_24;
        Drawable icon = androidx.core.content.ContextCompat.getDrawable(this, iconRes);
        if (icon != null) {
            int iconSize = dp(20);
            icon.setBounds(0, 0, iconSize, iconSize);
        }

        editText.setCompoundDrawables(null, null, icon, null);
        editText.setCompoundDrawablePadding(dp(6));
        editText.setPadding(dp(14), 0, dp(42), 0);
    }



    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnGoogleSignIn.setEnabled(false);
            btnGoogleSignIn.setAlpha(0.6f);
            btnLogin.setEnabled(false);
            btnLogin.setAlpha(0.6f);
        } else {
            progressBar.setVisibility(View.GONE);
            btnGoogleSignIn.setEnabled(true);
            btnGoogleSignIn.setAlpha(1.0f);
            btnLogin.setEnabled(true);
            btnLogin.setAlpha(1.0f);
        }
    }
}