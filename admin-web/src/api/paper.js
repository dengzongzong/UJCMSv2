import request from '@/utils/request'

export function paperPage(params) {
  return request({ url: '/admin/paper/page', method: 'get', params })
}

export function paperDetail(id) {
  return request({ url: '/admin/paper/' + id, method: 'get' })
}

export function paperList() {
  return request({ url: '/admin/paper/list', method: 'get' })
}

export function addPaper(data) {
  return request({ url: '/admin/paper', method: 'post', data })
}

export function updatePaper(data) {
  return request({ url: '/admin/paper', method: 'put', data })
}

export function deletePaper(id) {
  return request({ url: '/admin/paper/' + id, method: 'delete' })
}

export function batchDeletePapers(ids) {
  return request({ url: '/admin/paper/batch', method: 'delete', data: ids })
}

export function autoGeneratePaper(data) {
  return request({ url: '/admin/paper/auto-generate', method: 'post', data })
}
