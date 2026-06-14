package com.nekocafe.cat.service;

import com.nekocafe.cat.dto.CatPhotoUploadResponse;
import com.nekocafe.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CatPhotoStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
        "image/jpeg", ".jpg",
        "image/png", ".png",
        "image/webp", ".webp",
        "image/gif", ".gif"
    );

    private final Path uploadDir;
    private final String urlPrefix;

    public CatPhotoStorageService(@Value("${nekocafe.upload.cat-photo-dir:uploads/cats}") String uploadDir,
                                  @Value("${nekocafe.upload.cat-photo-url-prefix:/uploads/cats}") String urlPrefix) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.urlPrefix = normalizePrefix(urlPrefix);
    }

    public CatPhotoUploadResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(4201, "请选择要上传的猫咪照片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(4203, "猫咪照片不能超过 5MB");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new BizException(4202, "仅支持 jpg、png、webp、gif 格式的猫咪照片");
        }

        String filename = UUID.randomUUID() + extension;
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new BizException(4204, "照片保存失败");
        }

        try {
            Files.createDirectories(uploadDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BizException(4204, "照片保存失败");
        }

        return new CatPhotoUploadResponse(urlPrefix + "/" + filename);
    }

    public Path uploadDir() {
        return uploadDir;
    }

    public String urlPattern() {
        return urlPrefix + "/**";
    }

    private String normalizePrefix(String prefix) {
        String normalized = prefix == null || prefix.isBlank() ? "/uploads/cats" : prefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
