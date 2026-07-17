/**
 * api/asyncTask 单元测试
 * - 各函数 URL 拼装正确
 * - downloadTaskResultUrl:apiUrl 拼接,且 undefined base 兜底
 */
jest.mock('@/utils/request', () => {
  const mock = jest.fn().mockResolvedValue({ data: { ok: true } })
  return { __esModule: true, default: mock }
})
import request from '@/utils/request'
import {
  getTask, listActiveTasks, listAllTasks, pageTasks,
  cancelTask, retryTask, cleanupTasksNow, getTaskConfig,
  clearFinishedTasks, downloadTaskResultUrl
} from '@/api/asyncTask'

describe('api/asyncTask - 路由', () => {
  beforeEach(() => request.mockClear())

  test('getTask: /admin/task/<id>', () => {
    getTask('task-001')
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/task/task-001', method: 'get'
    }))
  })

  test('listActiveTasks / listAllTasks / getTaskConfig', () => {
    listActiveTasks()
    expect(request).toHaveBeenCalledWith(expect.objectContaining({ url: '/admin/task/active', method: 'get' }))
    listAllTasks()
    expect(request).toHaveBeenCalledWith(expect.objectContaining({ url: '/admin/task/list', method: 'get' }))
    getTaskConfig()
    expect(request).toHaveBeenCalledWith(expect.objectContaining({ url: '/admin/task/config', method: 'get' }))
  })

  test('pageTasks:params 透传', () => {
    pageTasks({ page: 1, size: 20, status: 'pending' })
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/task/page', method: 'get', params: { page: 1, size: 20, status: 'pending' }
    }))
  })

  test('cancelTask / retryTask', () => {
    cancelTask('t-1')
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/task/t-1/cancel', method: 'post'
    }))
    retryTask('t-1')
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/task/t-1/retry', method: 'post'
    }))
  })

  test('cleanupTasksNow / clearFinishedTasks', () => {
    cleanupTasksNow()
    expect(request).toHaveBeenCalledWith(expect.objectContaining({ url: '/admin/task/cleanup', method: 'post' }))
    clearFinishedTasks()
    expect(request).toHaveBeenCalledWith(expect.objectContaining({ url: '/admin/task/finished', method: 'delete' }))
  })
})

describe('api/asyncTask - downloadTaskResultUrl()', () => {
  // 关键:downloadTaskResultUrl 走原生 fetch,必须经 apiUrl() 补 /api 前缀
  // 否则后端 context-path=/api 收不到请求 -> 404
  test('base 未设置(undefined):返回 /api/admin/task/<id>/download', () => {
    delete process.env.VUE_APP_BASE_API
    expect(downloadTaskResultUrl('t-1')).toBe('/api/admin/task/t-1/download')
  })

  test('base 正常:正确拼接 base + /api + path', () => {
    process.env.VUE_APP_BASE_API = 'http://localhost:8080'
    expect(downloadTaskResultUrl('t-2')).toBe('http://localhost:8080/api/admin/task/t-2/download')
  })

  test('"undefined" 字面字符串:兜底为相对路径,自动补 /api(回归测试)', () => {
    process.env.VUE_APP_BASE_API = 'undefined'
    const url = downloadTaskResultUrl('t-3')
    expect(url).toBe('/api/admin/task/t-3/download')
    expect(url).not.toMatch(/undefined/)
  })
})
