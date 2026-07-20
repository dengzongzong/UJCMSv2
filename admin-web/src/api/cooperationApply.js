import request from '@/utils/request'

export function cooperationApplyPage(params) {
  return request({ url: '/cooperation-apply/page', method: 'get', params })
}

export function addCooperationApply(data) {
  return request({ url: '/cooperation-apply', method: 'post', data })
}

export function updateCooperationApply(data) {
  return request({ url: '/cooperation-apply', method: 'put', data })
}

export function deleteCooperationApply(id) {
  return request({ url: '/cooperation-apply/' + id, method: 'delete' })
}

export function batchDeleteCooperationApply(ids) {
  return request({ url: '/cooperation-apply/batch', method: 'delete', data: ids })
}
