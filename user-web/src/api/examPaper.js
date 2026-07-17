import request from '@/utils/request'

/**
 * 用户端 - 试卷查看
 * <p>查看某个考试对应的试卷(题目 + 选项 + 参考答案 + 该用户的历史作答)</p>
 */
export function viewPaper(examId) {
  return request({ url: '/user/exam/paper/view', method: 'get', params: { examId } })
}

/**
 * 兼容旧接口名
 */
export function getExamPaper(examId) {
  return viewPaper(examId)
}
