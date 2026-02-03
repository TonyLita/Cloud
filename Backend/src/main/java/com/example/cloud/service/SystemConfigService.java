package com.example.cloud.service;

import com.example.cloud.entity.SystemConfig;
import com.example.cloud.repository.SystemConfigRepository;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SystemConfigService {

    private final SystemConfigRepository configRepository;
    private static final String CONFIG_KEY_TIMEOUT = "session_timeout";
    private static final String FIREBASE_CONFIG_COLLECTION = "app_configs";
    private static final String FIREBASE_CONFIG_DOC = "session_settings";

    public SystemConfigService(SystemConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public int getSessionTimeoutMinutes() {
        return configRepository.findById(CONFIG_KEY_TIMEOUT)
                .map(config -> Integer.parseInt(config.getConfigValue()))
                .orElse(30); // Default 30 minutes
    }

    @Transactional
    public void updateSessionTimeout(int minutes) throws Exception {
        // 1. Update in PostgreSQL
        SystemConfig config = configRepository.findById(CONFIG_KEY_TIMEOUT)
                .orElse(new SystemConfig(CONFIG_KEY_TIMEOUT, String.valueOf(minutes)));
        config.setConfigValue(String.valueOf(minutes));
        configRepository.save(config);

        // 2. Sync with Firebase Firestore
        try {
            Firestore db = FirestoreClient.getFirestore();
            Map<String, Object> data = new HashMap<>();
            data.put("timeout_minutes", minutes);
            db.collection(FIREBASE_CONFIG_COLLECTION).document(FIREBASE_CONFIG_DOC).set(data).get();
        } catch (Exception e) {
            System.err.println("Firebase Firestore sync failed for config: " + e.getMessage());
        }
    }
}
