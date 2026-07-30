import request from '@/utils/request'

export function cooperationApplyPage(params) {
  return request({ url: '/admin/cooperation-apply/page', method: 'get', params })
}

export function addCooperationApply(data) {
  return request({ url: '/admin/cooperation-apply', method: 'post', data })
}

export function updateCooperationApply(data) {
  return request({ url: '/admin/cooperation-apply', method: 'put', data })
}

export function deleteCooperationApply(id) {
  return request({ url: '/admin/cooperation-apply/' + id, method: 'delete' })
}

export function batchDeleteCooperationApply(ids) {
  return request({ url: '/admin/cooperation-apply/batch', method: 'delete', data: ids })
}

// 授权培育基地证书内容(全局单条,保留兼容)
export function getCertContent() {
  return request({ url: '/admin/cooperation-cert-content', method: 'get' })
}

export function saveCertContent(data) {
  return request({ url: '/admin/cooperation-cert-content', method: 'put', data })
}

// 按合作单位 id 获取/保存证书内容(每个单位各自维护)
export function getCertContentById(id) {
  return request({ url: '/admin/cooperation-apply/' + id + '/cert-content', method: 'get' })
}

export function saveCertContentById(id, data) {
  return request({ url: '/admin/cooperation-apply/' + id + '/cert-content', method: 'put', data })
}
