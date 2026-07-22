import request from '@/utils/request'

export function bannerImageList() {
  return request({ url: '/admin/banner-image/list', method: 'get' })
}

export function addBannerImage(data) {
  return request({ url: '/admin/banner-image', method: 'post', data })
}

export function updateBannerImage(data) {
  return request({ url: '/admin/banner-image', method: 'put', data })
}

export function deleteBannerImage(id) {
  return request({ url: '/admin/banner-image/' + id, method: 'delete' })
}
