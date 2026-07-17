import request from '@/utils/request'

export function coursePage(params) {
  return request({ url: '/admin/course/page', method: 'get', params })
}

export function courseDetail(id) {
  return request({ url: '/admin/course/' + id, method: 'get' })
}

export function addCourse(data) {
  return request({ url: '/admin/course', method: 'post', data })
}

export function updateCourse(data) {
  return request({ url: '/admin/course', method: 'put', data })
}

export function deleteCourse(id) {
  return request({ url: '/admin/course/' + id, method: 'delete' })
}

export function batchDeleteCourses(ids) {
  return request({ url: '/admin/course/batch', method: 'delete', data: ids })
}

export function getCourseStudents(id, params) {
  return request({ url: '/admin/course/' + id + '/students', method: 'get', params })
}

export function openCourseStudents(data) {
  return request({ url: '/admin/course/open-students', method: 'post', data })
}

export function closeCourseStudent(courseId, studentId) {
  return request({ url: '/admin/course/close-student', method: 'delete', params: { courseId, studentId } })
}
