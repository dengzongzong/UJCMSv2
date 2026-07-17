import request from '@/utils/request'

export function templateList() {
  return request({ url: '/admin/certificate/template/list', method: 'get' })
}
export function templateDetail(id) {
  return request({ url: '/admin/certificate/template/' + id, method: 'get' })
}
export function saveTemplate(data) {
  return request({ url: '/admin/certificate/template', method: 'post', data })
}
export function deleteTemplate(id) {
  return request({ url: '/admin/certificate/template/' + id, method: 'delete' })
}
export function setDefaultTemplate(id) {
  return request({ url: '/admin/certificate/template/' + id + '/default', method: 'post' })
}
