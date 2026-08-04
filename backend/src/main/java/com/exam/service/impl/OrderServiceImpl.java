package com.exam.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.config.PayConfig;
import com.exam.entity.*;
import com.exam.mapper.CourseOrderMapper;
import com.exam.mapper.CourseMapper;
import com.exam.mapper.StudentCourseMapper;
import com.exam.mapper.StudentMapper;
import com.exam.service.OrderService;
import com.exam.util.pay.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<CourseOrderMapper, CourseOrder> implements OrderService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private StudentCourseMapper studentCourseMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private PayProviderFactory providerFactory;

    @Autowired
    private PayConfig payConfig;

    @Override
    @Transactional
    public Map<String, Object> create(Long studentId, Long courseId, String channel) {
        if (studentId == null) {
            throw new BusinessException("请先登录");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getStatus() == null || course.getStatus() == 0) {
            throw new BusinessException("课程不存在或未上架");
        }
        // 已开通直接返回, 无需再购买
        if (checkOpened(courseId, studentId)) {
            throw new BusinessException("您已开通该课程,无需重复购买");
        }
        BigDecimal price = course.getPrice() == null ? BigDecimal.ZERO : course.getPrice();
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            // 免费课程直接开通
            openCourse(courseId, studentId);
            Map<String, Object> free = new HashMap<>();
            free.put("opened", true);
            free.put("amount", 0);
            free.put("courseName", course.getName());
            return free;
        }
        int amount = price.multiply(BigDecimal.valueOf(100)).intValue();

        // 渠道归一化: 未传/不可用渠道时回退到默认通道(避免前端传了未启用通道导致下单失败)
        String ch = normalizeChannel(channel);

        // 复用同一学生/课程/渠道的未支付订单
        CourseOrder order = getPending(studentId, courseId, ch);
        boolean isNew = order == null;
        if (isNew) {
            order = new CourseOrder();
            order.setOrderNo(genOrderNo());
            order.setStudentId(studentId);
            order.setCourseId(courseId);
            order.setCourseName(course.getName());
            order.setAmount(amount);
            order.setChannel(ch);
            order.setStatus(0);
            this.save(order);
        }
        // 调用支付平台下单, 获取二维码
        PaymentProvider provider = providerFactory.getProvider(order.getChannel());
        if (provider == null) {
            throw new BusinessException("支付通道未配置");
        }
        PayCreateResult pay = provider.createOrder(order, notifyUrl(order.getChannel()));
        Map<String, Object> result = new HashMap<>();
        result.put("opened", false);
        result.put("isNew", isNew);
        result.put("orderNo", order.getOrderNo());
        result.put("channel", order.getChannel());
        result.put("qrCode", pay.getQrCode());
        result.put("qrImage", pay.getQrImage());
        result.put("amount", order.getAmount());
        result.put("amountYuan", String.format("%.2f", order.getAmount() / 100.0));
        result.put("courseName", order.getCourseName());
        return result;
    }

    @Override
    public CourseOrder getByOrderNo(String orderNo, Long studentId) {
        LambdaQueryWrapper<CourseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseOrder::getOrderNo, orderNo);
        if (studentId != null) {
            wrapper.eq(CourseOrder::getStudentId, studentId);
        }
        wrapper.last("LIMIT 1");
        return this.getOne(wrapper);
    }

    @Override
    public List<CourseOrder> myOrders(Long studentId) {
        LambdaQueryWrapper<CourseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseOrder::getStudentId, studentId)
                .orderByDesc(CourseOrder::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public PageResult<CourseOrder> adminPage(Integer page, Integer size, String keyword, Integer status, String channel) {
        LambdaQueryWrapper<CourseOrder> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            // 只匹配订单号/课程名(studentName 为非库字段, 不能进 SQL)
            wrapper.and(w -> w.like(CourseOrder::getOrderNo, keyword)
                    .or().like(CourseOrder::getCourseName, keyword));
        }
        if (status != null) {
            wrapper.eq(CourseOrder::getStatus, status);
        }
        if (channel != null && !channel.isEmpty()) {
            wrapper.eq(CourseOrder::getChannel, channel);
        }
        wrapper.orderByDesc(CourseOrder::getCreateTime);
        IPage<CourseOrder> result = this.page(new Page<>(page, size), wrapper);
        for (CourseOrder order : result.getRecords()) {
            Student student = studentMapper.selectById(order.getStudentId());
            order.setStudentName(student != null
                    ? (student.getName() != null ? student.getName() : student.getPhone())
                    : "");
        }
        return new PageResult<>(result);
    }

    @Override
    public String handleWechatCallback(HttpServletRequest request) {
        PaymentProvider provider = providerFactory.getProvider("wechat");
        PayNotifyResult notify = provider.verifyNotify(request);
        if (!notify.isSuccess()) {
            log.warn("微信回调处理失败: {}", notify.getMessage());
            return "{\"code\":\"FAIL\",\"message\":\"" + safe(notify.getMessage()) + "\"}";
        }
        if (!processPaid(notify)) {
            return "{\"code\":\"FAIL\",\"message\":\"订单处理失败\"}";
        }
        return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
    }

    @Override
    public String handleAlipayCallback(HttpServletRequest request) {
        PaymentProvider provider = providerFactory.getProvider("alipay");
        PayNotifyResult notify = provider.verifyNotify(request);
        if (!notify.isSuccess()) {
            log.warn("支付宝回调处理失败: {}", notify.getMessage());
            return "failure";
        }
        return processPaid(notify) ? "success" : "failure";
    }

    /**
     * 支付成功后: 更新订单 + 自动开通课程(幂等)
     */
    @Transactional
    protected boolean processPaid(PayNotifyResult notify) {
        if (notify.getOrderNo() == null) {
            return false;
        }
        CourseOrder order = getByOrderNo(notify.getOrderNo(), null);
        if (order == null) {
            log.warn("支付回调订单不存在: {}", notify.getOrderNo());
            return false;
        }
        // 已支付: 幂等直接返回成功
        if (order.getStatus() != null && order.getStatus() == 1) {
            return true;
        }
        // 金额校验
        if (notify.getAmountFen() != null && !notify.getAmountFen().equals(order.getAmount())) {
            log.error("支付金额不匹配: orderNo={} expect={} actual={}", order.getOrderNo(), order.getAmount(), notify.getAmountFen());
            return false;
        }
        order.setStatus(1);
        order.setTransactionId(notify.getTransactionId());
        order.setPayTime(LocalDateTime.now());
        this.updateById(order);
        // 自动开通课程
        openCourse(order.getCourseId(), order.getStudentId());
        log.info("支付成功自动开通: orderNo={} studentId={} courseId={}",
                order.getOrderNo(), order.getStudentId(), order.getCourseId());
        return true;
    }

    /** 写课程开通记录(幂等) */
    private void openCourse(Long courseId, Long studentId) {
        if (checkOpened(courseId, studentId)) {
            return;
        }
        StudentCourse sc = new StudentCourse();
        sc.setStudentId(studentId);
        sc.setCourseId(courseId);
        try {
            studentCourseMapper.insert(sc);
        } catch (Exception e) {
            log.warn("开通课程记录插入冲突(忽略): courseId={} studentId={}", courseId, studentId);
        }
    }

    private boolean checkOpened(Long courseId, Long studentId) {
        if (studentId == null) {
            return false;
        }
        LambdaQueryWrapper<StudentCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentCourse::getStudentId, studentId)
                .eq(StudentCourse::getCourseId, courseId)
                .last("LIMIT 1");
        return studentCourseMapper.selectCount(wrapper) > 0;
    }

    private CourseOrder getPending(Long studentId, Long courseId, String channel) {
        LambdaQueryWrapper<CourseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseOrder::getStudentId, studentId)
                .eq(CourseOrder::getCourseId, courseId)
                .eq(CourseOrder::getStatus, 0);
        if (channel != null && !channel.isEmpty()) {
            wrapper.eq(CourseOrder::getChannel, channel);
        }
        wrapper.last("LIMIT 1");
        return this.getOne(wrapper);
    }

    private String genOrderNo() {
        return "CO" + DateUtil.format(new java.util.Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(6);
    }

    /** 渠道归一化: 未传或未启用时回退到配置的默认通道 */
    private String normalizeChannel(String channel) {
        String defaultCh = payConfig.getChannel();
        if ("both".equalsIgnoreCase(defaultCh)) {
            defaultCh = "wechat";
        }
        String ch = (channel == null || channel.isEmpty()) ? defaultCh : channel;
        if (!providerFactory.enabled(ch)) {
            return defaultCh;
        }
        return ch;
    }

    private String notifyUrl(String channel) {
        return StrUtil.removeSuffix(payConfig.getCallbackBase(), "/") + "/public/pay/callback/" + channel;
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }
}
