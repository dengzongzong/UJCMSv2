import request from '@/utils/request'

/**
 * 订单分页
 * @param {Object} params - { page, size, keyword, status, channel }
 */
export function orderPage(params) {
  return request({ url: '/admin/order/page', method: 'get', params })
}
