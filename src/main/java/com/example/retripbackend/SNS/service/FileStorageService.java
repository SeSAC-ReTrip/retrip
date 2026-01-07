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

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // DB에 저장될 접근 경로
            String fileUrl = "/uploads/" + uniqueFilename;
            fileUrls.add(fileUrl);
        }
        return fileUrls;
    }

    /**
     * [추가] 물리 파일 삭제 로직
     * @param fileUrl DB에 저장된 파일의 URL 경로 (예: /uploads/abc.jpg)
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;

        try {
            // URL 경로(/uploads/파일명)에서 파일명만 추출
            String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(uploadDir).resolve(filename);

            // 파일이 존재하면 삭제
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // 삭제 실패 시 로그 출력 (비즈니스 로직에 영향을 주지 않도록 예외 처리)
            System.err.println("파일 삭제 실패: " + fileUrl + " - " + e.getMessage());
        }
    }

    public String getThumbnailUrl(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return null;
        return imageUrls.get(0);
    }
}