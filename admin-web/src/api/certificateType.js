import request from '@/utils/request'

export function certificateTypeList() {
  return request({ url: '/admin/certificate-type/list', method: 'get' })
}

export function addCertificateType(data) {
  return request({ url: '/admin/certificate-type', method: 'post', data })
}

export function updateCertificateType(data) {
  return request({ url: '/admin/certificate-type', method: 'put', data })
}

export function deleteCertificateType(id) {
  return request({ url: '/admin/certificate-type/' + id, method: 'delete' })
}

// 公开接口(不需要登录,供前端下拉选择用)
export function publicCertificateTypes() {
  return request({ url: '/public/certificate-types', method: 'get' })
}
