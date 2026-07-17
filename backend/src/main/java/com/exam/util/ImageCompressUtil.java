package com.exam.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 图片保存工具类。
 * <p>直接保存原始文件，不做任何压缩或缩放，避免轮播图/封面图模糊。</p>
 */
public class ImageCompressUtil {

    /**
     * 直接保存原始图片文件，不压缩、不缩放。
     *
     * @param file 上传的文件
     * @param dest 目标路径
     * @param ext  文件扩展名（如 .jpg .png）
     */
    public static void compressAndSave(MultipartFile file, File dest, String ext) throws IOException {
        file.transferTo(dest);
    }

    /**
     * 直接保存原始图片文件，不压缩、不缩放。
     *
     * @param file 上传的文件
     * @param dest 目标路径
     */
    public static void compressAndSave(MultipartFile file, File dest) throws IOException {
        file.transferTo(dest);
    }
}
