/**
 * 获取后端 API base URL。
 *
 * 约定:
 * - 后端 Spring Boot 的 servlet context-path = /api
 * - axios 的 baseURL = '/api',所以业务请求走 /api/<x> 即可
 * - 原生 fetch (下载/图片/video) 必须自己拼 base,所以走 apiUrl()
 *
 * 优先级:
 * 1) process.env.VUE_APP_BASE_API
 *    - 开发期: 'http://localhost:8080' (绝对地址,后端 8080)
 *    - 生产期: '' (同源,由 nginx 反代 /api 到后端)
 * 2) 兜底:空字符串 (相对路径,同源)
 *
 * 任何业务路径都必须带 /api 前缀,所以 apiUrl(path) 会强制补 /api;
 * 但如果 path 已经以 /api 开头就不再补(避免 /api/api 这种)。
 */
export function apiBase() {
  let base = process.env.VUE_APP_BASE_API
  if (!base || base === 'undefined' || base === 'null') {
    return ''
  }
  return base
}

/**
 * 拼接完整 URL,自动补 /api 前缀。
 *
 * @param {string} path - 业务路径,必须以 '/' 开头
 * @returns {string} 完整 URL
 *
 * @example
 *   apiUrl('/user/profile/info')
 *   // dev:  'http://localhost:8080/api/user/profile/info'
 *   // prod: '/api/user/profile/info'
 *
 *   apiUrl('/uploads/abc.png')
 *   // dev:  'http://localhost:8080/api/uploads/abc.png'  (被 ResourceHandler 命中)
 *   // prod: '/api/uploads/abc.png'
 */
export function apiUrl(path) {
  if (!path) return apiBase()
  if (/^https?:\/\//i.test(path)) return path
  if (path.startsWith('/api/') || path === '/api') return apiBase() + path
  return apiBase() + '/api' + (path.startsWith('/') ? path : '/' + path)
}

/**
 * 图片/视频资源 URL 解析器(对 <img :src> / <video :src> 友好)
 *
 * 行为:
 *  - 空值/假值 -> 留空(<img> 自然走 alt 文本,不会 404)
 *  - 已是 http(s) 绝对地址 -> 原样返回(支持外链)
 *  - 后端返回的 "/uploads/xxx.png" -> "/api/uploads/xxx.png"
 *  - 已带 /api 前缀 -> 不重复补
 *  - 没有前导 / -> 补上,避免解析成"当前路径下的相对路径"
 *
 * 这是所有图片/视频字段必走的解析函数。
 */
export function resolveImg(url) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  if (/^data:/i.test(url)) return url
  if (url.startsWith('/api/') || url === '/api') return apiBase() + url
  if (url.startsWith('/')) return apiBase() + '/api' + url
  return apiBase() + '/api/' + url
}

/**
 * 处理富文本(v-html)中的图片路径。
 *
 * 旧系统导入的数据中,图片 src 可能是:
 *  - 外部域名: https://www.zgrlosta.org.cn/static/upload/image/xxx.png
 *  - 相对路径: /static/upload/image/xxx.png
 *  - 相对路径: /uploads/xxx.png
 *
 * 本函数统一将其改写为 /api/static/... 或 /api/uploads/...,
 * 由 Nginx 代理到后端静态资源处理器。
 *
 * @param {string} html - 原始 HTML
 * @returns {string} 处理后的 HTML
 */
export function processRichContent(html) {
  if (!html) return ''
  let result = html
  // 1) 外部域名的 /static/upload/xxx -> /api/static/upload/xxx
  result = result.replace(/src=["']https?:\/\/[^"']*\/(static\/upload\/[^"']+)["']/g, 'src="/api/$1"')
  // 2) 相对路径 /static/upload/xxx -> /api/static/upload/xxx
  result = result.replace(/src=["'](\/static\/upload\/[^"']+)["']/g, 'src="/api$1"')
  // 3) 相对路径 /static/其他子路径 -> /api/static/...
  result = result.replace(/src=["'](\/static\/[^"']+)["']/g, 'src="/api$1"')
  // 4) /uploads/xxx -> /api/uploads/xxx
  result = result.replace(/src=["'](\/uploads\/[^"']+)["']/g, 'src="/api$1"')
  return result
}
