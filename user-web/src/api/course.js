import request from '@/utils/request'

/**
 * 获取课程列表(需登录,返回已开通课程)
 * @param {Object} params - { professionId, page, pageSize }
 */
export function getCourseList(params) {
  return request({
    url: '/user/course/list',
    method: 'get',
    params
  })
}

/**
 * 课程中心(公开列表): 未登录也可浏览所有上架课程
 * @param {string|number} [professionId] 专业 ID(可选)
 * @param {string}        [keyword]      搜索关键词(可选,后端按课程名称模糊匹配)
 */
export function getPublicCourseList(professionId, keyword) {
  var params = {}
  if (professionId) params.professionId = professionId
  if (keyword) params.keyword = keyword
  return request({
    url: '/user/course/public/list',
    method: 'get',
    params: params
  })
}

/**
 * 获取课程详情
 */
export function getCourseDetail(courseId) {
  return request({
    url: '/user/course/detail',
    method: 'get',
    params: { courseId }
  })
}

/**
 * 获取我的课程列表
 */
export function getMyCourses() {
  return request({
    url: '/user/course/my',
    method: 'get'
  })
}

/**
 * 上报视频播放进度
 */
export function reportVideoProgress(data) {
  return request({
    url: '/user/course/video-progress',
    method: 'post',
    data
  })
}

/**
 * 获取视频播放信息
 */
export function getVideoInfo(videoId, courseId) {
  return request({
    url: '/user/course/video-info',
    method: 'get',
    params: { videoId, courseId }
  })
}

/**
 * 课程访问权限校验(点开课程时调用)
 * - 未登录: 返回 code=1001
 * - 已登录未开通: 返回 code=1002
 * - 已开通: 放行
 */
export function checkCourseAccess(courseId, options = {}) {
  return request({
    url: '/user/course/check-access',
    method: 'get',
    params: { courseId },
    silent: options.silent || false
  })
}
