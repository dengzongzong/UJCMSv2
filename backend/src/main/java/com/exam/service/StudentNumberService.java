package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BusinessException;
import com.exam.entity.Student;
import com.exam.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 学生学号生成器
 * <p>格式: STU + yyyyMMdd + 4 位随机数字(范围 1000-9999,共 9000 个号段)</p>
 * <p>使用说明:</p>
 * <ul>
 *   <li>注册时(login)调用 ensureStudentNo(Student) - 学号为空时自动分配</li>
 *   <li>管理端 addStudent 调用 - 与历史逻辑保持一致</li>
 *   <li>导入时调用 - 导入文件里没填学号也能落地</li>
 * </ul>
 * <p>冲突处理: 走最多 10 次重试, 真冲突则抛 BusinessException</p>
 */
@Service
public class StudentNumberService {

    private static final String PREFIX = "STU";
    private static final String DATE_PATTERN = "yyyyMMdd";
    private static final int MAX_RETRY = 10;

    @Autowired
    private StudentMapper studentMapper;

    /**
     * 如果学号为空,自动分配一个;否则保留原值
     */
    public void ensureStudentNo(Student student) {
        if (student == null) return;
        if (student.getStudentNo() != null && !student.getStudentNo().isEmpty()) {
            return;
        }
        student.setStudentNo(generate());
    }

    /**
     * 生成新学号(自动避开已存在的)
     */
    public String generate() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN));
        for (int i = 0; i < MAX_RETRY; i++) {
            int rnd = ThreadLocalRandom.current().nextInt(1000, 10000); // 4 位, 1000-9999
            String no = PREFIX + dateStr + rnd;
            if (!exists(no)) {
                return no;
            }
        }
        throw new BusinessException("学号生成失败(已达最大重试次数),请重试");
    }

    /**
     * 生成新学号(别名方法,兼容旧调用)
     */
    public String generateStudentNo() {
        return generate();
    }

    private boolean exists(String no) {
        Long count = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, no));
        return count != null && count > 0;
    }
}
