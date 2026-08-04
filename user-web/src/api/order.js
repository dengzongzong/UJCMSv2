import request from '@/utils/request'

/**
 * 创建课程订单(返回支付二维码)
 * @param {number} courseId 课程ID
 * @param {string} [channel] 支付渠道 wechat/alipay,不传用默认
 */
export function createOrder(courseId, channel) {
  return request({
    url: '/user/order/create',
    method: 'post',
    params: { courseId, channel }
  })
}

/**
 * 我的订单列表
 */
export function getMyOrders() {
  return request({
    url: '/user/order/my',
    method: 'get'
  })
}

/**
 * 订单详情(轮询支付状态)
 */
export function getOrderByNo(orderNo) {
  return request({
    url: '/user/order/' + orderNo,
    method: 'get'
  })
}
