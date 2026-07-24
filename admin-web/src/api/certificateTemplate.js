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
export function getExportColumns(id) {
  return request({ url: '/admin/certificate/template/' + id + '/export-columns', method: 'get' })
}
export function saveExportColumns(id, data) {
  return request({ url: '/admin/certificate/template/' + id + '/export-columns', method: 'put', data })
}
