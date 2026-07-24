import request from '@/utils/request'
import { apiUrl } from '@/utils/apiBase'

// 证书管理
export function certificatePage(params) {
  return request({ url: '/admin/certificate/page', method: 'get', params })
}
export function certificateDetail(id) {
  return request({ url: '/admin/certificate/' + id, method: 'get' })
}
export function addCertificate(data) {
  return request({ url: '/admin/certificate', method: 'post', data })
}
export function updateCertificate(data) {
  return request({ url: '/admin/certificate', method: 'put', data })
}
export function deleteCertificate(ids) {
  return request({ url: '/admin/certificate', method: 'delete', data: ids })
}
export function importCertificate(file) {
  return request({
    url: '/admin/certificate/import',
    method: 'post',
    data: file,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export function switchExamQr(data) {
  return request({ url: '/admin/certificate/exam-qr/switch', method: 'post', data })
}
// 批量生成证书:
// - 数量 < 50: 后端 /batch 同步流式返回 zip(经此次修复,后端不再 throw "请使用 batch-sync")
// - 数量 >= 50: 后端 /batch 返回 { taskId, async: true },前端交给任务中心
export function generateCertificateBatch(data) {
  return request({ url: '/admin/certificate/generate/batch', method: 'post', data })
}

// 按用户发证书(选学员 -> 一键生成 cert 记录)
export function issueCertificates(data) {
  return request({ url: '/admin/certificate/issue', method: 'post', data })
}

/** 文件下载:返回 {blob, fileName},自动带 JWT 鉴权 */
/**
 * 通用文件下载助手
 * @param {string} url - 接口 URL(不带 baseApi)
 * @param {object} [opts] - { method, params, data, headers, token }
 * @returns {Promise<Blob>}
 */
export function downloadFile(url, opts) {
  opts = opts || {}
  // 使用 apiUrl 统一处理 base path,避免 .env 没加载时拼出 "undefined/xxx"
  const fullUrl = apiUrl(url)
  const headers = Object.assign({ Accept: '*/*' }, opts.headers || {})
  // 注入 JWT:
  // 1) 调用方显式传入的 opts.token 优先
  // 2) 其次用 Vuex 内存中的 token(从 @/store 取,避免依赖 vuex persistedstate)
  // 3) 兜底从 localStorage.admin_token 取(项目里 store/modules/admin.js 自己写的持久化)
  // 4) 最后从 localStorage.vuex / sessionStorage.vuex 取(兼容未来装 vuex-persistedstate)
  if (opts.token) {
    headers['Authorization'] = 'Bearer ' + opts.token
  } else {
    let token = ''
    try {
      // 1) 内存里的 store(避免循环 import,延迟取)
      // 2) 项目自带的 localStorage 键 admin_token
      token = localStorage.getItem('admin_token') || ''
      // 3) 兼容未来装 vuex-persistedstate 后的键
      if (!token) {
        const raw = localStorage.getItem('vuex') || sessionStorage.getItem('vuex') || ''
        if (raw) {
          const state = JSON.parse(raw)
          token = state.token || (state.auth && state.auth.token) || ''
        }
      }
    } catch (ignore) { /* 取不到就当没 token */ }
    if (token) headers['Authorization'] = 'Bearer ' + token
  }
  return fetch(fullUrl, {
    method: opts.method || 'get',
    headers,
    credentials: 'include'
  }).then(r => {
    if (!r.ok) throw new Error('HTTP ' + r.status + (r.status === 401 ? ' (未登录)' : ''))
    const cd = r.headers.get('Content-Disposition') || ''
    return r.blob().then(blob => ({ blob, fileName: parseFileName(cd) }))
  })
}
function parseFileName(cd) {
  if (!cd) return ''
  const m = cd.match(/filename\*=utf-8''([^;]+)/)
  if (m) return decodeURIComponent(m[1])
  const m2 = cd.match(/filename="?([^";]+)"?/)
  if (m2) return m2[1]
  return ''
}
/** 触发浏览器下载 Blob */
export function triggerDownload(blob, fileName) {
  const a = document.createElement('a')
  const url = URL.createObjectURL(blob)
  a.href = url
  a.download = fileName || 'download'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => URL.revokeObjectURL(url), 100)
}

/**
 * 单张证书下载(后台) - GET /admin/certificate/generate/single/{id}
 * @param {number} id 证书记录 ID
 * @param {string} format 'image' | 'pdf'
 * @returns {Promise<{ fileName: string, blob: Blob }>}
 */
export async function downloadSingleCertificate(id, format) {
  var fmt = format === 'pdf' ? 'pdf' : 'image'
  var fullUrl = apiUrl('/admin/certificate/generate/single/' + id + '?format=' + fmt)
  var headers = { Accept: '*/*' }
  // 与 downloadCertificateBatch 一致的 token 获取方式
  var token = ''
  try {
    token = localStorage.getItem('admin_token') || ''
    if (!token) {
      var raw = localStorage.getItem('vuex') || sessionStorage.getItem('vuex') || ''
      if (raw) {
        var state = JSON.parse(raw)
        token = state.token || (state.auth && state.auth.token) || ''
      }
    }
  } catch (e) { /* ignore */ }
  if (token) {
    headers['Authorization'] = 'Bearer ' + token
    headers['Admin-Token'] = token
  }
  var resp = await fetch(fullUrl, { method: 'get', headers: headers, credentials: 'include' })
  if (!resp.ok) {
    var msg = '下载失败'
    try {
      var data = await resp.json()
      if (data && data.message) msg = data.message
    } catch (_e) { /* ignore */ }
    throw new Error(msg + ' (HTTP ' + resp.status + ')')
  }
  var fileName = 'certificate_' + id + '.' + (fmt === 'pdf' ? 'pdf' : 'png')
  var cd = resp.headers.get('Content-Disposition') || ''
  var m = cd.match(/filename\*?=(?:UTF-8'')?["']?([^;"']+)/i)
  if (m) fileName = decodeURIComponent(m[1])
  var blob = await resp.blob()
  return { fileName: fileName, blob: blob }
}

/**
 * 批量下载证书(后台) - 一个人可有多张证书,选中多条后打包下载。
 * POST /admin/certificate/generate/batch
 * - < 50 张: 后端同步返回 ZIP,直接触发浏览器下载
 * - >= 50 张: 后端返回 { taskId, async: true },交给任务中心
 *
 * @param {number[]} ids 证书ID列表
 * @param {string} format 'image' | 'pdf'
 * @returns {Promise<{async?:boolean, taskId?:string, message?:string, blob?:Blob, fileName?:string}>}
 */
export function downloadCertificateBatch(ids, format) {
  const fullUrl = apiUrl('/admin/certificate/generate/batch')
  const headers = { Accept: '*/*', 'Content-Type': 'application/json' }
  let token = ''
  try {
    token = localStorage.getItem('admin_token') || ''
    if (!token) {
      const raw = localStorage.getItem('vuex') || sessionStorage.getItem('vuex') || ''
      if (raw) {
        const state = JSON.parse(raw)
        token = state.token || (state.auth && state.auth.token) || ''
      }
    }
  } catch (e) { /* ignore */ }
  if (token) headers['Authorization'] = 'Bearer ' + token
  return fetch(fullUrl, {
    method: 'post',
    headers,
    credentials: 'include',
    body: JSON.stringify({ ids, format })
  }).then(r => {
    if (!r.ok) throw new Error('HTTP ' + r.status + (r.status === 401 ? ' (未登录)' : ''))
    const ct = r.headers.get('Content-Type') || ''
    const cd = r.headers.get('Content-Disposition') || ''
    // 异步任务: 后端返回 JSON { code, message, data:{taskId, async:true} }
    if (ct.indexOf('application/json') >= 0) {
      return r.json().then(data => ({
        async: true,
        taskId: data && data.data && data.data.taskId,
        message: data && data.message
      }))
    }
    // 同步: ZIP 二进制流
    return r.blob().then(blob => ({ blob, fileName: parseFileName(cd) }))
  })
}

// 学员照片
/**
 * 批量导入照片(支持多文件)
 * - files: FormData 中可重复的 'files' 字段
 * - 后端按文件名解析身份证号
 */
export function importPhotoBatch(data) {
  return request({
    url: '/admin/certificate/photo/batch-import',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function photoPage(params) {
  return request({ url: '/admin/certificate/photo/page', method: 'get', params })
}

export function addPhoto(data) {
  return request({ url: '/admin/certificate/photo', method: 'post', data })
}

/** 为指定证书记录上传照片(支持同一个人不同证书设置不同照片) */
export function uploadPhotoForCertificate(data) {
  return request({
    url: '/admin/certificate/photo/upload-for-certificate',
    method: 'post',
    data
    // 不手动设置 Content-Type,让浏览器自动设置 multipart/form-data + boundary
  })
}

/** 查询某个身份证号下的所有证书记录(用于选择关联证书) */
export function getCertificatesByIdCard(idCard) {
  return request({ url: '/admin/certificate/photo/certs-by-idcard', method: 'get', params: { idCard } })
}

export function deletePhoto(data) {
  return request({ url: '/admin/certificate/photo', method: 'delete', data })
}

// 编号配置
export function getNumberConfig() {
  return request({ url: '/admin/certificate/number-config', method: 'get' })
}
export function updateNumberConfig(data) {
  return request({ url: '/admin/certificate/number-config', method: 'put', data })
}

// 按列表筛选条件获取所有已绑定模板的证书 ID(用于"下载全部证书")
export function certificateAllIds(params) {
  return request({ url: '/admin/certificate/all-ids', method: 'get', params })
}

// URL 配置(证书二维码生成规则)
export function getUrlConfig() {
  return request({ url: '/admin/certificate/url-config', method: 'get' })
}
export function updateUrlConfig(data) {
  return request({ url: '/admin/certificate/url-config', method: 'put', data })
}

// ============ 证书用户管理 ============
export function certificateUserPage(params) {
  return request({ url: '/admin/certificate/user/page', method: 'get', params })
}

export function certificateUserSync(certType) {
  const params = certType ? { certType } : {}
  return request({ url: '/admin/certificate/user/sync', method: 'post', params })
}

export function addCertificateUser(data) {
  return request({ url: '/admin/certificate/user', method: 'post', data })
}

export function updateCertificateUser(data) {
  return request({ url: '/admin/certificate/user', method: 'put', data })
}

export function deleteCertificateUser(id) {
  return request({ url: '/admin/certificate/user/' + id, method: 'delete' })
}

/**
 * 导出证书数据(Excel,使用与导入模板相同的20列结构)
 * @param {object} params - { name, idCard, agency, profession, issueDateStart, issueDateEnd, ids }
 *   ids 为空时按筛选条件导出全部;ids 非空时导出选中数据
 * @returns {Promise<{blob:Blob, fileName:string}>}
 */
export async function exportCertificates(params) {
  const fullUrl = apiUrl('/admin/certificate/export')
  const headers = { Accept: '*/*' }
  let token = ''
  try {
    token = localStorage.getItem('admin_token') || ''
  } catch (e) { /* ignore */ }
  if (token) headers['Authorization'] = 'Bearer ' + token

  // 构造 query string
  const queryParts = []
  if (params.name) queryParts.push('name=' + encodeURIComponent(params.name))
  if (params.idCard) queryParts.push('idCard=' + encodeURIComponent(params.idCard))
  if (params.agency) queryParts.push('agency=' + encodeURIComponent(params.agency))
  if (params.profession) queryParts.push('profession=' + encodeURIComponent(params.profession))
  if (params.issueDateStart) queryParts.push('issueDateStart=' + encodeURIComponent(params.issueDateStart))
  if (params.issueDateEnd) queryParts.push('issueDateEnd=' + encodeURIComponent(params.issueDateEnd))
  if (params.ids && params.ids.length > 0) {
    params.ids.forEach(id => queryParts.push('ids=' + id))
  }
  if (params.templateId) queryParts.push('templateId=' + params.templateId)
  const qs = queryParts.length > 0 ? '?' + queryParts.join('&') : ''

  const resp = await fetch(fullUrl + qs, { method: 'get', headers, credentials: 'include' })
  if (!resp.ok) throw new Error('导出失败 (HTTP ' + resp.status + ')')
  const cd = resp.headers.get('Content-Disposition') || ''
  const blob = await resp.blob()
  return { blob, fileName: parseFileName(cd) || '证书数据导出.xlsx' }
}
