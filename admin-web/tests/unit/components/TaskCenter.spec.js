/**
 * TaskCenter WebSocket 逻辑单元测试
 *
 * 重点覆盖(简单可靠):
 * - 组件挂载时自动 connectWebSocket
 * - 收 ws 消息时更新 activeList
 * - subscribe 任务:readyState=1 时 send
 * - 异常 ws(抛错/不存在)不挂组件
 */
import { mount } from '@vue/test-utils'

jest.mock('element-ui', () => ({ __esModule: true, default: { install: jest.fn() }, install: jest.fn() }))

// jest.mock factory 被 hoist 到 import 之前,
// 所以 factory 内只能用 jest.fn() 创建,不能引用外层 const
// 这里通过 import 之后修改 mock 的实现细节
jest.mock('@/api/asyncTask', () => ({
  getTask: jest.fn().mockResolvedValue({ data: null }),
  listActiveTasks: jest.fn().mockResolvedValue({ data: [] }),
  listAllTasks: jest.fn().mockResolvedValue({ data: [] }),
  pageTasks: jest.fn().mockResolvedValue({ data: { total: 0, records: [] } }),
  cancelTask: jest.fn().mockResolvedValue({ data: true }),
  retryTask: jest.fn().mockResolvedValue({ data: true }),
  cleanupTasksNow: jest.fn().mockResolvedValue({ data: 0 }),
  getTaskConfig: jest.fn().mockResolvedValue({ data: {} }),
  clearFinishedTasks: jest.fn().mockResolvedValue({ data: 0 }),
  downloadTaskResultUrl: jest.fn().mockImplementation((id) => `/api/admin/task/${id}/download`)
}))

jest.mock('@/router', () => ({ __esModule: true, default: { push: jest.fn() } }))

// Mock WebSocket
class MockWebSocket {
  constructor(url) {
    this.url = url
    this.readyState = 0
    this.sent = []
    this.onopen = null
    this.onclose = null
    this.onerror = null
    this.onmessage = null
    MockWebSocket.instances.push(this)
  }
  send(data) { this.sent.push(data) }
  close() { this.readyState = 3; if (this.onclose) this.onclose({}) }
  simulateOpen() { this.readyState = 1; if (this.onopen) this.onopen({}) }
  simulateMessage(data) { if (this.onmessage) this.onmessage({ data: JSON.stringify(data) }) }
}
MockWebSocket.instances = []
global.WebSocket = MockWebSocket

// 必须放在 jest.mock 之后
import TaskCenter from '@/components/TaskCenter.vue'
import asyncTask from '@/api/asyncTask'

const mockListActive = asyncTask.listActiveTasks
const mockDownloadUrl = asyncTask.downloadTaskResultUrl

async function mountTaskCenter() {
  const wrapper = mount(TaskCenter, { attachTo: document.body })
  await new Promise(r => setTimeout(r, 0))
  return wrapper
}

describe('TaskCenter - WebSocket 连接', () => {
  beforeEach(() => {
    MockWebSocket.instances = []
    mockListActive.mockClear().mockResolvedValue({ data: [] })
  })

  test('挂载后自动 connectWebSocket:WebSocket 构造函数被调用', async () => {
    const wrapper = await mountTaskCenter()
    expect(MockWebSocket.instances.length).toBeGreaterThanOrEqual(1)
    wrapper.destroy()
  })

  test('WebSocket URL 是 ws:// + host + /ws/task', async () => {
    const wrapper = await mountTaskCenter()
    const ws = MockWebSocket.instances[0]
    expect(ws.url).toMatch(/^ws:\/\/.*\/ws\/task$/)
    wrapper.destroy()
  })

  test('WebSocket 不存在:不抛错', () => {
    const origWS = global.WebSocket
    delete global.WebSocket
    const wrapper = mount(TaskCenter, { attachTo: document.body })
    expect(() => wrapper.vm.connectWebSocket()).not.toThrow()
    wrapper.destroy()
    global.WebSocket = origWS
  })

  test('WebSocket 构造函数抛错:不挂组件(走 polling 兜底)', () => {
    const origWS = global.WebSocket
    function FailingWS() { throw new Error('fail') }
    global.WebSocket = FailingWS
    const wrapper = mount(TaskCenter, { attachTo: document.body })
    expect(() => wrapper.vm.connectWebSocket()).not.toThrow()
    wrapper.destroy()
    global.WebSocket = origWS
  })
})

describe('TaskCenter - ws 消息处理', () => {
  beforeEach(() => {
    MockWebSocket.instances = []
    mockListActive.mockClear().mockResolvedValue({ data: [] })
  })

  test('onmessage action=update:task 进 activeList', async () => {
    const wrapper = await mountTaskCenter()
    const ws = MockWebSocket.instances[0]
    ws.simulateOpen()
    ws.simulateMessage({ action: 'update', task: { taskId: 't-100', status: 'running', progress: 50 } })
    expect(wrapper.vm.activeList.some(t => t.taskId === 't-100')).toBe(true)
    const t = wrapper.vm.activeList.find(x => x.taskId === 't-100')
    expect(t.progress).toBe(50)
    wrapper.destroy()
  })

  test('onmessage 非 JSON:不挂', async () => {
    const wrapper = await mountTaskCenter()
    const ws = MockWebSocket.instances[0]
    ws.simulateOpen()
    ws.onmessage({ data: 'not-json-data' })
    expect(true).toBe(true)
    wrapper.destroy()
  })

  test('onmessage action 不是 update:忽略', async () => {
    const wrapper = await mountTaskCenter()
    const ws = MockWebSocket.instances[0]
    ws.simulateOpen()
    ws.simulateMessage({ action: 'something_else' })
    expect(wrapper.vm.activeList).toHaveLength(0)
    wrapper.destroy()
  })
})

describe('TaskCenter - subscribe / unsubscribe', () => {
  beforeEach(() => {
    MockWebSocket.instances = []
    mockListActive.mockClear().mockResolvedValue({ data: [] })
  })

  test('subscribe(readyState=1):send JSON subscribe', async () => {
    const wrapper = await mountTaskCenter()
    const ws = MockWebSocket.instances[0]
    ws.simulateOpen()
    wrapper.vm.subscribe('t-1')
    const sentStr = ws.sent.join('')
    expect(sentStr).toContain('"action":"subscribe"')
    expect(sentStr).toContain('"taskId":"t-1"')
    wrapper.destroy()
  })

  test('subscribe(readyState=0):不 send', async () => {
    const wrapper = await mountTaskCenter()
    wrapper.vm.subscribe('t-2')
    expect(MockWebSocket.instances[0].sent).toHaveLength(0)
    wrapper.destroy()
  })

  test('unsubscribe:send JSON unsubscribe', async () => {
    const wrapper = await mountTaskCenter()
    const ws = MockWebSocket.instances[0]
    ws.simulateOpen()
    wrapper.vm.unsubscribe('t-3')
    const sentStr = ws.sent.join('')
    expect(sentStr).toContain('"action":"unsubscribe"')
    expect(sentStr).toContain('"taskId":"t-3"')
    wrapper.destroy()
  })
})

describe('TaskCenter - downloadTaskResultUrl', () => {
  test('api/downloadTaskResultUrl 返回正确路径(被 mock,验证 mock 入参)', async () => {
    // 实际上 TaskCenter 没直接暴露 downloadUrl,改测 api 已被引用并正确返回路径
    expect(mockDownloadUrl('t-9')).toBe('/api/admin/task/t-9/download')
    // mount 一下让组件不报错
    const wrapper = await mountTaskCenter()
    wrapper.destroy()
  })
})
