package com.mawrid.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.credentials-path}")
    private String credentialsPath;

    @PostConstruct
    public void initFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            InputStream serviceAccount = loadCredentials();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized successfully");
        } catch (IOException ex) {
            log.warn("Firebase credentials not found at '{}'. FCM notifications will be disabled.", credentialsPath);
        }
    }

    private InputStream loadCredentials() throws IOException {
        Path absolutePath = Path.of(credentialsPath);
        if (absolutePath.isAbsolute() && Files.exists(absolutePath)) {
            return Files.newInputStream(absolutePath);
        }
        // Fallback to classpath
        return new ClassPathResource(credentialsPath).getInputStream();
    }
}
