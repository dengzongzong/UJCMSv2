import axios from 'axios'
import { Message, MessageBox } from 'element-ui'
import store from '@/store'
import router from '@/router'

// 与后端约定:成功响应固定 code = 200,业务异常其它 code,401 = 登录过期
const SUCCESS_CODE = 200
const UNAUTHORIZED_CODE = 401

const service = axios.create({
  baseURL: '/api',
  timeout: 30000,
  withCredentials: false
})

service.interceptors.request.use(
  (config) => {
    const token = store.getters.token
    if (token) {
      // 与后端统一:仅使用 Authorization: Bearer <token>,不再额外塞 Admin-Token
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

function buildLoginRedirect() {
  return {
    path: '/login',
    query: { redirect: router.currentRoute.fullPath }
  }
}

function showError(message) {
  try {
    Message({ message: message || '请求异常', type: 'error', duration: 3000 })
  } catch (e) {
    // Element Message 组件没装上时,降级到 console
    // eslint-disable-next-line no-console
    console.error('[request] showError 失败,降级 console.error', message, e)
  }
}

function handleUnauthorized() {
  // 避免重复弹窗:只在非登录页时弹
  if (router.currentRoute.path === '/login') return
  MessageBox.confirm('登录状态已过期，您可以继续留在该页面，或者重新登录', '系统提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      store.dispatch('admin/resetToken').then(() => {
        router.push(buildLoginRedirect())
      })
    })
    .catch(() => {})
}

service.interceptors.response.use(
  (response) => {
    // 二进制流直接透传
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response
    }
    const res = response.data
    // 兼容后端两种风格:统一信封 { code, data, message } 或裸数据
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === SUCCESS_CODE) {
        return res
      }
      if (res.code === UNAUTHORIZED_CODE) {
        handleUnauthorized()
        return Promise.reject(new Error(res.message || '未授权'))
      }
      // 记录完整错误对象,便于排错
      // eslint-disable-next-line no-console
      console.error('[request] biz error', response.config.url, res)
      showError(res.message)
      return Promise.reject(new Error(res.message || '请求异常'))
    }
    return res
  },
  (error) => {
    const status = error.response && error.response.status
    const data = error.response && error.response.data
    // 记录网络/服务错误详情
    // eslint-disable-next-line no-console
    console.error('[request] http error', error.config && error.config.url, status, data || error.message)
    if (status === 401) {
      handleUnauthorized()
    } else if (status === 403) {
      showError('没有权限访问该资源')
    } else if (status === 404) {
      showError('请求资源不存在')
    } else if (status >= 500) {
      // 优先取后端返回的具体错误消息(可能是 Result{message} 或 Spring 默认 {error/message})
      const msg = (data && data.message) || (data && data.error) || '服务器异常，请稍后重试'
      showError(msg)
    } else if (status === 0) {
      showError('无法连接服务器，请检查网络或后端是否启动')
    } else {
      showError((data && data.message) || error.message || '网络异常')
    }
    // 返回一个干净的 rejected promise,避免原始 axios error 对象触发 dev-server 运行时遮罩
    const cleanError = new Error(
      (data && data.message) || (data && data.error) || error.message || '网络异常'
    )
    cleanError.status = status
    return Promise.reject(cleanError)
  }
)

export default service
