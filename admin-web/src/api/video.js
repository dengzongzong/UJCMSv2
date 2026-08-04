import request from '@/utils/request'

export function videoPage(params) {
  return request({ url: '/admin/video/page', method: 'get', params })
}

export function videoDetail(id) {
  return request({ url: '/admin/video/' + id, method: 'get' })
}

export function addVideo(data) {
  return request({ url: '/admin/video', method: 'post', data })
}

export function updateVideo(data) {
  return request({ url: '/admin/video', method: 'put', data })
}

export function deleteVideo(id) {
  return request({ url: '/admin/video/' + id, method: 'delete' })
}

export function batchDeleteVideos(ids) {
  return request({ url: '/admin/video/batch', method: 'delete', data: ids })
}

export function sortVideo(params) {
  return request({ url: '/admin/video/sort', method: 'get', params })
}
