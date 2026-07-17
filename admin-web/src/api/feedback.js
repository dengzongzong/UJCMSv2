import request from '@/utils/request'

// 合作咨询配置
export function getCooperationSetting() {
  return request({ url: '/admin/setting/cooperation', method: 'get' })
}

export function updateCooperationSetting(data) {
  return request({ url: '/admin/setting/cooperation', method: 'put', data })
}

// 网站声明
export function getDeclaration() {
  return request({ url: '/admin/setting/declaration', method: 'get' })
}

export function updateDeclaration(data) {
  return request({ url: '/admin/setting/declaration', method: 'put', data })
}

// 留言管理(合作咨询/投诉建议)
export function feedbackPage(params) {
  return request({ url: '/admin/feedback/page', method: 'get', params })
}

export function handleFeedback(id, remark) {
  return request({ url: '/admin/feedback/handle/' + id, method: 'put', data: { remark } })
}

export function deleteFeedback(ids) {
  return request({ url: '/admin/feedback', method: 'delete', data: ids })
}
