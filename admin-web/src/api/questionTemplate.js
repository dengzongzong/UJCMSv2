import request from '@/utils/request'

export function questionTemplatePage(params) {
  return request({ url: '/admin/question-template/page', method: 'get', params })
}

export function questionTemplateList() {
  return request({ url: '/admin/question-template/list', method: 'get' })
}

export function questionTemplateDetail(id) {
  return request({ url: '/admin/question-template/' + id, method: 'get' })
}

export function addQuestionTemplate(data) {
  return request({ url: '/admin/question-template', method: 'post', data })
}

export function updateQuestionTemplate(data) {
  return request({ url: '/admin/question-template', method: 'put', data })
}

export function deleteQuestionTemplate(id) {
  return request({ url: '/admin/question-template/' + id, method: 'delete' })
}
