import { login, getAdminInfo } from '@/api/auth'

const getDefaultState = () => ({
  token: localStorage.getItem('admin_token') || '',
  adminInfo: JSON.parse(sessionStorage.getItem('admin_info') || 'null')
})

const state = getDefaultState()

// 防止多个路由守卫同时触发 fetchAdminInfo（竞态锁）
let fetchAdminInfoPromise = null

const mutations = {
  RESET_STATE: (state) => {
    Object.assign(state, getDefaultState())
  },
  SET_TOKEN: (state, token) => {
    state.token = token
    if (token) {
      localStorage.setItem('admin_token', token)
    } else {
      localStorage.removeItem('admin_token')
    }
  },
  SET_ADMIN_INFO: (state, info) => {
    state.adminInfo = info
    // 持久化到 sessionStorage，页面刷新后不需要重新请求
    if (info) {
      sessionStorage.setItem('admin_info', JSON.stringify(info))
    } else {
      sessionStorage.removeItem('admin_info')
    }
  }
}

const actions = {
  login({ commit }, payload) {
    return new Promise((resolve, reject) => {
      login(payload)
        .then((res) => {
          const data = res.data || {}
          const token = data.token || ''
          commit('SET_TOKEN', token)
          if (token) {
            const info = {
              userId: data.userId,
              username: data.username,
              nickname: data.nickname,
              avatar: data.avatar,
              role: data.role,
              permissions: data.permissions || [],
              isSuper: data.isSuper
            }
            commit('SET_ADMIN_INFO', info)
          }
          resolve(res)
        })
        .catch((err) => {
          reject(err)
        })
    })
  },
  fetchAdminInfo({ commit }) {
    // 竞态锁：如果已在请求中，复用同一个 Promise，避免并发重复请求
    if (fetchAdminInfoPromise) return fetchAdminInfoPromise
    fetchAdminInfoPromise = new Promise((resolve, reject) => {
      getAdminInfo()
        .then((res) => {
          const info = res.data || null
          commit('SET_ADMIN_INFO', info)
          resolve(info)
        })
        .catch((err) => {
          reject(err)
        })
        .finally(() => {
          fetchAdminInfoPromise = null
        })
    })
    return fetchAdminInfoPromise
  },
  resetToken({ commit }) {
    return new Promise((resolve) => {
      commit('SET_TOKEN', '')
      commit('SET_ADMIN_INFO', null)
      commit('RESET_STATE')
      resolve()
    })
  },
  logout({ commit }) {
    return new Promise((resolve) => {
      commit('SET_TOKEN', '')
      commit('SET_ADMIN_INFO', null)
      commit('RESET_STATE')
      resolve()
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
