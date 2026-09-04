package com.personal.base.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Creates the MinIO client and, on startup, makes sure the configured bucket exists with an
 * anonymous-read policy — the "Upload Minio" test page links directly to the object URL, so
 * there's no presigned-URL round trip needed to view/download an uploaded file.
 */
@Configuration
public class MinioConfig {

  private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

  @Value("${minio.endpoint}")
  private String endpoint;

  @Value("${minio.access-key}")
  private String accessKey;

  @Value("${minio.secret-key}")
  private String secretKey;

  @Value("${minio.bucket}")
  private String bucket;

  @Bean
  public MinioClient minioClient() {
    // Supplying our own OkHttpClient skips the MinIO SDK's default client setup, which reads the
    // SSL_CERT_FILE/SSL_CERT_DIR env vars to build a custom trust store — on dev machines where
    // some other tool (e.g. a Python install) has set SSL_CERT_FILE to a path Java can't open,
    // that lookup throws and MinioClient.builder().build() fails outright.
    OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .httpClient(httpClient)
            .build();
  }

  @Bean
  public CommandLineRunner ensureMinioBucket(MinioClient minioClient) {
    return args -> {
      try {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
          minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }

        String publicReadPolicy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(publicReadPolicy).build());
      } catch (Exception e) {
        // MinIO may not be running yet (e.g. `docker compose up` not started) — don't block app
        // startup for a feature that's only used by the admin-only test page.
        log.warn("Could not initialize MinIO bucket '{}': {}", bucket, e.getMessage());
      }
    };
  }
}
