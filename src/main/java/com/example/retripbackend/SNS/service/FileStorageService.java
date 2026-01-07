package com.example.retripbackend.SNS.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public List<String> saveFiles(MultipartFile[] files) throws IOException {
        List<String> fileUrls = new ArrayList<>();
        
        if (files == null || files.length == 0) {
            return fileUrls;
        }

        // 업로드 디렉토리 생성
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // 고유한 파일명 생성
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // 파일 저장
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // URL 생성 (웹에서 접근 가능한 경로)
            String fileUrl = "/uploads/" + uniqueFilename;
            fileUrls.add(fileUrl);
        }

        return fileUrls;
    }

    public String getThumbnailUrl(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        return imageUrls.get(0); // 첫 번째 이미지를 썸네일로
    }
}
