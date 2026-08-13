import request from '@/utils/request'

/**
 * 学员端证书查询(POST /portal/certificate/search)
 * @param {Object} payload - { idCard, name, certNo?, page?, size? }
 *   - idCard 必填, 6-32 字符
 *   - name   必填, 1-32 字符
 *   - 后端会强制以 (idCard + name) 同时匹配, 防止越权
 * @returns {Promise<{ total, page, size, records: Array }>}
 */
export function searchMyCertificates(payload) {
  return request({
    url: '/portal/certificate/search',
    method: 'post',
    data: payload
  })
}

/**
 * 已登录用户查询自己的证书(GET /portal/certificate/my-certificates)
 * - 走 JWT 鉴权,后端按当前登录用户取其名下证书,无需身份证+姓名
 * @returns {Promise<Array>}
 */
export function getMyCertificates() {
  return request({
    url: '/portal/certificate/my-certificates',
    method: 'get'
  })
}

/**
 * 已登录用户查询自己的考试记录(GET /portal/certificate/my-exam-records)
 * - 走 JWT 鉴权,按专业分组取最高分
 * @returns {Promise<Array>}
 */
export function getMyExamRecords() {
  return request({
    url: '/portal/certificate/my-exam-records',
    method: 'get'
  })
}

/**
 * 构造下载证书的 URL(用于浏览器 <a> 下载或 fetch+blob)
 *
 * - 完整 URL 由 baseURL='/api' + 路径拼接而成
 * - 路径上带 idCard + name, 后端会再次校验, 防止越权
 * - idCard 优先取后端在 search 时返的 idCardRaw(真实值),其次才用列表里的脱敏 idCard
 *   (避免脱敏串回传后端匹配失败)
 * - format: 'image'(PNG) 或 'pdf'
 */
export function buildCertificateDownloadUrl(cert, format) {
  var fmt = format === 'pdf' ? 'pdf' : 'image'
  var params = new URLSearchParams()
  // idCard/name 可选(仅凭证书编号查询时可能没有)
  var idCard = cert.idCardRaw || cert.idCard || ''
  if (idCard) params.append('idCard', idCard)
  if (cert.name) params.append('name', cert.name)
  params.append('format', fmt)
  // 加时间戳防止浏览器缓存(模板更新后预览能看到最新效果)
  params.append('_t', String(Date.now()))
  return '/api/portal/certificate/download/' + cert.id + '?' + params.toString()
}

/**
 * 通过 fetch 下载证书并触发浏览器保存。
 *
 * - 走原生 fetch 而非 axios,因为返回的是二进制流
 * - 自动从 localStorage.token 取 student JWT 注入 Authorization 头
 * - 文件名优先使用后端 Content-Disposition;否则按 姓名_身份证后6位.png/pdf
 *
 * @param {Object} cert - 列表里的证书对象,含 id/name/idCard
 * @param {string} [format='image'] - 'image' | 'pdf'
 * @returns {Promise<{ fileName: string, blob: Blob }>}
 */
