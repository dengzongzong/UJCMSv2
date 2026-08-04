package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.entity.CourseOrder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public interface OrderService extends IService<CourseOrder> {

    /** 创建订单并返回支付二维码 */
    Map<String, Object> create(Long studentId, Long courseId, String channel);

    /** 查询订单(校验归属) */
    CourseOrder getByOrderNo(String orderNo, Long studentId);

    /** 我的订单 */
    List<CourseOrder> myOrders(Long studentId);

    /** 管理端分页查询 */
    PageResult<CourseOrder> adminPage(Integer page, Integer size, String keyword, Integer status, String channel);

    /** 微信支付回调(返回微信要求格式的 JSON 字符串) */
    String handleWechatCallback(HttpServletRequest request);

    /** 支付宝异步通知(返回 "success"/"failure") */
    String handleAlipayCallback(HttpServletRequest request);
}
