package com.expensetracker.expensetracker_api.service;

import com.expensetracker.expensetracker_api.dto.request.FirebaseLoginRequest;
import com.expensetracker.expensetracker_api.dto.request.LoginRequest;
import com.expensetracker.expensetracker_api.dto.request.RegisterRequest;
import com.expensetracker.expensetracker_api.dto.response.AuthResponse;
import com.expensetracker.expensetracker_api.entity.AuthProvider;
import com.expensetracker.expensetracker_api.entity.UserEntity;
import com.expensetracker.expensetracker_api.exception.ResourceNotFoundException;
import com.expensetracker.expensetracker_api.repository.UserRepository;
import com.expensetracker.expensetracker_api.security.JwtTokenProvider;
import com.expensetracker.expensetracker_api.entity.CategoryEntity;
import com.expensetracker.expensetracker_api.entity.WalletEntity;
import com.expensetracker.expensetracker_api.repository.CategoryRepository;
import com.expensetracker.expensetracker_api.repository.WalletRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        UserEntity user = new UserEntity();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setPhoneNumber(request.getPhoneNumber());

        user.setProvider(AuthProvider.LOCAL);
        user.setEmailVerified(false);

        userRepository.save(user);

        seedDefaultCategories(user);
        seedDefaultWallet(user);

        String jwtToken = jwtTokenProvider.generateToken(user);
        return AuthResponse.builder()
                .message("Register successful")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .jwtToken(jwtToken)
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        boolean match = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!match) {
            throw new RuntimeException("Wrong password");
        }

        seedDefaultCategories(user);
        seedDefaultWallet(user);

        String jwtToken = jwtTokenProvider.generateToken(user);
        return AuthResponse.builder()
                .message("Login successful")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .jwtToken(jwtToken)
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public AuthResponse firebaseLogin(FirebaseLoginRequest request) {
        try {
            // a. Verify Firebase ID Token
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
            String firebaseUid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String picture = decodedToken.getPicture();

            if (email == null) {
                throw new RuntimeException("Email not provided by Firebase token");
            }

            UserEntity user;

            // b. Tìm User theo firebaseUid
            var userByUidOpt = userRepository.findByFirebaseUid(firebaseUid);

            if (userByUidOpt.isPresent()) {
                // c. Nếu tồn tại thì cập nhật thông tin
                user = userByUidOpt.get();
                boolean updated = false;
                if (name != null && !name.equals(user.getFullName())) {
                    user.setFullName(name);
                    updated = true;
                }
                if (picture != null && !picture.equals(user.getAvatarUrl())) {
                    user.setAvatarUrl(picture);
                    updated = true;
                }
                if (updated) {
                    user = userRepository.save(user);
                }
            } else {
                // d. Nếu chưa tồn tại thì tìm theo email
                var userByEmailOpt = userRepository.findByEmail(email);

                if (userByEmailOpt.isPresent()) {
                    // e. Nếu email tồn tại thì liên kết firebaseUid với tài khoản đó
                    user = userByEmailOpt.get();
                    user.setFirebaseUid(firebaseUid);
                    user.setProvider(AuthProvider.GOOGLE);
                    if (picture != null) {
                        user.setAvatarUrl(picture);
                    }
                    user = userRepository.save(user);
                } else {
                    // f. Nếu không tồn tại thì tạo User mới
                    user = new UserEntity();
                    user.setFullName(name != null ? name : "Google User");
                    user.setEmail(email);
                    // Sinh password ngẫu nhiên được mã hóa
                    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    user.setFirebaseUid(firebaseUid);
                    user.setProvider(AuthProvider.GOOGLE);
                    user.setAvatarUrl(picture);
                    user.setEmailVerified(true);
                    user = userRepository.save(user);
                }
            }

            seedDefaultCategories(user);
            seedDefaultWallet(user);

            // g. Sinh JWT và trả về AuthResponse
            String jwtToken = jwtTokenProvider.generateToken(user);

            return AuthResponse.builder()
                    .message("Firebase login successful")
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .avatarUrl(user.getAvatarUrl())
                    .jwtToken(jwtToken)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Firebase token verification failed: " + e.getMessage(), e);
        }
    }

    public AuthResponse forgotPassword(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        String code = String.format("%06d", new java.util.Random().nextInt(1000000));
        user.setResetToken(passwordEncoder.encode(code));
        user.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        System.out.println("================================================");
        System.out.println("PASSWORD RESET CODE FOR " + email + ": " + code);
        System.out.println("================================================");

        return AuthResponse.builder()
                .message("Reset code generated. Code (for test): " + code)
                .email(email)
                .build();
    }

    public AuthResponse resetPassword(String email, String token, String newPassword) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Reset code has expired");
        }

        if (!passwordEncoder.matches(token, user.getResetToken())) {
            throw new RuntimeException("Invalid reset code");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return AuthResponse.builder()
                .message("Password reset successful")
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    private void seedDefaultCategories(UserEntity user) {
        if (categoryRepository.findByUserId(user.getId()).isEmpty()) {
            String[][] defaultCats = {
                {"Ăn uống", "EXPENSE", "fastfood", "#FF5722"},
                {"Đi lại", "EXPENSE", "directions_car", "#2196F3"},
                {"Quần áo", "EXPENSE", "checkroom", "#9C27B0"},
                {"Chi ngoài", "EXPENSE", "payments", "#E91E63"},
                {"Đi chơi", "EXPENSE", "sports_esports", "#009688"},
                {"Y tế", "EXPENSE", "local_hospital", "#4CAF50"},
                {"Khác", "EXPENSE", "more_horiz", "#607D8B"},
                {"Thu nhập", "INCOME", "attach_money", "#4CAF50"}
            };
            for (String[] cat : defaultCats) {
                CategoryEntity c = new CategoryEntity();
                c.setName(cat[0]);
                c.setType(cat[1]);
                c.setIcon(cat[2]);
                c.setColor(cat[3]);
                c.setUser(user);
                categoryRepository.save(c);
            }
        }
    }

    private void seedDefaultWallet(UserEntity user) {
        if (walletRepository.findByUserId(user.getId()).isEmpty()) {
            WalletEntity wallet = new WalletEntity();
            wallet.setName("Ví tiền mặt");
            wallet.setType("CASH");
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setDescription("Ví tiền mặt mặc định");
            wallet.setUser(user);
            walletRepository.save(wallet);
        }
    }
}