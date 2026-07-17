/**
 * views/certificateTemplate/List.vue 组件测试
 *
 * 覆盖:
 * - mount 时调 load() → templateList API
 * - 表格显示
 * - 删除按钮 → 调 deleteTemplate → 重新 load
 * - 设为默认 → 调 setDefaultTemplate → 重新 load
 * - resolveUrl:http 直通;/uploads/... 走 apiUrl(无 token)
 */
import { mount } from '@vue/test-utils'

jest.mock('element-ui', () => ({ __esModule: true, default: { install: jest.fn() }, install: jest.fn() }))

const mockTemplateList = jest.fn()
const mockDelete = jest.fn()
const mockSetDefault = jest.fn()

jest.mock('@/api/certificateTemplate', () => ({
  templateList: (...a) => mockTemplateList(...a),
  deleteTemplate: (...a) => mockDelete(...a),
  setDefaultTemplate: (...a) => mockSetDefault(...a)
}))

jest.mock('@/router', () => ({
  __esModule: true,
  default: { push: jest.fn() }
}))

import CertificateTemplateList from '@/views/certificateTemplate/List.vue'

async function mountList({ list = [] } = {}) {
  mockTemplateList.mockResolvedValue({ data: list })
  mockDelete.mockResolvedValue({ data: true })
  mockSetDefault.mockResolvedValue({ data: true })

  const wrapper = mount(CertificateTemplateList, {
    attachTo: document.body,
    stubs: {
      'el-card': true, 'el-button': true, 'el-tag': true, 'el-image': true,
      'el-table': true, 'el-table-column': true
    },
    mocks: {
      // 模拟 ElementUI 的 $confirm 和 $message
      $confirm: jest.fn().mockResolvedValue('confirm'),
      $message: { success: jest.fn(), error: jest.fn(), info: jest.fn(), warning: jest.fn() }
    }
  })
  await new Promise(r => setTimeout(r, 0))
  await new Promise(r => setTimeout(r, 0))
  return wrapper
}

describe('CertificateTemplateList - 加载 + 显示', () => {
  beforeEach(() => {
    mockTemplateList.mockClear(); mockDelete.mockClear(); mockSetDefault.mockClear()
  })

  test('mount 时自动调 templateList', async () => {
    await mountList()
    expect(mockTemplateList).toHaveBeenCalled()
  })

  test('数据赋值到 vm.list', async () => {
    const wrapper = await mountList({
      list: [{ id: 1, name: 'tpl-A', bgWidth: 1920, bgHeight: 1080, isDefault: 0 }]
    })
    expect(wrapper.vm.list).toHaveLength(1)
    expect(wrapper.vm.list[0].name).toBe('tpl-A')
  })

  test('空数据:list 是空数组', async () => {
    const wrapper = await mountList({ list: [] })
    expect(wrapper.vm.list).toEqual([])
  })

  test('loading 状态:load 完变 false', async () => {
    const wrapper = await mountList()
    expect(wrapper.vm.loading).toBe(false)
  })
})

describe('CertificateTemplateList - 操作按钮', () => {
  beforeEach(() => {
    mockTemplateList.mockClear(); mockDelete.mockClear(); mockSetDefault.mockClear()
  })

  test('onDelete:调 deleteTemplate,删除后重新 load', async () => {
    const wrapper = await mountList({ list: [{ id: 100, name: 'X', isDefault: 0 }] })
    mockTemplateList.mockClear()
    wrapper.vm.onDelete(100)
    // deleteTemplate 是 then 链 + 内部又有 setTimeout;等 50ms
    await new Promise(r => setTimeout(r, 50))
    expect(mockDelete).toHaveBeenCalledWith(100)
    expect(mockTemplateList).toHaveBeenCalled()
  })

  test('onSetDefault:调 setDefaultTemplate + 重新 load', async () => {
    const wrapper = await mountList({ list: [{ id: 200, name: 'Y', isDefault: 0 }] })
    mockTemplateList.mockClear()
    wrapper.vm.onSetDefault(200)
    await new Promise(r => setTimeout(r, 50))
    expect(mockSetDefault).toHaveBeenCalledWith(200)
    expect(mockTemplateList).toHaveBeenCalled()
  })
})

describe('CertificateTemplateList - resolveUrl', () => {
  let wrapper

  beforeAll(async () => {
    wrapper = await mountList()
  })

  test('http://... 直通,不加 base', () => {
    expect(wrapper.vm.resolveUrl('http://cdn.example.com/x.png'))
      .toBe('http://cdn.example.com/x.png')
  })

  test('https://... 直通', () => {
    expect(wrapper.vm.resolveUrl('https://cdn.example.com/y.png'))
      .toBe('https://cdn.example.com/y.png')
  })

  test('/uploads/... 走 apiUrl(无 base 时自动补 /api,关键回归)', () => {
    // 关键: 后端 context-path = /api,前端图片必须走 /api/uploads 才能命中
    delete process.env.VUE_APP_BASE_API
    expect(wrapper.vm.resolveUrl('/uploads/bg/test.png'))
      .toBe('/api/uploads/bg/test.png')
  })

  test('空字符串返回空', () => {
    expect(wrapper.vm.resolveUrl('')).toBe('')
  })

  test('null/undefined 返回空', () => {
    expect(wrapper.vm.resolveUrl(null)).toBe('')
    expect(wrapper.vm.resolveUrl(undefined)).toBe('')
  })
})
