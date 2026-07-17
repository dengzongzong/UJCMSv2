/**
 * api/certificate 单元测试
 * - 各函数调用 request 时 url/method/data 正确
 * - downloadFile: token 注入、URL 拼接、blob + filename 解析
 * - triggerDownload:触发浏览器下载(用 mock 的 a.click 验证)
 */
jest.mock('@/utils/request', () => {
  const mock = jest.fn().mockResolvedValue({ data: { id: 1 } })
  return { __esModule: true, default: mock }
})

import request from '@/utils/request'
import {
  certificatePage, certificateDetail, addCertificate, updateCertificate,
  deleteCertificate, importCertificate, commitImport, switchExamQr,
  generateCertificateBatch, downloadFile, triggerDownload,
  photoPage, addPhoto, deletePhoto
} from '@/api/certificate'

describe('api/certificate - 路由拼装', () => {
  beforeEach(() => request.mockClear())

  test('certificatePage(params):get /admin/certificate/page,params 透传', () => {
    certificatePage({ page: 1, size: 10, name: '张三' })
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/certificate/page',
      method: 'get',
      params: { page: 1, size: 10, name: '张三' }
    }))
  })

  test('certificateDetail(id):get /admin/certificate/<id>', () => {
    certificateDetail(123)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/certificate/123', method: 'get'
    }))
  })

  test('addCertificate:post /admin/certificate, data 透传', () => {
    addCertificate({ name: '张三', idCard: '110101199001011235' })
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/certificate', method: 'post', data: { name: '张三', idCard: '110101199001011235' }
    }))
  })

  test('updateCertificate:put', () => {
    updateCertificate({ id: 1, name: '改' })
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/certificate', method: 'put'
    }))
  })

  test('deleteCertificate:delete, data 是 id 数组', () => {
    deleteCertificate([1, 2, 3])
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/certificate', method: 'delete', data: [1, 2, 3]
    }))
  })

  test('importCertificate:post multipart/form-data, data 是 file', () => {
    const file = new FormData()
    importCertificate(file)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/certificate/import', method: 'post',
      data: file,
      headers: { 'Content-Type': 'multipart/form-data' }
    }))
  })

  test('commitImport:post /import/commit', () => {
    commitImport({ taskId: 't1' })
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/certificate/import/commit', method: 'post'
    }))
  })

  test('switchExamQr:post /exam-qr/switch', () => {
    switchExamQr({ id: 1, enabled: true })
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/certificate/exam-qr/switch', method: 'post'
    }))
  })

  test('generateCertificateBatch:post /generate/batch', () => {
    generateCertificateBatch({ ids: [1, 2] })
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/certificate/generate/batch', method: 'post'
    }))
  })

  test('photoPage: /photo/page', () => {
    photoPage({ page: 1, size: 10 })
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/certificate/photo/page', method: 'get'
    }))
  })

  test('addPhoto/deletePhoto', () => {
    addPhoto({ url: '/x.png' })
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/certificate/photo', method: 'post'
    }))
    deletePhoto([1])
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/admin/certificate/photo', method: 'delete', data: [1]
    }))
  })
})

