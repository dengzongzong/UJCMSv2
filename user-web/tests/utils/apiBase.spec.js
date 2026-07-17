/**
 * apiBase 单元测试
 *
 * 重点回归:
 *  1) apiBase() 返回的 base 与 VUE_APP_BASE_API 一致
 *  2) apiUrl(path):
 *     - 空值 -> 返 base
 *     - http(s) 绝对地址 -> 原样返
 *     - /api/ 开头 -> 不重复补
 *     - 其它 /xxx -> 补 /api
 *     - 非前导 / 路径 -> 补 /api/
 *  3) resolveImg(url):
 *     - 空 -> ''
 *     - http(s) -> 原样
 *     - /api/xxx -> 原样(base + path)
 *     - /uploads/xxx -> /api/uploads/xxx
 *     - uploads/xxx (无前导 /) -> /api/uploads/xxx
 */
import { apiBase, apiUrl, resolveImg } from '@/utils/apiBase'

describe('utils/apiBase', () => {
  // 关键: 每个 test 都要隔离 process.env,避免测试间状态污染
  const ORIGINAL_BASE = process.env.VUE_APP_BASE_API
  afterEach(() => {
    if (ORIGINAL_BASE === undefined) {
      delete process.env.VUE_APP_BASE_API
    } else {
      process.env.VUE_APP_BASE_API = ORIGINAL_BASE
    }
  })

  describe('apiBase()', () => {
    test('未设置 VUE_APP_BASE_API 时返空串(相对路径)', () => {
      delete process.env.VUE_APP_BASE_API
      expect(apiBase()).toBe('')
    })
    test('显式返空串时返空串', () => {
      process.env.VUE_APP_BASE_API = ''
      expect(apiBase()).toBe('')
    })
    test('字符串 "undefined" "null" 当空处理', () => {
      process.env.VUE_APP_BASE_API = 'undefined'
      expect(apiBase()).toBe('')
      process.env.VUE_APP_BASE_API = 'null'
      expect(apiBase()).toBe('')
    })
    test('设值时原样返', () => {
      process.env.VUE_APP_BASE_API = 'http://localhost:8080'
      expect(apiBase()).toBe('http://localhost:8080')
    })
  })

  describe('apiUrl(path)', () => {
    beforeEach(() => {
      // 默认 base 为空
      delete process.env.VUE_APP_BASE_API
    })
    test('空 path -> 返 base', () => {
      expect(apiUrl('')).toBe(apiBase())
    })
    test('http 绝对地址原样返', () => {
      expect(apiUrl('http://example.com/x.png')).toBe('http://example.com/x.png')
      expect(apiUrl('https://example.com/x.png')).toBe('https://example.com/x.png')
    })
    test('/api/ 开头不再补 /api', () => {
      expect(apiUrl('/api/uploads/abc.png')).toBe('/api/uploads/abc.png')
    })
    test('根 /api 不再补', () => {
      expect(apiUrl('/api')).toBe('/api')
    })
    test('以 / 开头的非 /api 路径补 /api', () => {
      expect(apiUrl('/uploads/abc.png')).toBe('/api/uploads/abc.png')
    })
    test('无前导 / 的路径补 /api/', () => {
      expect(apiUrl('uploads/abc.png')).toBe('/api/uploads/abc.png')
    })
  })

  describe('resolveImg(url) — 图片/视频 URL 解析', () => {
    beforeEach(() => {
      delete process.env.VUE_APP_BASE_API
    })
    test('空 url -> 空串(不 404)', () => {
      expect(resolveImg('')).toBe('')
      expect(resolveImg(null)).toBe('')
      expect(resolveImg(undefined)).toBe('')
    })
    test('http 绝对地址原样', () => {
      expect(resolveImg('https://cdn.example.com/a.png')).toBe('https://cdn.example.com/a.png')
    })
    test('/api/ 路径不再补', () => {
      expect(resolveImg('/api/uploads/abc.png')).toBe('/api/uploads/abc.png')
    })
    test('后端返回的 /uploads/xxx -> /api/uploads/xxx', () => {
      expect(resolveImg('/uploads/abc.png')).toBe('/api/uploads/abc.png')
    })
    test('无前导 / 的 uploads/xxx -> /api/uploads/xxx', () => {
      expect(resolveImg('uploads/abc.png')).toBe('/api/uploads/abc.png')
    })
    test('带 base 时正确拼接', () => {
      process.env.VUE_APP_BASE_API = 'http://localhost:8080'
      expect(resolveImg('/uploads/abc.png')).toBe('http://localhost:8080/api/uploads/abc.png')
    })
  })
})
