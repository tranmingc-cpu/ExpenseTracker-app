package com.expensetracker.expensetracker_api.service;

import com.expensetracker.expensetracker_api.entity.UserEntity;
import com.expensetracker.expensetracker_api.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseMigrationService {

    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateUsersToFirebase() {
        log.info("Starting migration of existing local users to Firebase Auth...");
        try {
            if (com.google.firebase.FirebaseApp.getApps().isEmpty()) {
                log.warn("Firebase is not initialized. Skipping user migration.");
                return;
            }

            List<UserEntity> unmigratedUsers = userRepository.findAll();
            for (UserEntity user : unmigratedUsers) {
                if (user.getFirebaseUid() == null || user.getFirebaseUid().isBlank()) {
                    log.info("Migrating user: {}", user.getEmail());
                    try {
                        UserRecord userRecord;
                        try {
                            // Check if user already exists in Firebase Auth
                            userRecord = FirebaseAuth.getInstance().getUserByEmail(user.getEmail());
                            log.info("User {} already exists in Firebase Auth. Linking UID: {}", user.getEmail(), userRecord.getUid());
                        } catch (Exception e) {
                            // Create request for Firebase Auth (Bỏ hoàn toàn số điện thoại)
                            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                                    .setEmail(user.getEmail())
                                    .setEmailVerified(user.getEmailVerified() != null ? user.getEmailVerified() : false);

                            if (user.getFullName() != null && !user.getFullName().isBlank()) {
                                createRequest.setDisplayName(user.getFullName().trim());
                            }

                            userRecord = FirebaseAuth.getInstance().createUser(createRequest);
                            log.info("Successfully created user {} in Firebase Auth with UID: {}", user.getEmail(), userRecord.getUid());
                        }

                        user.setFirebaseUid(userRecord.getUid());
                        userRepository.save(user);
                    } catch (FirebaseAuthException fae) {
                        log.error("Firebase Auth error for user {}: Code = {}, Message = {}",
                                user.getEmail(), fae.getAuthErrorCode(), fae.getMessage());
                    } catch (Exception ex) {
                        log.error("Failed to migrate user {}: {}", user.getEmail(), ex.getMessage());
                    }
                }
            }
            log.info("Firebase Auth user migration completed.");
        } catch (Exception e) {
            log.error("Error during user migration: {}", e.getMessage(), e);
        }
    }
}