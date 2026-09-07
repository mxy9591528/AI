package com.example.aispringboot.service.system;

import cn.hutool.core.util.StrUtil;
import com.example.aispringboot.dto.response.FileUploadVO;
import com.example.aispringboot.entity.SysFileInfo;
import com.example.aispringboot.exception.BusinessException;
import com.example.aispringboot.mapper.SysFileInfoMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传：按日期分目录存储，UUID 重命名，落库元数据后返回访问 URL。
 */
@Service
public class FileService {

    public static final Path UPLOAD_ROOT = Paths.get("uploads").toAbsolutePath();
    public static final String URL_PREFIX = "/upload/";
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final DateTimeFormatter DATE_DIR_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Resource
    private SysFileInfoMapper sysFileInfoMapper;

    public FileUploadVO upload(MultipartFile file, String businessType, String businessId,
                               String businessField, Long uploadUserId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String ext = extractExtension(originalName);
        String relativePath = LocalDate.now().format(DATE_DIR_FORMAT) + "/"
                + UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
        try {
            Path targetFile = UPLOAD_ROOT.resolve(relativePath);
            Files.createDirectories(targetFile.getParent());
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            throw new BusinessException("文件保存失败：" + e.getMessage());
        }
        String fileUrl = URL_PREFIX + relativePath;
        SysFileInfo fileInfo = new SysFileInfo();
        fileInfo.setOriginalName(originalName);
        fileInfo.setFilePath(fileUrl);
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFileType(IMAGE_EXTENSIONS.contains(ext) ? "IMG" : ext.toUpperCase(Locale.ROOT));
        fileInfo.setBusinessType(businessType);
        fileInfo.setBusinessId(businessId);
        fileInfo.setBusinessField(businessField);
        fileInfo.setUploadUserId(uploadUserId);
        fileInfo.setIsTemp(0);
        fileInfo.setStatus(1);
        fileInfo.setCreateTime(LocalDateTime.now());
        sysFileInfoMapper.insert(fileInfo);
        return FileUploadVO.of(fileInfo.getId(), originalName, fileUrl, file.getSize(), fileInfo.getFileType());
    }

    private static String extractExtension(String originalName) {
        if (StrUtil.isBlank(originalName) || !originalName.contains(".")) {
            return "";
        }
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
