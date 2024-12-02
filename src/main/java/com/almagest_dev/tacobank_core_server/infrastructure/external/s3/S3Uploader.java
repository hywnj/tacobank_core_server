package com.almagest_dev.tacobank_core_server.infrastructure.external.s3;

import java.io.File;
import java.util.Objects;

public class S3Uploader {
    private static final String BUCKET_NAME = "taco-bank-logs";
    private static final String S3_FOLDER = "logs";
    private static final String LOCAL_LOG_PATH = "/log";

    private final S3Client s3Client;

    public S3Uploader() {
        this.s3Client = S3Client.create();
    }

    public void uploadLogsToS3() {
        try {
            File logDir = new File(LOCAL_LOG_PATH);
            if (!logDir.exists() || !logDir.isDirectory()) {
                System.err.println("Log directory does not exist or is not a directory.");
                return;
            }

            for (File logFile : Objects.requireNonNull(logDir.listFiles())) {
                if (logFile.isFile()) {
                    String s3Key = String.format("%s/%s", S3_FOLDER, logFile.getName());
                    s3Client.putObject(
                            PutObjectRequest.builder()
                                    .bucket(BUCKET_NAME)
                                    .key(s3Key)
                                    .build(),
                            logFile.toPath()
                    );
                    System.out.println("Uploaded: " + logFile.getName());
                    Files.delete(logFile.toPath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
