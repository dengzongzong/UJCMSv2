package com.exam.controller;

import com.exam.common.BusinessException;
import com.exam.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 上传文件清理(图片/视频)。
 * <p>扫描 uploads 目录,比对数据库中所有字符串列里引用的 /uploads/ 文件名,
 * 找出未被任何业务数据引用的"孤儿文件",支持预览与删除。</p>
 * <p>安全机制(多重防护,避免误删):
 * <ol>
 *   <li>跳过 cert_preview 缓存目录</li>
 *   <li>跳过 static 目录(旧系统导入文件,引用格式多样)</li>
 *   <li>按文件名匹配(同名文件只要有一个被引用,全部保护)</li>
 *   <li>按相对路径匹配(image/20250715/xxx.png)</li>
 *   <li>按多种路径前缀匹配(uploads/、static/upload/、static/)</li>
 *   <li>删除前二次验证: 同时检查文件名和相对路径</li>
 * </ol>
 * </p>
 */
@RestController
@RequestMapping("/admin/upload")
public class UploadCleanController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${upload.path}")
    private String uploadPath;

    /**
     * 匹配字符串中各种形式的上传文件引用:
     * - /uploads/xxx
     * - /static/upload/xxx
     * - /static/证书模板图/xxx
     * - /static/xxx (旧路径,如 /static/image/xxx.png)
     * 同时兼容无前导 / 的情况: uploads/xxx
     */
    private static final Pattern UPLOAD_REF = Pattern.compile(
            "/?(?:uploads|static/upload|static/证书模板图|static)/([^\"'\\s<>)(\\\\]+)"
    );

    /**
     * 扫描孤儿文件(未被数据库任何业务数据引用的上传文件)。
     *
     * @return { total, orphanCount, referencedCount, orphans:[{filename, size, lastModified, path}] }
     */
    @GetMapping("/orphans")
    public Result<Map<String, Object>> orphans() {
        // 1. 收集磁盘上所有文件(排除证书预览缓存目录 cert_preview 和 static 目录)
        File root = new File(uploadPath);
        List<File> diskFiles = new ArrayList<>();
        if (root.exists() && root.isDirectory()) {
            collectFiles(root, diskFiles);
        }

        // 2. 收集数据库中所有被引用的文件名和相对路径
        Set<String> referencedNames = collectReferencedFilenames();
        Set<String> referencedPaths = collectReferencedFilePaths();

        // 3. 计算孤儿
        List<Map<String, Object>> orphanList = new ArrayList<>();
        int referencedCount = 0;
        for (File f : diskFiles) {
            String name = f.getName();
            // 跳过证书预览缓存文件(由系统管理,不算孤儿)
            if (isUnderCertPreview(f)) continue;
            // 跳过 static 目录下的文件(旧系统导入的文件,引用格式多样,容易误报)
            if (isUnderStaticDir(f)) continue;

            // 多重匹配检查
            if (isFileReferenced(f, root, name, referencedNames, referencedPaths)) {
                referencedCount++;
                continue;
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("filename", name);
            m.put("size", f.length());
            m.put("lastModified", f.lastModified());
            m.put("path", relativePath(root, f));
            orphanList.add(m);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", diskFiles.size());
        data.put("referencedCount", referencedCount);
        data.put("orphanCount", orphanList.size());
        data.put("orphans", orphanList);
        return Result.success(data);
    }

    /**
     * 删除指定的孤儿文件。
     *
     * @param body { files: ["filename1","filename2",...] }
     * @return { deleted, failed:[{filename, reason}] }
     */
    @PostMapping("/clean")
    public Result<Map<String, Object>> clean(@RequestBody Map<String, Object> body) {
        Object filesObj = body == null ? null : body.get("files");
        if (!(filesObj instanceof List)) {
            throw new BusinessException("请传入要删除的文件列表");
        }
        @SuppressWarnings("unchecked")
        List<String> files = (List<String>) filesObj;
        if (files.isEmpty()) {
            throw new BusinessException("文件列表不能为空");
        }

        // 安全:删除前再次确认这些文件未被引用,避免误删
        Set<String> referencedNames = collectReferencedFilenames();
        Set<String> referencedPaths = collectReferencedFilePaths();
        File root = new File(uploadPath);

        int deleted = 0;
        List<Map<String, Object>> failed = new ArrayList<>();
        for (String name : files) {
            // 仅按文件名查找(uploads 根目录下的文件)
            File target = findByName(root, name);
            if (target == null) {
                failed.add(fail(name, "文件不存在"));
                continue;
            }
            if (isUnderCertPreview(target)) {
                failed.add(fail(name, "证书预览缓存不可通过此接口删除"));
                continue;
            }
            if (isUnderStaticDir(target)) {
                failed.add(fail(name, "static目录下的文件不可删除(旧系统导入文件)"));
                continue;
            }
            // 二次验证: 同时检查文件名和相对路径
            if (isFileReferenced(target, root, name, referencedNames, referencedPaths)) {
                failed.add(fail(name, "该文件仍被业务数据引用,已跳过"));
                continue;
            }
            if (target.delete()) {
                deleted++;
            } else {
                failed.add(fail(name, "删除失败(可能无权限)"));
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deleted", deleted);
        data.put("failed", failed);
        return Result.success(data);
    }

    /**
     * 清空证书预览缓存(单独提供,因为预览缓存可安全重建)。
     */
    @PostMapping("/clean-preview-cache")
    public Result<Map<String, Object>> cleanPreviewCache() {
        File dir = new File(new File(uploadPath), "cert_preview");
        int deleted = 0;
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.delete()) deleted++;
                }
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deleted", deleted);
        return Result.success(data);
    }

    // ============ 内部方法 ============

    /**
     * 多重匹配: 判断文件是否被数据库引用
     * 检查顺序(任一命中即认为被引用):
     * 1. 文件名匹配(同名文件只要有一个被引用,全部保护)
     * 2. 相对路径匹配(如 image/20250715/xxx.png)
     * 3. uploads/ + 相对路径匹配
     * 4. static/upload/ + 相对路径匹配
     * 5. static/ + 相对路径匹配(旧路径格式)
     */
    private boolean isFileReferenced(File f, File root, String name,
                                     Set<String> referencedNames, Set<String> referencedPaths) {
        // 1. 按文件名匹配(最宽泛,同名即保护)
        if (referencedNames.contains(name)) return true;

        // 2. 按相对路径匹配
        String relPath = relativePath(root, f);
        if (relPath.startsWith("/")) relPath = relPath.substring(1);

        if (referencedPaths.contains(relPath)) return true;
        if (referencedPaths.contains("uploads/" + relPath)) return true;
        if (referencedPaths.contains("static/upload/" + relPath)) return true;
        if (referencedPaths.contains("static/" + relPath)) return true;

        return false;
    }

    /** 递归收集目录下所有文件 */
    private void collectFiles(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File c : children) {
            if (c.isDirectory()) {
                collectFiles(c, out);
            } else if (c.isFile()) {
                out.add(c);
            }
        }
    }

    /** 查询数据库所有字符串列中引用到的文件名集合 */
    private Set<String> collectReferencedFilenames() {
        Set<String> referenced = new HashSet<>();
        collectReferencedFromDb(referenced, null);
        return referenced;
    }

    /** 查询数据库所有字符串列中引用到的完整路径集合(如 image/20250715/xxx.png) */
    private Set<String> collectReferencedFilePaths() {
        Set<String> paths = new HashSet<>();
        collectReferencedFromDb(null, paths);
        return paths;
    }

    /**
     * 统一的数据库引用扫描: 同时收集文件名和相对路径
     * @param nameSet 文件名集合(可为null)
     * @param pathSet 相对路径集合(可为null)
     */
    private void collectReferencedFromDb(Set<String> nameSet, Set<String> pathSet) {
        // 取当前库名
        String dbName;
        try {
            dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        } catch (Exception e) {
            return;
        }
        if (dbName == null) return;

        // 查询所有字符串类型列(包括 json 类型)
        List<Map<String, Object>> cols;
        try {
            cols = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = ? AND DATA_TYPE IN ('varchar','text','mediumtext','longtext','char','tinytext','json')",
                    dbName);
        } catch (Exception e) {
            return;
        }

        for (Map<String, Object> col : cols) {
            String table = String.valueOf(col.get("TABLE_NAME"));
            String column = String.valueOf(col.get("COLUMN_NAME"));
            try {
                // 宽匹配: 覆盖 /uploads/、/static/upload/、/static/证书模板图/、/static/、uploads/ 等所有格式
                List<String> values = jdbcTemplate.queryForList(
                        "SELECT `" + column + "` FROM `" + table + "` WHERE " +
                                "`" + column + "` LIKE '%/uploads/%' " +
                                "OR `" + column + "` LIKE '%/static/upload/%' " +
                                "OR `" + column + "` LIKE '%/static/证书模板图/%' " +
                                "OR `" + column + "` LIKE '%/static/%' " +
                                "OR `" + column + "` LIKE '%uploads/%'",
                        String.class);
                for (String v : values) {
                    if (v == null) continue;
                    Matcher m = UPLOAD_REF.matcher(v);
                    while (m.find()) {
                        String path = m.group(1);
                        // 收集完整相对路径(如 image/20250715/xxx.png)
                        if (pathSet != null) {
                            pathSet.add(path);
                        }
                        // 收集文件名
                        int slash = path.lastIndexOf('/');
                        String name = slash >= 0 ? path.substring(slash + 1) : path;
                        // 去掉可能的 query 参数
                        int q = name.indexOf('?');
                        if (q >= 0) name = name.substring(0, q);
                        if (!name.isEmpty() && nameSet != null) {
                            nameSet.add(name);
                        }
                    }
                }
            } catch (Exception ignored) {
                // 某些表/列可能无权访问或不存在,跳过
            }
        }
    }

    /** 在 uploads 根目录(含子目录)按文件名查找文件 */
    private File findByName(File root, String name) {
        // 先尝试根目录(绝大多数上传文件在根目录)
        File direct = new File(root, name);
        if (direct.isFile()) return direct;
        // 递归查找
        return findByNameRecursive(root, name);
    }

    private File findByNameRecursive(File dir, String name) {
        File[] children = dir.listFiles();
        if (children == null) return null;
        for (File c : children) {
            if (c.isFile() && c.getName().equals(name)) return c;
            if (c.isDirectory()) {
                File r = findByNameRecursive(c, name);
                if (r != null) return r;
            }
        }
        return null;
    }

    private boolean isUnderCertPreview(File f) {
        try {
            return f.getCanonicalPath().startsWith(new File(uploadPath, "cert_preview").getCanonicalPath());
        } catch (Exception e) {
            return false;
        }
    }

    /** 判断文件是否在 static 目录下(旧系统导入的文件,引用格式多样,不做孤儿扫描) */
    private boolean isUnderStaticDir(File f) {
        try {
            return f.getCanonicalPath().startsWith(new File(uploadPath, "static").getCanonicalPath());
        } catch (Exception e) {
            return false;
        }
    }

    private String relativePath(File root, File f) {
        try {
            String rp = f.getCanonicalPath().substring(root.getCanonicalPath().length());
            return rp.replace('\\', '/');
        } catch (Exception e) {
            return f.getName();
        }
    }

    private Map<String, Object> fail(String filename, String reason) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("filename", filename);
        m.put("reason", reason);
        return m;
    }
}
