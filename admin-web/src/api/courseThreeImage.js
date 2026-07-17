import request from '@/utils/request'

export function courseThreeImagePage(params) {
  return request({ url: '/admin/course-three-image/page', method: 'get', params })
}

export function addCourseThreeImage(data) {
  return request({ url: '/admin/course-three-image', method: 'post', data })
}

export function updateCourseThreeImage(data) {
  return request({ url: '/admin/course-three-image', method: 'put', data })
}

export function deleteCourseThreeImage(ids) {
  return request({ url: '/admin/course-three-image', method: 'delete', data: ids })
}
