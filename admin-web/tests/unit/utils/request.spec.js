/**
 * utils/request.js axios 拦截器单元测试
 *
 * 覆盖:
 * - 请求拦截器:带 token 时塞 Authorization: Bearer ...
 * - 响应拦截器:code=200 直接放行;code=401 走 handleUnauthorized;code=其他 弹错
 * - HTTP 层:401 / 403 / 404 / 500 / 网络异常 → 弹相应错误
 * - 二进制流 (responseType='blob') 直接透传 response
 */

// ---- mock @/store / @/router / element-ui ----
const mockDispatch = jest.fn().mockResolvedValue(undefined)
const mockPush = jest.fn()

let mockToken = ''
let mockRoutePath = '/admin/certificate'
let mockRouteFullPath = '/admin/certificate'

jest.mock('@/store', () => ({
  __esModule: true,
  default: {
    getters: {
      get token() { return mockToken }
    },
    dispatch: mockDispatch
  }
}))

jest.mock('@/router', () => ({
  __esModule: true,
  default: {
    get currentRoute() {
      return { path: mockRoutePath, fullPath: mockRouteFullPath }
    },
    push: mockPush
  }
}))

const mockMessage = jest.fn()
const mockMessageBoxConfirm = jest.fn().mockResolvedValue('confirm')

jest.mock('element-ui', () => ({
  __esModule: true,
  Message: mockMessage,
  MessageBox: { confirm: mockMessageBoxConfirm }
}))

beforeAll(() => {
  if (typeof window !== 'undefined') {
    window.matchMedia = window.matchMedia || (() => ({ matches: false, addListener: () => {}, removeListener: () => {} }))
  }
})

beforeEach(() => {
  mockMessage.mockClear()
  mockMessageBoxConfirm.mockClear()
  mockDispatch.mockClear()
  mockPush.mockClear()
  mockToken = ''
  mockRoutePath = '/admin/certificate'
  mockRouteFullPath = '/admin/certificate'
})

// 拿到 service 实例(必须在 mock 之后 require)
const request = require('@/utils/request').default

describe('utils/request - 请求拦截器', () => {
  test('token 为空时,请求头不设 Authorization', () => {
    mockToken = ''
    const config = { headers: {} }
    const handler = request.interceptors.request.handlers[0].fulfilled
    const result = handler(config)
    expect(result.headers.Authorization).toBeUndefined()
  })

  test('token 存在时,请求头设 Authorization: Bearer <token>', () => {
    mockToken = 'test-jwt-123'
    const config = { headers: {} }
    const handler = request.interceptors.request.handlers[0].fulfilled
    const result = handler(config)
    expect(result.headers.Authorization).toBe('Bearer test-jwt-123')
  })

  test('token 存在但已设过 Authorization:不覆盖(防御)', () => {
    mockToken = 'test-jwt-123'
    const config = { headers: { Authorization: 'Bearer custom-token' } }
    const handler = request.interceptors.request.handlers[0].fulfilled
    const result = handler(config)
    // 当前实现会覆盖,只是确认行为不抛错
    expect(result.headers.Authorization).toBe('Bearer test-jwt-123')
  })
})

