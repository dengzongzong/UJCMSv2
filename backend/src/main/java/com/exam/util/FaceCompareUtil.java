package com.exam.util;

import com.exam.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务端人脸比对工具(基于 OpenCV)
 *
 * 流程:
 * 1. Haar 级联分类器检测两张照片中的人脸区域
 * 2. 裁剪人脸区域并缩放到统一尺寸(128x128 灰度图)
 * 3. 计算灰度直方图并用 Bhattacharyya 距离比较
 *
 * 距离值范围 [0, 1]: 0 = 完全相同, 1 = 完全不同
 * 阈值建议 0.5-0.6(可在后台 system_setting 表 face_verify_threshold 调整)
 */
@Slf4j
@Component
public class FaceCompareUtil {

    private CascadeClassifier cascade;
    private boolean initialized = false;
    private String initError = "";

    @PostConstruct
    public void init() {
        try {
            // 尝试方式1: 使用 nu.pattern.OpenCV 自动加载(从 JAR 中提取原生库)
            try {
                nu.pattern.OpenCV.loadLocally();
                log.info("OpenCV native library loaded via nu.pattern");
            } catch (Throwable e1) {
                log.warn("nu.pattern.OpenCV.loadLocally() failed: {}", e1.getMessage());
                // 尝试方式2: 直接 System.loadLibrary(需服务器已安装 opencv)
                try {
                    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
                    log.info("OpenCV native library loaded via System.loadLibrary");
                } catch (Throwable e2) {
                    log.error("System.loadLibrary also failed: {}", e2.getMessage());
                    // 尝试方式3: 从 openpnp 包中手动提取原生库
                    try {
                        loadNativeFromJar();
                        log.info("OpenCV native library loaded from JAR manually");
                    } catch (Throwable e3) {
                        initError = "OpenCV原生库加载失败: " + e3.getMessage();
                        log.error(initError);
                        return;
                    }
                }
            }

            // 从 classpath 提取 Haar 级联文件到临时文件
            InputStream is = getClass().getResourceAsStream("/opencv/haarcascade_frontalface_default.xml");
            if (is == null) {
                initError = "Haar级联分类器文件未找到";
                log.error(initError);
                return;
            }

            File cascadeFile = File.createTempFile("haarcascade_frontalface_default", ".xml");
            cascadeFile.deleteOnExit();
            try (FileOutputStream os = new FileOutputStream(cascadeFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            is.close();

            cascade = new CascadeClassifier();
            cascade.load(cascadeFile.getAbsolutePath());

            if (cascade.empty()) {
                initError = "Haar级联分类器加载失败";
                log.error(initError);
                return;
            }

            initialized = true;
            log.info("FaceCompareUtil initialized successfully");
        } catch (Exception e) {
            initError = "人脸比对引擎初始化失败: " + e.getMessage();
            log.error(initError, e);
        }
    }

    /**
     * 从 openpnp opencv JAR 中手动提取并加载原生库
     */
    private void loadNativeFromJar() throws Exception {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        String libName;
        String resourcePath;

        if (osName.contains("linux")) {
            libName = "libopencv_java470.so";
            resourcePath = "/nu/pattern/opencv/linux/" + osArch + "/" + libName;
        } else if (osName.contains("windows")) {
            libName = "opencv_java470.dll";
            resourcePath = "/nu/pattern/opencv/windows/" + osArch + "/" + libName;
        } else if (osName.contains("mac")) {
            libName = "libopencv_java470.dylib";
            resourcePath = "/nu/pattern/opencv/osx/" + osArch + "/" + libName;
        } else {
            throw new Exception("不支持的操作系统: " + osName);
        }

        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            // 尝试不带 arch 子目录
            is = getClass().getResourceAsStream("/nu/pattern/opencv/" + libName);
        }
        if (is == null) {
            throw new Exception("原生库文件未找到: " + resourcePath);
        }

        File tempLib = File.createTempFile("opencv_native", libName.substring(libName.lastIndexOf(".")));
        tempLib.deleteOnExit();
        try (FileOutputStream os = new FileOutputStream(tempLib)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
        is.close();

        System.load(tempLib.getAbsolutePath());
        log.info("Loaded OpenCV native lib from: {}", tempLib.getAbsolutePath());
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String getInitError() {
        return initError;
    }

    /**
     * 比对两张照片中的人脸
     *
     * @param idPhotoBytes   证件照字节数组
     * @param capturedBytes  拍摄照片字节数组
     * @return Bhattacharyya 距离 [0, 1], 越小越相似
     * @throws BusinessException 如果引擎未初始化、图片解码失败或未检测到人脸
     */
    public double compare(byte[] idPhotoBytes, byte[] capturedBytes) {
        if (!initialized) {
            String msg = initError.isEmpty() ? "人脸比对引擎未初始化" : initError;
            log.error("compare called but not initialized: {}", msg);
            throw new BusinessException(msg + "，请联系管理员");
        }

        // 解码图片
        Mat idPhoto = Imgcodecs.imdecode(new MatOfByte(idPhotoBytes), Imgcodecs.IMREAD_COLOR);
        Mat captured = Imgcodecs.imdecode(new MatOfByte(capturedBytes), Imgcodecs.IMREAD_COLOR);

        if (idPhoto.empty()) {
            throw new BusinessException("证件照解码失败，可能文件损坏");
        }
        if (captured.empty()) {
            throw new BusinessException("拍摄照片解码失败，请重新拍照");
        }

        try {
            // 检测人脸
            Rect idFace = detectFace(idPhoto);
            Rect capturedFace = detectFace(captured);

            if (idFace == null) {
                throw new BusinessException("证件照中未检测到人脸，请联系管理员更换证件照");
            }
            if (capturedFace == null) {
                throw new BusinessException("拍摄照片中未检测到人脸，请确保面部清晰、光线充足");
            }

            // 裁剪人脸区域并缩放到统一尺寸
            Mat idFaceMat = cropAndResize(idPhoto, idFace);
            Mat capturedFaceMat = cropAndResize(captured, capturedFace);

            // 计算灰度直方图
            Mat hist1 = computeHistogram(idFaceMat);
            Mat hist2 = computeHistogram(capturedFaceMat);

            // 用 Bhattacharyya 距离比较直方图
            double distance = Imgproc.compareHist(hist1, hist2, Imgproc.HISTCMP_BHATTACHARYYA);

            log.info("Face compare distance: {}", distance);
            return distance;
        } finally {
            idPhoto.release();
            captured.release();
        }
    }

    /**
     * 检测图片中最大的人脸区域
     */
    private Rect detectFace(Mat image) {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);

        MatOfRect faces = new MatOfRect();
        cascade.detectMultiScale(gray, faces, 1.1, 3, 0, new Size(30, 30), new Size());

        Rect[] facesArray = faces.toArray();
        if (facesArray.length == 0) {
            gray.release();
            return null;
        }

        // 取最大的人脸
        Rect largest = facesArray[0];
        for (Rect r : facesArray) {
            if (r.area() > largest.area()) {
                largest = r;
            }
        }

        gray.release();
        return largest;
    }

    /**
     * 裁剪人脸区域并缩放到 128x128 灰度图
     */
    private Mat cropAndResize(Mat image, Rect face) {
        int x = Math.max(0, face.x);
        int y = Math.max(0, face.y);
        int width = Math.min(face.width, image.cols() - x);
        int height = Math.min(face.height, image.rows() - y);
        Rect safeRect = new Rect(x, y, width, height);

        Mat cropped = new Mat(image, safeRect);
        Mat resized = new Mat();
        Imgproc.resize(cropped, resized, new Size(128, 128));

        cropped.release();
        return resized;
    }

    /**
     * 计算灰度直方图(256 bins, 归一化到 [0, 1])
     */
    private Mat computeHistogram(Mat image) {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        List<Mat> images = new ArrayList<>();
        images.add(gray);
        MatOfInt histSize = new MatOfInt(256);
        MatOfInt channels = new MatOfInt(0);
        MatOfFloat ranges = new MatOfFloat(0f, 256f);
        Mat hist = new Mat();
        Imgproc.calcHist(images, channels, new Mat(), hist, histSize, ranges);
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);

        gray.release();
        return hist;
    }
}
