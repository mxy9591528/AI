package com.example.aispringboot.dto.response;

import lombok.Data;

@Data
public class FileUploadVO {
    private Long id;
    private String originalName;
    private String filePath;
    private Long fileSize;
    private String fileType;

    public static FileUploadVO of(Long id, String originalName, String filePath, Long fileSize, String fileType) {
        FileUploadVO vo = new FileUploadVO();
        vo.setId(id);
        vo.setOriginalName(originalName);
        vo.setFilePath(filePath);
        vo.setFileSize(fileSize);
        vo.setFileType(fileType);
        return vo;
    }
}
