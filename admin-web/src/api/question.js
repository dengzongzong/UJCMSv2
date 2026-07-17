import request from '@/utils/request'

export function questionPage(params) {
  return request({ url: '/admin/question/page', method: 'get', params })
}

export function questionDetail(id) {
  return request({ url: '/admin/question/' + id, method: 'get' })
}

// 题干重复检测：返回库中已存在的相同题干题目详情列表(含选项/答案/解析)，供对比展示
export function checkQuestionDuplicate(params) {
  return request({ url: '/admin/question/check-duplicate', method: 'get', params })
}

export function addQuestion(data) {
  return request({ url: '/admin/question', method: 'post', data })
}

export function updateQuestion(data) {
  return request({ url: '/admin/question', method: 'put', data })
}

export function deleteQuestion(id) {
  return request({ url: '/admin/question/' + id, method: 'delete' })
}

export function batchDeleteQuestions(ids) {
  return request({ url: '/admin/question/batch', method: 'delete', data: ids })
}

export function exportQuestion(params) {
  return request({ url: '/admin/question/export', method: 'get', params, responseType: 'blob' })
}

export function downloadTemplate() {
  return request({ url: '/admin/question/template', method: 'get', responseType: 'blob' })
}

export function importQuestion(data) {
  return request({ url: '/admin/question/import', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' } })
}
