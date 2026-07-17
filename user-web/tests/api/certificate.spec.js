/**
 * api/certificate.js 单元测试
 *
 * 重点回归:
 *  1) searchMyCertificates: 调用 /portal/certificate/search, POST JSON
 *  2) buildCertificateDownloadUrl: URL 上带 idCard + name + format(防越权)
 *  3) downloadCertificate:
 *     - 自动从 localStorage.token 注入 Authorization
 *     - 后端 4xx 时尝试解析 JSON message
 *     - 解析 Content-Disposition 的 filename*=
 *     - 兜底文件名 姓名_身份证后6位.png
 *  4) saveBlob: 创建 <a download> 并触发点击
 */
import {
  searchMyCertificates,
  buildCertificateDownloadUrl,
  downloadCertificate,
  saveBlob
} from '@/api/certificate'

// mock @/utils/request, 只验证调用方传的参数是否正确
jest.mock('@/utils/request', () => ({
  __esModule: true,
  default: jest.fn()
}))

import request from '@/utils/request'

describe('api/certificate', () => {
  beforeEach(() => {
    jest.clearAllMocks()
    localStorage.clear()
  })

  describe('searchMyCertificates()', () => {
    test('调用 /portal/certificate/search, POST JSON', () => {
      request.mockResolvedValue({ data: { total: 0, records: [] } })
      searchMyCertificates({ idCard: '110101199001011234', name: '张三' })
      expect(request).toHaveBeenCalledWith({
        url: '/portal/certificate/search',
        method: 'post',
        data: { idCard: '110101199001011234', name: '张三' }
      })
    })

    test('certNo 为空字符串时仍传, 由后端忽略', () => {
      request.mockResolvedValue({ data: { total: 0, records: [] } })
      searchMyCertificates({ idCard: 'x', name: 'y', certNo: '' })
      const call = request.mock.calls[0][0]
      // 实际行为: certNo 字段保留空串原样传给后端,后端忽略空字符串
      expect(call.data.certNo).toBe('')
    })
  })

  describe('buildCertificateDownloadUrl()', () => {
    test('带 idCard + name + format=image', () => {
      const url = buildCertificateDownloadUrl(
        { id: 100, name: '张三', idCard: '110101199001011234' },
        'image'
      )
      expect(url).toContain('/portal/certificate/download/100?')
      expect(url).toContain('idCard=110101199001011234')
      expect(url).toContain('name=' + encodeURIComponent('张三'))
      expect(url).toContain('format=image')
    })

    test('format=pdf 走 pdf', () => {
      const url = buildCertificateDownloadUrl(
        { id: 5, name: '李四', idCard: 'X' },
        'pdf'
      )
      expect(url).toContain('format=pdf')
    })

    test('非法 format 默认为 image', () => {
      const url = buildCertificateDownloadUrl(
        { id: 5, name: '李四', idCard: 'X' },
        'whatever'
      )
      expect(url).toContain('format=image')
    })
  })

  describe('downloadCertificate()', () => {
    let originalFetch
    beforeEach(() => {
      originalFetch = global.fetch
    })
    afterEach(() => {
      global.fetch = originalFetch
    })

    function mockFetchResponse({ ok = true, status = 200, ct = 'image/png', cd = '', blob = new Blob(['x']) } = {}) {
      return jest.fn().mockResolvedValue({
        ok,
        status,
        headers: {
          get: (name) => {
            if (name === 'Content-Disposition') return cd
            if (name === 'Content-Type') return ct
            return null
          }
        },
        blob: () => Promise.resolve(blob),
        json: () => Promise.resolve({ code: 200, message: 'success', data: null })
      })
    }

    test('从 localStorage.token 注入 Bearer 头', async () => {
      localStorage.setItem('token', 'STUDENT-JWT-XXX')
      global.fetch = mockFetchResponse()
      await downloadCertificate({ id: 1, name: '张三', idCard: 'X' }, 'image')
      const [url, init] = global.fetch.mock.calls[0]
      expect(url).toContain('/portal/certificate/download/1')
      expect(init.headers['Authorization']).toBe('Bearer STUDENT-JWT-XXX')
    })

    test('无 token 时不发 Authorization 头', async () => {
      global.fetch = mockFetchResponse()
      await downloadCertificate({ id: 1, name: '张三', idCard: 'X' }, 'image')
      const [, init] = global.fetch.mock.calls[0]
      expect(init.headers['Authorization']).toBeUndefined()
    })

    test('后端 403 时抛错并尝试解析 message', async () => {
      global.fetch = jest.fn().mockResolvedValue({
        ok: false,
        status: 403,
        headers: { get: () => null },
        json: () => Promise.resolve({ code: 403, message: '该证书不属于当前账号,无法下载' }),
        blob: () => Promise.resolve(new Blob())
      })
      await expect(downloadCertificate({ id: 1, name: 'x', idCard: 'X' }, 'image'))
        .rejects.toThrow(/该证书不属于当前账号/)
    })

    test('解析 Content-Disposition 的 filename*=utf-8\'\'', async () => {
      const cd = "attachment;filename*=utf-8''%E5%BC%A0%E4%B8%89_123456.png"
      global.fetch = mockFetchResponse({ cd })
      const r = await downloadCertificate({ id: 1, name: '张三', idCard: 'X' }, 'image')
      expect(r.fileName).toBe('张三_123456.png')
    })

    test('解析 Content-Disposition 的 filename= 后备', async () => {
      const cd = 'attachment;filename="李四_999.pdf"'
      global.fetch = mockFetchResponse({ cd })
      const r = await downloadCertificate({ id: 1, name: '李四', idCard: 'X' }, 'pdf')
      expect(r.fileName).toBe('李四_999.pdf')
    })

    test('无 Content-Disposition 时, 兜底 姓名_身份证后6位.png', async () => {
      global.fetch = mockFetchResponse({ cd: '' })
      const r = await downloadCertificate({ id: 1, name: '王五', idCard: '1234567890ABCDEF' }, 'image')
      // 身份证后 6 位 = 'ABCDEF'
      expect(r.fileName).toBe('王五_ABCDEF.png')
    })

    test('无 Content-Disposition 且无 idCard 时, 兜底 姓名_<id>.pdf', async () => {
      global.fetch = mockFetchResponse({ cd: '' })
      const r = await downloadCertificate({ id: 999, name: '钱七', idCard: null }, 'pdf')
      expect(r.fileName).toBe('钱七_999.pdf')
    })

    test('返回 blob 给调用方', async () => {
      const blob = new Blob(['pngbytes'])
      global.fetch = mockFetchResponse({ blob })
      const r = await downloadCertificate({ id: 1, name: 'a', idCard: 'X' }, 'image')
      expect(r.blob).toBe(blob)
    })
  })

  describe('saveBlob()', () => {
    test('创建 <a> 元素, 设置 download, 模拟点击', () => {
      const blob = new Blob(['x'])
      // 在 jsdom 里 window.URL.createObjectURL 默认为空实现
      const originalCreate = window.URL.createObjectURL
      const originalRevoke = window.URL.revokeObjectURL
      window.URL.createObjectURL = jest.fn().mockReturnValue('blob:fake')
      window.URL.revokeObjectURL = jest.fn().mockImplementation(() => {})

      // 用一个 beforeunload 监听器存住 <a> 元素引用(因为 saveBlob 内部已经 removeChild 了)
      let captured = null
      const origAppend = document.body.appendChild.bind(document.body)
      document.body.appendChild = function (node) {
        const r = origAppend(node)
        if (node && node.tagName === 'A' && node.download) captured = node
        return r
      }
      const clickSpy = jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})

      saveBlob(blob, 'test.png')

      expect(window.URL.createObjectURL).toHaveBeenCalledWith(blob)
      expect(captured).not.toBeNull()
      expect(captured.download).toBe('test.png')
      expect(captured.href).toBe('blob:fake')
      expect(clickSpy).toHaveBeenCalledTimes(1)

      // 还原
      window.URL.createObjectURL = originalCreate
      window.URL.revokeObjectURL = originalRevoke
      document.body.appendChild = origAppend
    })
  })
})
