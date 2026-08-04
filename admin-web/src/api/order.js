import request from '@/utils/request'

/**
 * 订单分页
 * @param {Object} params - { page, size, keyword, status, channel }
 */
export function orderPage(params) {
  return request({ url: '/admin/order/page', method: 'get', params })
}

/**
 * 支付总开关状态
 */
export function getPaySwitch() {
  return request({ url: '/admin/order/pay-switch', method: 'get' })
}

/**
 * 设置支付总开关
 * @param {boolean} enabled
 */
export function setPaySwitch(enabled) {
  return request({ url: '/admin/order/pay-switch', method: 'put', data: { enabled } })
}
