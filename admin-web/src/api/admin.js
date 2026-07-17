import request from '@/utils/request'

export function adminPage(params) {
  return request({ url: '/admin/admin/page', method: 'get', params })
}

export function addAdmin(data) {
  return request({ url: '/admin/admin', method: 'post', data })
}

export function updateAdmin(data) {
  return request({ url: '/admin/admin', method: 'put', data })
}

export function deleteAdmin(id) {
  return request({ url: '/admin/admin/' + id, method: 'delete' })
}

export function getAdminDetail(id) {
  return request({ url: '/admin/admin/' + id, method: 'get' })
}

export function getCurrentAdmin() {
  return request({ url: '/admin/account/current', method: 'get' })
}

export function changeAdminPassword(data) {
  return request({ url: '/admin/account/password', method: 'post', data })
}
