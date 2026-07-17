import request from '@/utils/request'

export function studentPage(params) {
  return request({ url: '/admin/student/page', method: 'get', params })
}

export function addStudent(data) {
  return request({ url: '/admin/student', method: 'post', data })
}

export function updateStudent(id, data) {
  return request({ url: '/admin/student/' + id, method: 'put', data })
}

export function deleteStudent(id) {
  return request({ url: '/admin/student/' + id, method: 'delete' })
}

export function batchDeleteStudents(ids) {
  return request({ url: '/admin/student/batch', method: 'delete', data: ids })
}

export function studentDetail(id) {
  return request({ url: '/admin/student/' + id, method: 'get' })
}

export function freezeStudent(id) {
  return request({ url: '/admin/student/freeze/' + id, method: 'put' })
}

export function getStudentCourses(id) {
  return request({ url: '/admin/student/' + id + '/courses', method: 'get' })
}

export function openCourses(data) {
  return request({ url: '/admin/student/open-courses', method: 'post', data })
}

export function getStudentExams(id) {
  return request({ url: '/admin/student/' + id + '/exams', method: 'get' })
}

export function openExams(data) {
  return request({ url: '/admin/student/open-exams', method: 'post', data })
}

export function importStudents(data) {
  return request({ url: '/admin/student/import', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' } })
}

export function downloadTemplate() {
  return request({ url: '/admin/student/import/template', method: 'get', responseType: 'blob' })
}
