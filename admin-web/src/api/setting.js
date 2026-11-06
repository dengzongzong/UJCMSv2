import request from '@/utils/request'

// 专业列表体积较大(约 37KB),多个页面会重复拉取;做 10 分钟内存缓存,
// 降低带宽占用和首屏等待。任何新增/修改/删除后立即失效。
const PROFESSION_CACHE_TTL = 10 * 60 * 1000
let professionCache = { data: null, key: '', time: 0 }

function clearProfessionCache() {
  professionCache = { data: null, key: '', time: 0 }
}

function isProfessionCacheValid(key) {
  return (
    professionCache.data &&
    professionCache.key === key &&
    Date.now() - professionCache.time < PROFESSION_CACHE_TTL
  )
}

// 专业
export function professions(params) {
  const key = params ? JSON.stringify(params) : ''
  if (isProfessionCacheValid(key)) {
    const data = Array.isArray(professionCache.data)
      ? professionCache.data.slice()
      : professionCache.data
    return Promise.resolve({ data })
  }
  return request({ url: '/admin/setting/professions', method: 'get', params }).then(res => {
    professionCache = { data: res.data, key, time: Date.now() }
    return res
  })
}

export function addProfession(data) {
  clearProfessionCache()
  return request({ url: '/admin/setting/profession', method: 'post', data })
}

export function updateProfession(data) {
  clearProfessionCache()
  return request({ url: '/admin/setting/profession', method: 'put', data })
}

export function deleteProfession(id) {
  clearProfessionCache()
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

// 考试安全设置（人脸识别）
export function getFaceVerifyConfig() {
  return request({ url: '/admin/face-verify/config', method: 'get' })
}

export function saveFaceVerifyConfig(data) {
  return request({ url: '/admin/face-verify/config', method: 'put', data })
}

export function getFaceVerifyStats(date) {
  return request({ url: '/admin/face-verify/stats', method: 'get', params: { date } })
}

export function getFaceVerifyLogs(params) {
  return request({ url: '/admin/face-verify/logs', method: 'get', params })
}
