import request from '@/utils/request'

export function fieldList(type) {
  return request({ url: '/admin/certificate/field/list', method: 'get', params: { type } })
}
export function addField(data) {
  return request({ url: '/admin/certificate/field', method: 'post', data })
}
export function updateField(data) {
  return request({ url: '/admin/certificate/field', method: 'put', data })
}
export function deleteField(id) {
  return request({ url: '/admin/certificate/field/' + id, method: 'delete' })
}
