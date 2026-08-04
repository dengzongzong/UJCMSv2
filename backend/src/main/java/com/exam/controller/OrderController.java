package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.CourseOrder;
import com.exam.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户端课程订单(购买课程)
 */
@RestController
@RequestMapping("/user/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单并返回支付二维码
     * @param channel wechat/alipay(不传用默认通道)
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestAttribute("userId") Long userId,
                                              @RequestParam Long courseId,
                                              @RequestParam(required = false) String channel) {
        return Result.success(orderService.create(userId, courseId, channel));
    }

    /**
     * 我的订单列表
     */
    @GetMapping("/my")
    public Result<List<CourseOrder>> my(@RequestAttribute("userId") Long userId) {
        return Result.success(orderService.myOrders(userId));
    }

    /**
     * 订单详情(前端轮询支付状态用)
     */
    @GetMapping("/{orderNo}")
    public Result<CourseOrder> detail(@RequestAttribute("userId") Long userId,
                                      @PathVariable String orderNo) {
        return Result.success(orderService.getByOrderNo(orderNo, userId));
    }
}
