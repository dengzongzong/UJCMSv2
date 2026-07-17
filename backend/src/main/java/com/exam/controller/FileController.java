package com.exam.controller;

import com.exam.common.BusinessException;
import com.exam.common.Result;
import com.exam.util.ImageCompressUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传控制器（需要 JWT 鉴权）
 */
@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${upload.path}")
    private String uploadPath;

    /**
     * 文件上传
     * 文件名使用 UUID 重命名，保留原扩展名，返回访问 URL。
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String newFileName = UUID.randomUUID().toString().replace("-", "") + ext;
        File dest = new File(uploadPath, newFileName);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }

        try {
            String contentType = file.getContentType();
            if (contentType != null && contentType.startsWith("image/")) {
                ImageCompressUtil.compressAndSave(file, dest, ext);
            } else {
                file.transferTo(dest);
            }
        } catch (IOException e) {
            throw new BusinessException("文件上传失败");
        }

        String url = "/uploads/" + newFileName;
        return Result.success(url);
    }
}
