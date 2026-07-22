import request from '@/utils/request'

/**
 * 公共接口(/public/**)
 * 学员端首页/课程详情等页面用
 */
export function getBanners() {
  return request({ url: '/public/banner/list', method: 'get' })
}

export function getBannerImages() {
  return request({ url: '/public/banner-images', method: 'get' })
}

export function getNewsList() {
  return request({ url: '/public/news', method: 'get' })
}

export function getEventsList() {
  return request({ url: '/public/events', method: 'get' })
}

export function getAnnouncements() {
  return request({ url: '/public/announcements', method: 'get' })
}

export function getAnnouncementList() {
  return request({ url: '/public/announcements', method: 'get' })
}

export function getProfessions() {
  return request({ url: '/public/professions', method: 'get' })
}

/**
 * 友情链接(学员端首页底部 2 行 5 列展示)
 */
export function getFriendlyLinks() {
  return request({ url: '/public/friendly-link/list', method: 'get' })
}

/**
 * 课程关联三图(学员端课程详情页视频下方)
 * @param {Number|Long} courseId 课程ID;不传则拉全站通用三图
 */
export function getCourseThreeImages(courseId) {
  return request({
    url: '/public/course-three-image/list',
    method: 'get',
    params: courseId ? { courseId } : {}
  })
}

/**
 * 公开课程列表(学员端首页/未登录用户也能浏览)
 * @param {Object} params - { professionId, subjectId, categoryId }
 */
export function getPublicCourseList(params) {
  return request({
    url: '/public/course/list',
    method: 'get',
    params
  })
}

/**
 * 获取全部视频分类(课程中心按分类分组展示用,公开接口)
 */
export function getVideoCategories() {
  return request({ url: '/public/video-categories', method: 'get' })
}

/**
 * 获取首页内容板块(政策法规/信息公开)
 * type: 1-政策法规 2-信息公开, 不传返回全部
 */
export function getHomepageSections(type) {
  return request({
    url: '/public/homepage-sections',
    method: 'get',
    params: type != null ? { type } : {}
  })
}

/**
 * 通用搜索 - 搜索新闻、公告、课程、考试、政策法规
 */
export function searchAll(keyword) {
  return request({
    url: '/public/search',
    method: 'get',
    params: { keyword }
  })
}
