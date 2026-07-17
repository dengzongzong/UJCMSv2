import request from '@/utils/request'

/**
 * 学员端 - 合作咨询/网站声明/投诉建议
 */

/** 拉取合作咨询配置(弹窗内容) */
export function getCooperationSetting() {
  return request({ url: '/public/cooperation', method: 'get' })
}

/** 拉取网站声明 */
export function getDeclaration() {
  return request({ url: '/public/declaration', method: 'get' })
}

/** 提交留言(合作咨询/投诉建议/网站声明反馈) */
export function submitFeedback(data) {
  return request({ url: '/public/feedback', method: 'post', data })
}
