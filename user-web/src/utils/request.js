import axios from 'axios'
import { Toast } from 'vant'
import store from '@/store'
import router from '@/router'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 自动携带 token
    const token = store.getters.token
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data

    // 处理文件流
    if (response.config.responseType === 'blob') {
      return response
    }

    // 业务状态码判断
    if (res.code !== undefined && res.code !== 0 && res.code !== 200) {
      // 构造带业务 code 的错误(供调用方判断 1001/1002 等)
      const businessError = new Error(res.message || '请求失败')
      businessError.code = res.code
      // 401 未授权
      if (res.code === 401) {
        handleUnauthorized(response.config)
        return Promise.reject(businessError)
      }
      // 静默请求（如断点续考保存答案）失败时不弹 Toast，避免打扰用户答题
      if (!response.config.silent) {
        Toast.fail(res.message || '请求失败')
      }
      return Promise.reject(businessError)
    }

    return res
  },
  error => {
    const { response } = error
    const silent = error.config && error.config.silent
    if (response) {
      // 优先取后端返回的具体错误消息(Result{message} 或 Spring 默认 {error/message})
      const data = response.data
      const msg = (data && data.message) || (data && data.error)
      switch (response.status) {
        case 401:
          handleUnauthorized(error.config)
          break
        case 403:
          if (!silent) Toast.fail(msg || '没有权限访问')
          break
        case 404:
          if (!silent) Toast.fail(msg || '请求资源不存在')
          break
        case 500:
          if (!silent) Toast.fail(msg || '服务器内部错误')
          break
        default:
          if (!silent) Toast.fail(msg || '网络请求失败')
      }
    } else {
      if (!silent) Toast.fail('网络连接异常，请检查网络')
    }
    // 返回干净的 rejected promise,避免原始 axios error 触发 dev-server 运行时遮罩
    const cleanError = new Error(
      (response && response.data && response.data.message) ||
      (response && response.data && response.data.error) ||
      error.message || '网络异常'
    )
    cleanError.status = response && response.status
    // 同时携带业务 code,方便调用方判断(如 401/1001 等)
    cleanError.code = (response && response.data && response.data.code) || (response && response.status)
    return Promise.reject(cleanError)
  }
)

// 处理 401 未授权
function handleUnauthorized(config) {
  // 如果已经在登录页，不再重复跳转，避免重定向参数嵌套累积
  if (router.currentRoute.name === 'Login') {
    return
  }
  // 静默请求(如 checkCourseAccess)不弹 Toast 也不自动跳转,
  // 由调用方自行处理(如弹出 Dialog 让用户选择是否登录)
  if (config && config.silent) {
    store.dispatch('logout')
    return
  }
  Toast.fail('登录已过期，请重新登录')
  store.dispatch('logout')
  setTimeout(() => {
    router.push({
      name: 'Login',
      query: { redirect: router.currentRoute.fullPath }
    })
  }, 1000)
}

export default service
