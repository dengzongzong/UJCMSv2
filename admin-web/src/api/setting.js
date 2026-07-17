import request from '@/utils/request'

// 专业
export function professions(params) {
  return request({ url: '/admin/setting/professions', method: 'get', params })
}

export function addProfession(data) {
  return request({ url: '/admin/setting/profession', method: 'post', data })
}

export function updateProfession(data) {
  return request({ url: '/admin/setting/profession', method: 'put', data })
}

export function deleteProfession(id) {
  return request({ url: '/admin/setting/profession/' + id, method: 'delete' })
}

// 关于我们
export function getAbout() {
  return request({ url: '/admin/setting/about', method: 'get' })
}

export function updateAbout(data) {
  return request({ url: '/admin/setting/about', method: 'put', data })
}

// 课程分类
export function videoCategories(params) {
  return request({ url: '/admin/setting/video-categories', method: 'get', params })
}

export function addVideoCategory(data) {
  return request({ url: '/admin/setting/video-category', method: 'post', data })
}

export function updateVideoCategory(data) {
  return request({ url: '/admin/setting/video-category', method: 'put', data })
}

export function deleteVideoCategory(id) {
  return request({ url: '/admin/setting/video-category/' + id, method: 'delete' })
}

// 题目分类
export function questionCategories(params) {
  return request({ url: '/admin/setting/question-categories', method: 'get', params })
}

export function addQuestionCategory(data) {
  return request({ url: '/admin/setting/question-category', method: 'post', data })
}

export function updateQuestionCategory(data) {
  return request({ url: '/admin/setting/question-category', method: 'put', data })
}

export function deleteQuestionCategory(id) {
  return request({ url: '/admin/setting/question-category/' + id, method: 'delete' })
}
