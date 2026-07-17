import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: { ...data, role: 'admin', agreement: true }
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function getAdminInfo() {
  return request({
    url: '/auth/info',
    method: 'get'
  })
}

export function changePassword(data) {
  return request({
    url: '/auth/reset-password',
    method: 'post',
    data
  })
}
