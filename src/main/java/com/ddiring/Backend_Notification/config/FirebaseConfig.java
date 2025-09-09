package com.ddiring.Backend_Notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        String firebaseJson = System.getenv("FIREBASE_CONFIG");

//        if (firebaseJson != null && !firebaseJson.isEmpty()) {
//            //운영 환경: 환경변수에서 읽기
//            GoogleCredentials credentials = GoogleCredentials.fromStream(
//                    new ByteArrayInputStream(firebaseJson.getBytes())
//            );
//            FirebaseOptions options = FirebaseOptions.builder()
//                    .setCredentials(credentials)
//                    .build();
//            return FirebaseApp.initializeApp(options);
//        } else {
//            //로컬 테스트: 파일에서 읽기
//            String localPath = "src/main/resources/serviceAccountKey.json"; //다운로드한 JSON 위치
//            if (!Files.exists(Paths.get(localPath))) {
//                throw new IllegalStateException("Firebase 설정 파일 없음: " + localPath);
//            }
//            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(localPath));
//            FirebaseOptions options = FirebaseOptions.builder()
//                    .setCredentials(credentials)
//                    .build();
//            return FirebaseApp.initializeApp(options);
//        }
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(firebaseJson.getBytes())
        );
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}