export async function downloadCertificate(cert, format) {
  var url = buildCertificateDownloadUrl(cert, format)
  var token = localStorage.getItem('token') || ''

  var headers = { Accept: '*/*' }
  if (token) headers['Authorization'] = 'Bearer ' + token

  var init = { method: 'get', headers: headers, credentials: 'include' }
  var resp = await fetch(url, init)
  if (!resp.ok) {
    var msg = '下载失败'
    try {
      var data = await resp.json()
      if (data && data.message) msg = data.message
    } catch (_e) { /* blob, ignore */ }
    throw new Error(msg + ' (HTTP ' + resp.status + ')')
  }

  var fileName = ''
  var cd = resp.headers.get('Content-Disposition') || ''
  var m = cd.match(/filename\*?=(?:UTF-8'')?["']?([^;"']+)/i)
  if (m) {
    fileName = decodeURIComponent(m[1])
  }
  if (!fileName) {
    var name = cert.name || 'certificate'
    var idTail = (cert.idCard || '').slice(-6) || String(cert.id)
    fileName = name + '_' + idTail + (format === 'pdf' ? '.pdf' : '.png')
  }

  var blob = await resp.blob()
  return { fileName: fileName, blob: blob }
}

/**
 * 选择部分证书打包下载(ZIP) - 用户勾选多张证书中的几张,打包下载。
 *
 * - 走原生 fetch POST(返回二进制流)
 * - 后端按 (idCard + name) 双因子校验每条证书归属,防越权
 * - format: 'image'(PNG) 或 'pdf'
 *
 * @param {string} idCard 身份证号(查询时输入的原始值)
 * @param {string} name   姓名
 * @param {number[]} ids  选中的证书 ID 列表
 * @param {string} [format='image'] - 'image' | 'pdf'
 * @returns {Promise<{ fileName: string, blob: Blob }>}
 */
export async function downloadSelectedCertificates(idCard, name, ids, format) {
  var fmt = format === 'pdf' ? 'pdf' : 'image'
  var url = '/api/portal/certificate/download/selected'

  var token = localStorage.getItem('token') || ''
  var headers = { 'Content-Type': 'application/json', Accept: '*/*' }
  if (token) headers['Authorization'] = 'Bearer ' + token

  var body = JSON.stringify({ idCard: idCard || '', name: name || '', ids: ids, format: fmt })
  var resp = await fetch(url, { method: 'post', headers: headers, body: body, credentials: 'include' })
  if (!resp.ok) {
    var msg = '下载失败'
    try {
      var data = await resp.json()
      if (data && data.message) msg = data.message
    } catch (_e) { /* blob, ignore */ }
    throw new Error(msg + ' (HTTP ' + resp.status + ')')
  }

  var fileName = 'certificates_selected_' + fmt + (fmt === 'pdf' ? '.pdf' : '.zip')
  var cd = resp.headers.get('Content-Disposition') || ''
  var m = cd.match(/filename\*?=(?:UTF-8'')?["']?([^;"']+)/i)
  if (m) fileName = decodeURIComponent(m[1])

  var blob = await resp.blob()
  return { fileName: fileName, blob: blob }
}

/**
 * 把 Blob 触发浏览器下载
 */
export function saveBlob(blob, fileName) {
  var url = window.URL.createObjectURL(blob)
  var a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(function () { window.URL.revokeObjectURL(url) }, 1000)
}

/**
 * 批量下载证书(ZIP 打包) - 一个人可有多张证书,一次性打包下载。
 *
 * - 走原生 fetch(返回二进制流),Authorization 仅在登录态时携带(未登录也能用)
 * - 后端按 (idCard + name) 双因子匹配该人全部证书,打包成 zip 返回
 * - format: 'image'(PNG) 或 'pdf'
 *
 * @param {string} idCard 身份证号(查询时输入的原始值)
 * @param {string} name   姓名
 * @param {string} [format='image'] - 'image' | 'pdf'
 * @returns {Promise<{ fileName: string, blob: Blob }>}
 */
export async function downloadAllCertificates(idCard, name, format, certNo) {
  var fmt = format === 'pdf' ? 'pdf' : 'image'
  var params = new URLSearchParams()
  if (idCard) params.append('idCard', idCard)
  if (name) params.append('name', name)
  if (certNo) params.append('certNo', certNo)
  params.append('format', fmt)
  var url = '/api/portal/certificate/download/batch?' + params.toString()

  var token = localStorage.getItem('token') || ''
  var headers = { Accept: '*/*' }
  if (token) headers['Authorization'] = 'Bearer ' + token

  var resp = await fetch(url, { method: 'get', headers: headers, credentials: 'include' })
  if (!resp.ok) {
    var msg = '下载失败'
    try {
      var data = await resp.json()
      if (data && data.message) msg = data.message
    } catch (_e) { /* blob, ignore */ }
    throw new Error(msg + ' (HTTP ' + resp.status + ')')
  }

  var fileName = 'certificates_' + fmt + (fmt === 'pdf' ? '.pdf' : '.zip')
  var cd = resp.headers.get('Content-Disposition') || ''
  var m = cd.match(/filename\*?=(?:UTF-8'')?["']?([^;"']+)/i)
  if (m) fileName = decodeURIComponent(m[1])

  var blob = await resp.blob()
  return { fileName: fileName, blob: blob }
}
