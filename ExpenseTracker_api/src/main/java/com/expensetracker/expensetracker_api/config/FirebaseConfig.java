package com.expensetracker.expensetracker_api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                String firebaseCredentials = System.getenv("FIREBASE_CREDENTIALS");
                FirebaseOptions options = null;
                if (firebaseCredentials != null && !firebaseCredentials.trim().isEmpty()) {
                    InputStream credentialsStream = new ByteArrayInputStream(
                            firebaseCredentials.getBytes(StandardCharsets.UTF_8)
                    );
                    options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                            .build();
                    System.out.println("Firebase Admin SDK initialized successfully via Environment Variable (Render).");
                }
                else {
                    ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
                    if (resource.exists()) {
                        InputStream serviceAccount = resource.getInputStream();
                        options = FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                .build();
                        System.out.println("Firebase Admin SDK initialized successfully via local serviceAccountKey.json.");
                    } else {
                        System.err.println("WARNING: Neither FIREBASE_CREDENTIALS env nor serviceAccountKey.json found. Firebase verification will fail.");
                    }

                }

                if (options != null) {
                    FirebaseApp.initializeApp(options);
                }
            }
        } catch (IOException e) {
            System.err.println("Firebase Admin SDK failed to initialize: " + e.getMessage());
        }
    }
}