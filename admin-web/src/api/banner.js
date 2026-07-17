import request from '@/utils/request'

export function bannerPage(params) {
  return request({ url: '/admin/banner/page', method: 'get', params })
}
export function bannerAdd(data) {
  return request({ url: '/admin/banner', method: 'post', data })
}
export function bannerUpdate(data) {
  return request({ url: '/admin/banner', method: 'put', data })
}
export function bannerDelete(id) {
  return request({ url: '/admin/banner/' + id, method: 'delete' })
}
