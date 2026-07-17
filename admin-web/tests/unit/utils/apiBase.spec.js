/**
 * utils/apiBase 单元测试
 *
 * 重点回归:
 * 1) process.env.VUE_APP_BASE_API = 'undefined'(字面字符串)时,不应拼出 "undefined/xxx"
 * 2) 同上 'null' 也是
 * 3) undefined / null / '' 都 fallback 到空字符串
 * 4) 正常 base URL 正确拼接
 * 5) 路径以 / 开头不带,也不会重复 /
 * 6) **强制补 /api 前缀**(关键): 后端 servlet context-path = /api,
 *    前端原生 fetch 走 apiUrl() 必须把 /api 前缀补上,否则会 404
 * 7) path 已带 /api 时不再重复补
 * 8) 绝对 URL(http/https)原样返回
 */
import { apiBase, apiUrl } from '@/utils/apiBase'

describe('utils/apiBase', () => {
  const ORIGINAL_ENV = process.env.VUE_APP_BASE_API

  afterEach(() => {
    // 恢复
    if (ORIGINAL_ENV === undefined) {
      delete process.env.VUE_APP_BASE_API
    } else {
      process.env.VUE_APP_BASE_API = ORIGINAL_ENV
    }
  })

  describe('apiBase()', () => {
    test('process.env 未设置 -> 返回空串(走相对路径)', () => {
      delete process.env.VUE_APP_BASE_API
      expect(apiBase()).toBe('')
    })

    test('process.env 为 undefined 字面字符串 -> 兜底返回空串(关键回归)', () => {
      process.env.VUE_APP_BASE_API = 'undefined'
      expect(apiBase()).toBe('')
    })

    test('process.env 为 null 字面字符串 -> 兜底返回空串', () => {
      process.env.VUE_APP_BASE_API = 'null'
      expect(apiBase()).toBe('')
    })

    test('process.env 为空串 -> 返回空串', () => {
      process.env.VUE_APP_BASE_API = ''
      expect(apiBase()).toBe('')
    })

    test('process.env 为 null(JS null) -> 返回空串', () => {
      process.env.VUE_APP_BASE_API = null
      expect(apiBase()).toBe('')
    })

    test('正常 base URL 应原样返回', () => {
      process.env.VUE_APP_BASE_API = 'http://localhost:8080'
      expect(apiBase()).toBe('http://localhost:8080')
    })

    test('base URL 末尾带 / 也应原样返回(不主动 trim)', () => {
      process.env.VUE_APP_BASE_API = 'http://api.example.com/'
      expect(apiBase()).toBe('http://api.example.com/')
    })
  })

  describe('apiUrl()', () => {
    test('base="" 时,apiUrl(/x) 必须补 /api 前缀 -> /api/x', () => {
      delete process.env.VUE_APP_BASE_API
      expect(apiUrl('/admin/certificate')).toBe('/api/admin/certificate')
    })

    test('base 存在时,apiUrl 拼接 base + /api + path', () => {
      process.env.VUE_APP_BASE_API = 'http://localhost:8080'
      expect(apiUrl('/admin/certificate')).toBe('http://localhost:8080/api/admin/certificate')
    })

    test('"undefined" base + 路径 -> 不应出现 "undefined/"', () => {
      process.env.VUE_APP_BASE_API = 'undefined'
      const url = apiUrl('/admin/certificate/template')
      expect(url).toBe('/api/admin/certificate/template')
      expect(url).not.toMatch(/undefined/)
    })

    test('/uploads/xxx.png 资源路径自动补 /api', () => {
      // 关键回归: 模板背景图、学员照片都走这个
      delete process.env.VUE_APP_BASE_API
      expect(apiUrl('/uploads/abc.png')).toBe('/api/uploads/abc.png')
      process.env.VUE_APP_BASE_API = 'http://localhost:8080'
      expect(apiUrl('/uploads/abc.png')).toBe('http://localhost:8080/api/uploads/abc.png')
    })

    test('path 已带 /api 前缀时,不再重复补', () => {
      delete process.env.VUE_APP_BASE_API
      expect(apiUrl('/api/admin/certificate')).toBe('/api/admin/certificate')
      process.env.VUE_APP_BASE_API = 'http://localhost:8080'
      expect(apiUrl('/api/admin/certificate')).toBe('http://localhost:8080/api/admin/certificate')
    })

    test('绝对 URL(http/https)原样返回,不补 /api', () => {
      delete process.env.VUE_APP_BASE_API
      expect(apiUrl('https://cdn.example.com/x.png')).toBe('https://cdn.example.com/x.png')
    })

    test('空 path 返回 base', () => {
      process.env.VUE_APP_BASE_API = 'http://localhost:8080'
      expect(apiUrl('')).toBe('http://localhost:8080')
    })

    test('证书下载接口 /admin/certificate/template 在 dev 环境下正确拼接', () => {
      // 关键回归: 用户报 404 的接口
      process.env.VUE_APP_BASE_API = 'http://localhost:8080'
      expect(apiUrl('/admin/certificate/template')).toBe('http://localhost:8080/api/admin/certificate/template')
    })

    test('证书下载接口 /admin/certificate/generate/single/{id} 在 dev 环境下正确拼接', () => {
      // 关键回归: 用户报 404 的接口
      process.env.VUE_APP_BASE_API = 'http://localhost:8080'
      expect(apiUrl('/admin/certificate/generate/single/123'))
        .toBe('http://localhost:8080/api/admin/certificate/generate/single/123')
    })
  })
})
