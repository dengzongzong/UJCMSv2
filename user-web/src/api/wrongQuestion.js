import request from '@/utils/request'

/**
 * 获取错题列表
 * @param {Object} params - { type, status, page, pageSize }
 */
export function getWrongQuestions(params) {
  return request({
    url: '/user/profile/wrong-questions',
    method: 'get',
    params
  })
}

/**
 * 删除错题
 * @param {Number|String} id - 错题ID
 */
export function deleteWrongQuestion(id) {
  return request({
    url: '/user/profile/wrong-question',
    method: 'delete',
    params: { wrongQuestionId: id }
  })
}

/**
 * 清空所有错题
 */
export function clearWrongQuestions() {
  return request({
    url: '/user/profile/wrong-questions/clear',
    method: 'delete'
  })
}

/**
 * 更新错题状态（如标记已掌握）
 * @param {Number|String} id - 错题ID
 * @param {Object} data - { mastered }
 */
export function updateWrongQuestion(id, data) {
  return request({
    url: '/user/profile/wrong-question',
    method: 'put',
    params: { id },
    data
  })
}
