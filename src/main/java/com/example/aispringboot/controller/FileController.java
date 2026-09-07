package com.example.aispringboot.controller;

import com.example.aispringboot.common.Result;
import com.example.aispringboot.dto.response.FileUploadVO;
import com.example.aispringboot.service.system.FileService;
import com.example.aispringboot.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口。
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Resource
    private FileService fileService;

    @PostMapping("/upload")
    public Result<FileUploadVO> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) String businessType,
                                       @RequestParam(required = false) String businessId,
                                       @RequestParam(required = false) String businessField) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        return Result.ok(fileService.upload(file, businessType, businessId, businessField, userId));
    }
}
