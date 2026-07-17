import request from '@/utils/request'

export function examPage(params) {
  return request({ url: '/admin/exam/page', method: 'get', params })
}

export function examDetail(id) {
  return request({ url: '/admin/exam/' + id, method: 'get' })
}

export function addExam(data) {
  return request({ url: '/admin/exam', method: 'post', data })
}

export function updateExam(data) {
  return request({ url: '/admin/exam', method: 'put', data })
}

export function deleteExam(id) {
  return request({ url: '/admin/exam/' + id, method: 'delete' })
}

export function batchDeleteExams(ids) {
  return request({ url: '/admin/exam/batch', method: 'delete', data: ids })
}

export function getExamStudents(id, params) {
  return request({ url: '/admin/exam/' + id + '/students', method: 'get', params })
}

export function openExamStudents(data) {
  return request({ url: '/admin/exam/open-students', method: 'post', data })
}

export function closeExamStudent(examId, studentId) {
  return request({ url: '/admin/exam/close-student', method: 'delete', params: { examId, studentId } })
}

export function autoExam(data) {
  return request({ url: '/admin/exam/auto-exam', method: 'post', data })
}
