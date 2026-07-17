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

export function getVideoStudents(videoId, params) {
  return request({ url: '/admin/video/' + videoId + '/students', method: 'get', params })
}

export function openVideoStudents(videoId, studentIds) {
  return request({ url: '/admin/video/' + videoId + '/students', method: 'post', data: studentIds })
}

export function closeVideoStudent(videoId, studentId) {
  return request({ url: '/admin/video/' + videoId + '/students/' + studentId, method: 'delete' })
}
