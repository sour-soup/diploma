package org.soursoup.bimbim.service.impl;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.soursoup.bimbim.config.MinioConfig;
import org.soursoup.bimbim.dto.request.ImageRequest;
import org.soursoup.bimbim.service.ImageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class ImageServiceImpl implements ImageService {
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @SneakyThrows
    public String upload(ImageRequest imageRequest) {
        createBucket();

        MultipartFile file = imageRequest.image();

        String fileName = generateFileName(file);

        try (InputStream inputStream = file.getInputStream();) {
            saveImage(inputStream, fileName);
        }
        return fileName;
    }

    @SneakyThrows
    public String getImage(String filename) {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .method(Method.GET)
                        .object(filename)
                        .expiry(1, TimeUnit.MINUTES)
                        .build()
        );
    }

    private String generateFileName(MultipartFile file) {
        String extension = getExtension(file);
        return UUID.randomUUID() + "." + extension;
    }

    private String getExtension(MultipartFile file) {
        return Objects.requireNonNull(file.getOriginalFilename())
                .substring(file.getOriginalFilename().lastIndexOf(".") + 1);
    }

    @SneakyThrows
    private void createBucket() {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build());

        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
        }
    }

    @SneakyThrows
    private void saveImage(InputStream inputStream, String fileName) {
        minioClient.putObject(PutObjectArgs.builder()
                .stream(inputStream, inputStream.available(), -1)
                .bucket(minioConfig.getBucket())
                .object(fileName)
                .build());
    }
}
