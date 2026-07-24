import request from '@/utils/request'

/**
 * 获取考试列表(需登录,返回已开通考试)
 * @param {Object} params - { professionId, subjectId, keyword, page, pageSize, purchasedOnly }
 */
export function getExamList(params) {
  return request({
    url: '/user/exam/list',
    method: 'get',
    params
  })
}

/**
 * 获取我已开通的考试(等价 getExamList,语义更清晰)
 * @param {Object} [params] - { page, pageSize, keyword }
 */
export function getMyExams(params) {
  var merged = Object.assign({ purchasedOnly: true }, params || {})
  return getExamList(merged)
}

/**
 * 考试访问权限校验(点开考试时调用)
 * - 未登录: 返回 code=1001
 * - 已登录未开通: 返回 code=1002
 * - 已开通: 放行
 */
export function checkExamAccess(examId) {
  return request({
    url: '/user/exam/check-access',
    method: 'get',
    params: { examId }
  })
}

/**
 * 查看试卷(已开通考试可看)
 */
export function viewPaper(examId) {
  return request({
    url: '/user/exam/paper/view',
    method: 'get',
    params: { examId }
  })
}

/**
 * 考试中心(公开): 未登录也可访问,登录后自动标记已开通
 * @param {string|number} [professionId] 专业 ID(可选)
 * @param {string|number} [subjectId]    科目 ID(可选)
 * @param {string}        [keyword]      搜索关键词(可选,后端按考试名称模糊匹配)
 * @param {number}        [page]         页码(可选,从 1 开始,传入后返回分页结构)
 * @param {number}        [pageSize]     每页条数(可选,最大 50)
 */
export function getPublicExamList(professionId, subjectId, keyword, page, pageSize) {
  var params = {}
  if (professionId) params.professionId = professionId
  if (subjectId) params.subjectId = subjectId
  if (keyword) params.keyword = keyword
  if (page) params.page = page
  if (pageSize) params.pageSize = pageSize
  return request({
    url: '/user/exam/public/list',
    method: 'get',
    params: params
  })
}

/**
 * 课程中心(公开列表): 未登录也可浏览
 */
export function getPublicCourseList(params) {
  return request({
    url: '/user/course/public/list',
    method: 'get',
    params
  })
}

/**
 * 获取试卷介绍
 */
export function getExamIntro(examId) {
  return request({
    url: '/user/exam/intro',
    method: 'get',
    params: { examId }
  })
}

/**
 * 开始考试
 */
export function startExam(examId) {
  return request({
    url: '/user/exam/start',
    method: 'post',
    params: { examId }
  })
}

/**
 * 获取考试试卷（题目列表）
 * 支持断点续考: 传入 recordId 时后端返回该记录已答的 userAnswer
 */
export function getExamPaper(examId, recordId) {
  var params = { examId: examId }
  if (recordId) params.recordId = recordId
  return request({
    url: '/user/exam/paper',
    method: 'get',
    params: params,
    silent: true
  })
}

/**
 * 保存单题答案(断点续考用,静默保存,失败不弹 Toast)
 */
export function saveExamAnswer(recordId, questionId, studentAnswer) {
  return request({
    url: '/user/exam/answer',
    method: 'post',
    data: { recordId, questionId, studentAnswer },
    silent: true
  })
}

/**
 * 提交考试
 */
export function submitExam(data) {
  return request({
    url: '/user/exam/submit',
    method: 'post',
    data
  })
}

/**
 * 自动交卷
 */
export function autoSubmitExam(data) {
  return request({
    url: '/user/exam/auto-submit',
    method: 'post',
    data
  })
}

/**
 * 获取考试结果
 */
export function getExamResult(recordId) {
  return request({
    url: '/user/exam/result',
    method: 'get',
    params: { recordId }
  })
}

/**
 * 获取考试记录列表
 * @param {number} [page]     页码(可选,从 1 开始,传入后返回分页结构含统计概览)
 * @param {number} [pageSize] 每页条数(可选,最大 50)
 */
export function getExamRecords(page, pageSize) {
  var params = {}
  if (page) params.page = page
  if (pageSize) params.pageSize = pageSize
  return request({
    url: '/user/exam/records',
    method: 'get',
    params
  })
}

/**
 * 获取当前登录用户考过的试卷(同专业取最高分)
 * - GET /user/exam/best-records
 * - 后端按当前登录用户的考试记录聚合,同一专业仅保留最高分
 * @returns {Promise<Array>}
 */
export function getBestRecords() {
  return request({
    url: '/user/exam/best-records',
    method: 'get'
  })
}
