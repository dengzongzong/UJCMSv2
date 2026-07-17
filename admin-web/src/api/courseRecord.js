import request from '@/utils/request'

export function courseRecordPage(params) {
  return request({ url: '/admin/course-record/page', method: 'get', params })
}

export function courseRecordDetail(id) {
  return request({ url: '/admin/course-record/' + id, method: 'get' })
}

export function exportCourseRecord(params) {
  return request({ url: '/admin/course-record/export', method: 'get', params, responseType: 'blob' })
}
