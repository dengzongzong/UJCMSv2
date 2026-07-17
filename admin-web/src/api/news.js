import request from '@/utils/request'

export function newsPage(params) {
  return request({ url: '/admin/news/page', method: 'get', params })
}

export function addNews(data) {
  return request({ url: '/admin/news', method: 'post', data })
}

export function updateNews(data) {
  return request({ url: '/admin/news', method: 'put', data })
}

export function deleteNews(id) {
  return request({ url: '/admin/news/' + id, method: 'delete' })
}

export function batchDeleteNews(ids) {
  return request({ url: '/admin/news/batch', method: 'delete', data: ids })
}
