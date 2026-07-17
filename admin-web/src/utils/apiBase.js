/**
 * 获取后端 API base URL。
 *
 * 约定:
 * - 后端 Spring Boot 的 servlet context-path = /api(见 backend/src/main/resources/application.yml)
 * - 所有业务接口实际路径是 /api/<business-path>
 * - axios 的 baseURL = '/api',所以 axios 直接发 /api/<x> 即可
 * - 原生 fetch (downloadFile / 图片 / 资源直链) 必须自己拼 base,所以走 apiUrl()
 *
 * 优先级:
 * 1) process.env.VUE_APP_BASE_API
 *    - 开发期: 'http://localhost:8080' (绝对地址,后端 8080)
 *    - 生产期: '' (同源,由 nginx 反代 /api 到后端)
 * 2) 兜底:空字符串 (相对路径,同源)
 *
 * 关键点: 任何业务路径都必须带 /api 前缀,所以 apiUrl(path) 会强制补 /api;
 * 但如果 path 已经以 /api 开头就不再补(避免 /api/api 这种)
 */
export function apiBase() {
  let base = process.env.VUE_APP_BASE_API
  if (!base || base === 'undefined' || base === 'null') {
    // 默认使用相对路径,后端部署在同一域或用 dev server 代理即可
    return ''
  }
  return base
}

/**
 * 拼接完整 URL,自动补 /api 前缀。
 *
 * @param {string} path - 业务路径,必须以 '/' 开头,如 '/admin/certificate/page'
 * @returns {string} 完整 URL
 *
 * @example
 *   apiUrl('/admin/certificate/page')
 *   // dev:  'http://localhost:8080/api/admin/certificate/page'
 *   // prod: '/api/admin/certificate/page'
 *
 *   apiUrl('/uploads/abc.png')
 *   // dev:  'http://localhost:8080/api/uploads/abc.png'  (被 ResourceHandler 命中,放行 JWT)
 *   // prod: '/api/uploads/abc.png'
 */
export function apiUrl(path) {
  if (!path) return apiBase()
  // 已是绝对地址,原样返回
  if (/^https?:\/\//i.test(path)) return path
  // 已有 /api 前缀,不再补
  if (path.startsWith('/api/') || path === '/api') return apiBase() + path
  // 自动补 /api
  return apiBase() + '/api' + (path.startsWith('/') ? path : '/' + path)
}
