import request from '@/utils/request'

export function friendlyLinkPage(params) {
  return request({ url: '/admin/friendly-link/page', method: 'get', params })
}

export function addFriendlyLink(data) {
  return request({ url: '/admin/friendly-link', method: 'post', data })
}

export function updateFriendlyLink(data) {
  return request({ url: '/admin/friendly-link', method: 'put', data })
}

export function deleteFriendlyLink(ids) {
  return request({ url: '/admin/friendly-link', method: 'delete', data: ids })
}
