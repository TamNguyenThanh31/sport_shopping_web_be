package com.runner.shopping.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalStorageService {

    private final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    public String uploadImage(MultipartFile file, Long productId) throws IOException {
        // Nén ảnh
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(file.getInputStream())
                .size(800, 800) // Kích thước tối đa
                .outputQuality(0.8) // Chất lượng 80%
                .outputFormat("jpg")
                .toOutputStream(baos);
        byte[] compressedData = baos.toByteArray();

        // Tạo thư mục theo productId nếu chưa tồn tại
        String productDir = UPLOAD_DIR + "products/" + productId + "/";
        Files.createDirectories(Paths.get(productDir));

        // Tạo tên file duy nhất
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(productDir + fileName);

        // Lưu file
        Files.write(filePath, compressedData);

        // Trả về URL tương đối
        return "/uploads/products/" + productId + "/" + fileName;
    }

    public void deleteImage(String imageUrl) throws IOException {
        String filePath = "src/main/resources/static" + imageUrl;
        Files.deleteIfExists(Paths.get(filePath));
    }
}