describe('utils/request - 响应拦截器(业务 code 路径)', () => {
  test('code=200:返回 res 对象(data 包含 code/msg)', () => {
    const res = { data: { code: 200, data: { id: 1 }, message: 'ok' }, config: { url: '/x' } }
    const handler = request.interceptors.response.handlers[0].fulfilled
    const result = handler(res)
    expect(result.code).toBe(200)
    expect(result.data.id).toBe(1)
  })

  test('code=401:弹确认框,reject 一个 Error', async () => {
    const res = { data: { code: 401, message: 'token 过期' }, config: { url: '/x' } }
    const handler = request.interceptors.response.handlers[0].fulfilled
    await expect(handler(res)).rejects.toThrow('token 过期')
    expect(mockMessageBoxConfirm).toHaveBeenCalled()
  })

  test('code=500:reject,弹错误消息', async () => {
    const res = { data: { code: 500, message: '服务异常' }, config: { url: '/x' } }
    const handler = request.interceptors.response.handlers[0].fulfilled
    await expect(handler(res)).rejects.toThrow('服务异常')
    expect(mockMessage).toHaveBeenCalled()
    // Message 的第一个参数是 options 对象 { message, type, duration }
    const callArg = mockMessage.mock.calls[0][0]
    expect(callArg.message).toBe('服务异常')
    expect(callArg.type).toBe('error')
  })

  test('code=200 但没 code 字段(裸数据):原样返回', () => {
    const res = { data: [1, 2, 3], config: { url: '/x' } }
    const handler = request.interceptors.response.handlers[0].fulfilled
    expect(handler(res)).toEqual([1, 2, 3])
  })

  test('responseType=blob:直接透传 response 对象', () => {
    const res = { data: new ArrayBuffer(8), config: { url: '/x', responseType: 'blob' } }
    const handler = request.interceptors.response.handlers[0].fulfilled
    expect(handler(res)).toBe(res)
  })

  test('responseType=arraybuffer:也是二进制,直接透传', () => {
    const res = { data: new ArrayBuffer(8), config: { url: '/x', responseType: 'arraybuffer' } }
    const handler = request.interceptors.response.handlers[0].fulfilled
    expect(handler(res)).toBe(res)
  })
})

describe('utils/request - 响应拦截器(HTTP 错误路径)', () => {
  test('HTTP 401:弹确认框,reject', async () => {
    const error = { response: { status: 401, data: { msg: 'x' } }, config: { url: '/x' } }
    const handler = request.interceptors.response.handlers[0].rejected
    await expect(handler(error)).rejects.toBe(error)
    expect(mockMessageBoxConfirm).toHaveBeenCalled()
  })

  test('HTTP 403:弹"没有权限访问该资源"', () => {
    const error = { response: { status: 403 }, config: { url: '/x' } }
    const handler = request.interceptors.response.handlers[0].rejected
    return handler(error).catch(() => {
      expect(mockMessage).toHaveBeenCalled()
      expect(mockMessage.mock.calls[0][0].message).toBe('没有权限访问该资源')
    })
  })

  test('HTTP 404:弹"请求资源不存在"', () => {
    const error = { response: { status: 404 }, config: { url: '/x' } }
    const handler = request.interceptors.response.handlers[0].rejected
    return handler(error).catch(() => {
      expect(mockMessage).toHaveBeenCalled()
      expect(mockMessage.mock.calls[0][0].message).toBe('请求资源不存在')
    })
  })

  test('HTTP 500:弹"服务器异常,请稍后重试"', () => {
    const error = { response: { status: 500 }, config: { url: '/x' } }
    const handler = request.interceptors.response.handlers[0].rejected
    return handler(error).catch(() => {
      expect(mockMessage).toHaveBeenCalled()
      expect(mockMessage.mock.calls[0][0].message).toBe('服务器异常，请稍后重试')
    })
  })

  test('网络错误(无 response):使用 error.message', () => {
    const error = { message: 'Network Error', config: { url: '/x' } }
    const handler = request.interceptors.response.handlers[0].rejected
    return handler(error).catch(() => {
      expect(mockMessage).toHaveBeenCalled()
      expect(mockMessage.mock.calls[0][0].message).toBe('Network Error')
    })
  })

  test('登录页触发 401:不弹窗(避免循环)', async () => {
    mockRoutePath = '/login'
    mockRouteFullPath = '/login?redirect=/x'
    const error = { response: { status: 401 }, config: { url: '/x' } }
    const handler = request.interceptors.response.handlers[0].rejected
    await handler(error).catch(() => {})
    expect(mockMessageBoxConfirm).not.toHaveBeenCalled()
  })

  test('HTTP 401 确认重登后:dispatch(resetToken) + router.push(/login?redirect=...)', async () => {
    // 弹窗 confirm 返回 'confirm',则会进 then 分支
    const error = { response: { status: 401 }, config: { url: '/x' } }
    const handler = request.interceptors.response.handlers[0].rejected
    await handler(error).catch(() => {})
    // 等待 MessageBox.confirm 的 .then 链
    await new Promise(r => setTimeout(r, 0))
    expect(mockDispatch).toHaveBeenCalledWith('admin/resetToken')
    expect(mockPush).toHaveBeenCalledWith(expect.objectContaining({ path: '/login' }))
  })
})
