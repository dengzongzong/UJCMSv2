/**
 * store/modules/admin 单元测试
 * 测试 mutation SET_TOKEN 写/读 localStorage
 * action login / fetchAdminInfo / resetToken / logout
 */
jest.mock('@/api/auth', () => ({
  login: jest.fn(),
  getAdminInfo: jest.fn()
}))

import admin from '@/store/modules/admin'
import { login as apiLogin, getAdminInfo as apiGetInfo } from '@/api/auth'

describe('store/modules/admin - mutations', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  test('SET_TOKEN 有值:写 localStorage.admin_token', () => {
    const state = {}
    admin.mutations.SET_TOKEN(state, 'mock-jwt-1')
    expect(state.token).toBe('mock-jwt-1')
    expect(localStorage.getItem('admin_token')).toBe('mock-jwt-1')
  })

  test('SET_TOKEN 空值:删 localStorage.admin_token', () => {
    localStorage.setItem('admin_token', 'old')
    const state = { token: 'old' }
    admin.mutations.SET_TOKEN(state, '')
    expect(state.token).toBe('')
    expect(localStorage.getItem('admin_token')).toBeNull()
  })

  test('SET_ADMIN_INFO:存对象', () => {
    const state = { adminInfo: null }
    admin.mutations.SET_ADMIN_INFO(state, { id: 1, username: 'admin' })
    expect(state.adminInfo).toEqual({ id: 1, username: 'admin' })
  })

  test('RESET_STATE:回到 default state', () => {
    const state = { token: 'x', adminInfo: { id: 1 } }
    admin.mutations.RESET_STATE(state)
    expect(state.token).toBe('')
    expect(state.adminInfo).toBeNull()
  })
})

describe('store/modules/admin - actions', () => {
  beforeEach(() => {
    localStorage.clear()
    apiLogin.mockReset()
    apiGetInfo.mockReset()
  })

  describe('login()', () => {
    test('成功:token + adminInfo 都写 store + localStorage', async () => {
      apiLogin.mockResolvedValue({
        data: { token: 'mock-jwt-1', userId: 100, username: 'admin', nickname: '管理员', role: 'admin' }
      })
      const state = { token: '', adminInfo: null }
      const commit = (type, payload) => admin.mutations[type](state, payload)
      await admin.actions.login({ commit }, { username: 'admin', password: 'pwd' })
      // state 已经被真实 mutation 改写
      expect(state.token).toBe('mock-jwt-1')
      expect(localStorage.getItem('admin_token')).toBe('mock-jwt-1')
      expect(state.adminInfo).toMatchObject({ userId: 100, username: 'admin', role: 'admin' })
    })

    test('成功但无 token(失败场景):不应写 adminInfo', async () => {
      apiLogin.mockResolvedValue({ data: { /* no token */ } })
      const state = { token: 'old', adminInfo: { id: 99 } }
      const commit = (type, payload) => admin.mutations[type](state, payload)
      await admin.actions.login({ commit }, { username: 'admin', password: 'wrong' })
      // token 被置空(失败)
      expect(state.token).toBe('')
      // adminInfo 不变(没 SET_ADMIN_INFO 调用)
      expect(state.adminInfo).toEqual({ id: 99 })
    })

    test('api 失败:reject(不写 store)', async () => {
      apiLogin.mockRejectedValue(new Error('401'))
      const state = { token: 'old', adminInfo: null }
      const commit = (type, payload) => admin.mutations[type](state, payload)
      await expect(admin.actions.login({ commit }, { username: 'admin', password: 'wrong' }))
        .rejects.toThrow('401')
      // state 不变
      expect(state.token).toBe('old')
    })
  })

  describe('fetchAdminInfo()', () => {
    test('成功:写 adminInfo', async () => {
      apiGetInfo.mockResolvedValue({ data: { id: 1, username: 'admin', role: 'admin' } })
      const state = { adminInfo: null }
      const commit = (type, payload) => admin.mutations[type](state, payload)
      const info = await admin.actions.fetchAdminInfo({ commit })
      expect(info).toEqual({ id: 1, username: 'admin', role: 'admin' })
      expect(state.adminInfo).toEqual({ id: 1, username: 'admin', role: 'admin' })
    })

    test('失败:reject', async () => {
      apiGetInfo.mockRejectedValue(new Error('network'))
      const commit = jest.fn()
      await expect(admin.actions.fetchAdminInfo({ commit }))
        .rejects.toThrow('network')
    })
  })

  describe('resetToken() / logout()', () => {
    test('resetToken:清 token + adminInfo + RESET_STATE', async () => {
      localStorage.setItem('admin_token', 'old')
      const state = { token: 'old', adminInfo: { id: 1 } }
      const commit = (type, payload) => admin.mutations[type](state, payload)
      await admin.actions.resetToken({ commit })
      expect(state.token).toBe('')
      expect(state.adminInfo).toBeNull()
      expect(localStorage.getItem('admin_token')).toBeNull()
    })

    test('logout:同 resetToken', async () => {
      const state = { token: 'old', adminInfo: { id: 1 } }
      const commit = (type, payload) => admin.mutations[type](state, payload)
      await admin.actions.logout({ commit })
      expect(state.token).toBe('')
      expect(state.adminInfo).toBeNull()
    })
  })
})