describe('api/certificate - downloadFile()', () => {
  let originalFetch

  beforeEach(() => {
    originalFetch = global.fetch
    request.mockClear()
    // 清 vuex / localStorage
    localStorage.clear()
    sessionStorage.clear()
  })

  afterEach(() => {
    global.fetch = originalFetch
  })

  function mockFetchResponse({ ok = true, status = 200, blob = new Blob(['x']), cd = 'attachment; filename="cert.xlsx"' } = {}) {
    return jest.fn().mockResolvedValue({
      ok, status,
      headers: { get: (k) => k.toLowerCase() === 'content-disposition' ? cd : null },
      blob: () => Promise.resolve(blob)
    })
  }

  test('url 以 /uploads 开头,base="" → fetch 直接打到 /api/uploads/...(关键回归)', async () => {
    // 关键: 后端 servlet context-path = /api,前端原生 fetch 必须走 /api 前缀
    // 否则 fetch 命中的是前端 dev server 的 8081 端口 -> 404
    delete process.env.VUE_APP_BASE_API
    global.fetch = mockFetchResponse()
    await downloadFile('/uploads/template.xlsx')
    expect(global.fetch).toHaveBeenCalledTimes(1)
    const url = global.fetch.mock.calls[0][0]
    expect(url).toBe('/api/uploads/template.xlsx')
    // 默认 method get
    expect(global.fetch.mock.calls[0][1].method).toBe('get')
  })

  test('证书模板下载 /admin/certificate/template 走 /api 前缀', async () => {
    // 关键回归: 用户报 404 的接口
    delete process.env.VUE_APP_BASE_API
    global.fetch = mockFetchResponse()
    await downloadFile('/admin/certificate/template')
    const url = global.fetch.mock.calls[0][0]
    expect(url).toBe('/api/admin/certificate/template')
  })

  test('证书下载 /admin/certificate/generate/single/<id> 走 /api 前缀', async () => {
    // 关键回归: 用户报 404 的接口
    delete process.env.VUE_APP_BASE_API
    global.fetch = mockFetchResponse()
    await downloadFile('/admin/certificate/generate/single/123?format=image')
    const url = global.fetch.mock.calls[0][0]
    expect(url).toBe('/api/admin/certificate/generate/single/123?format=image')
  })

  test('带 vuex token 时:Authorization: Bearer <token>', async () => {
    delete process.env.VUE_APP_BASE_API
    sessionStorage.setItem('vuex', JSON.stringify({ token: 'mock-jwt-1' }))
    global.fetch = mockFetchResponse()
    await downloadFile('/uploads/template.xlsx')
    const headers = global.fetch.mock.calls[0][1].headers
    expect(headers.Authorization).toBe('Bearer mock-jwt-1')
  })

  test('带 vuex 但 token 在 nested auth 下也能取到', async () => {
    sessionStorage.setItem('vuex', JSON.stringify({ auth: { token: 'nested-jwt' } }))
    global.fetch = mockFetchResponse()
    await downloadFile('/uploads/template.xlsx')
    expect(global.fetch.mock.calls[0][1].headers.Authorization).toBe('Bearer nested-jwt')
  })

  test('401 应 throw "HTTP 401 (未登录)"', async () => {
    global.fetch = mockFetchResponse({ ok: false, status: 401 })
    await expect(downloadFile('/uploads/x')).rejects.toThrow(/HTTP 401.*未登录/)
  })

  test('解析 filename*=utf-8\'\'xxx (RFC 5987)', async () => {
    global.fetch = mockFetchResponse({ cd: "attachment; filename*=utf-8''%E8%AF%81%E4%B9%A6.xlsx" })
    const r = await downloadFile('/uploads/x')
    expect(r.fileName).toBe('证书.xlsx')
  })

  test('解析 filename="xxx" 简单形式', async () => {
    global.fetch = mockFetchResponse({ cd: 'attachment; filename="cert.xlsx"' })
    const r = await downloadFile('/uploads/x')
    expect(r.fileName).toBe('cert.xlsx')
  })

  test('无 Content-Disposition:fileName=""', async () => {
    global.fetch = mockFetchResponse({ cd: '' })
    const r = await downloadFile('/uploads/x')
    expect(r.fileName).toBe('')
  })

  test('vuex 格式坏 JSON:不抛错,继续(无 Authorization)', async () => {
    sessionStorage.setItem('vuex', '{not valid json')
    global.fetch = mockFetchResponse()
    await expect(downloadFile('/uploads/x')).resolves.toBeDefined()
    expect(global.fetch.mock.calls[0][1].headers.Authorization).toBeUndefined()
  })
})

describe('api/certificate - triggerDownload()', () => {
  let origCreate, origRevoke

  beforeAll(() => {
    origCreate = URL.createObjectURL
    origRevoke = URL.revokeObjectURL
    URL.createObjectURL = jest.fn(() => 'blob:mock-url')
    URL.revokeObjectURL = jest.fn()
  })

  afterAll(() => {
    URL.createObjectURL = origCreate
    URL.revokeObjectURL = origRevoke
  })

  test('点 a.click 设置 a.download + 移除元素', async () => {
    const blob = new Blob(['hello'])
    const aMock = {
      click: jest.fn(),
      set href(v) { this._href = v },
      get href() { return this._href },
      set download(v) { this._download = v },
      get download() { return this._download }
    }
    const createElSpy = jest.spyOn(document, 'createElement').mockReturnValue(aMock)
    const appendSpy = jest.spyOn(document.body, 'appendChild').mockImplementation(() => {})
    const removeSpy = jest.spyOn(document.body, 'removeChild').mockImplementation(() => {})

    triggerDownload(blob, 'cert.xlsx')

    expect(URL.createObjectURL).toHaveBeenCalledWith(blob)
    expect(aMock.click).toHaveBeenCalled()
    expect(aMock.download).toBe('cert.xlsx')
    expect(aMock.href).toBe('blob:mock-url')
    expect(appendSpy).toHaveBeenCalled()
    expect(removeSpy).toHaveBeenCalled()
    // 100ms 后会 revoke(用真实 setTimeout 异步验证)
    await new Promise(r => setTimeout(r, 150))
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url')

    createElSpy.mockRestore()
    appendSpy.mockRestore()
    removeSpy.mockRestore()
  })

  test('不传 fileName:默认 "download"', () => {
    const aMock = {
      click: jest.fn(),
      set href(v) { this._href = v },
      get href() { return this._href },
      set download(v) { this._download = v },
      get download() { return this._download }
    }
    const createElSpy = jest.spyOn(document, 'createElement').mockReturnValue(aMock)
    jest.spyOn(document.body, 'appendChild').mockImplementation(() => {})
    jest.spyOn(document.body, 'removeChild').mockImplementation(() => {})

    triggerDownload(new Blob(['x']))
    expect(aMock.download).toBe('download')
    createElSpy.mockRestore()
  })
})
