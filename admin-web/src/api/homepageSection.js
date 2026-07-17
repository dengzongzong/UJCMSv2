import request from '@/utils/request'

export function homepageSectionPage(params) {
  return request({ url: '/admin/homepage-section/page', method: 'get', params })
}

export function addHomepageSection(data) {
  return request({ url: '/admin/homepage-section', method: 'post', data })
}

export function updateHomepageSection(data) {
  return request({ url: '/admin/homepage-section', method: 'put', data })
}

export function deleteHomepageSection(id) {
  return request({ url: '/admin/homepage-section/' + id, method: 'delete' })
}

export function batchDeleteHomepageSection(ids) {
  return request({ url: '/admin/homepage-section/batch', method: 'delete', data: ids })
}
