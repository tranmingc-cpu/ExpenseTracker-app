package com.expensetracker.expensetracker_api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
                if (resource.exists()) {
                    InputStream serviceAccount = resource.getInputStream();
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                    FirebaseApp.initializeApp(options);
                    System.out.println("Firebase Admin SDK initialized successfully.");
                } else {
                    System.err.println("WARNING: serviceAccountKey.json not found in classpath (src/main/resources). Firebase verification will fail until configured.");
                }
            }
        } catch (IOException e) {
            System.err.println("Firebase Admin SDK failed to initialize: " + e.getMessage());
        }
    }
}
