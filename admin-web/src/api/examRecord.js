import request from '@/utils/request'

export function examRecordPage(params) {
  return request({ url: '/admin/exam-record/page', method: 'get', params })
}

export function examRecordDetail(id) {
  return request({ url: '/admin/exam-record/' + id, method: 'get' })
}

export function exportExamRecord(params) {
  return request({ url: '/admin/exam-record/export', method: 'get', params, responseType: 'blob' })
}

export function gradeExamRecord(recordId, data) {
  return request({
    url: '/admin/exam-record/' + recordId + '/grade',
    method: 'post',
    data
  })
}

export function deleteExamRecord(id) {
  return request({ url: '/admin/exam-record/' + id, method: 'delete' })
}

export function batchDeleteExamRecords(ids) {
  return request({ url: '/admin/exam-record/batch', method: 'delete', data: ids })
}
