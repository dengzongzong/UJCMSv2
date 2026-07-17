import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
    currentSubject: JSON.parse(localStorage.getItem('currentSubject') || 'null')
  },
  getters: {
    token: state => state.token,
    userInfo: state => state.userInfo,
    isLoggedIn: state => !!state.token,
    currentSubject: state => state.currentSubject,
    // 以下 getter 保持兼容，currentSubject 现存储专业信息
    subjectId: state => state.currentSubject ? (state.currentSubject.professionId || state.currentSubject.id) : null,
    subjectName: state => state.currentSubject ? (state.currentSubject.professionName || state.currentSubject.name) : '',
    professionId: state => state.currentSubject ? (state.currentSubject.professionId || state.currentSubject.id) : null,
    professionName: state => state.currentSubject ? (state.currentSubject.professionName || state.currentSubject.name) : ''
  },
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      if (token) {
        localStorage.setItem('token', token)
      } else {
        localStorage.removeItem('token')
      }
    },
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },
    SET_CURRENT_SUBJECT(state, subject) {
      state.currentSubject = subject
      if (subject) {
        localStorage.setItem('currentSubject', JSON.stringify(subject))
      } else {
        localStorage.removeItem('currentSubject')
      }
    },
    CLEAR_ALL(state) {
      state.token = ''
      state.userInfo = {}
      state.currentSubject = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('currentSubject')
    }
  },
  actions: {
    setToken({ commit }, token) {
      commit('SET_TOKEN', token)
    },
    setUserInfo({ commit }, userInfo) {
      commit('SET_USER_INFO', userInfo)
    },
    setCurrentSubject({ commit }, subject) {
      commit('SET_CURRENT_SUBJECT', subject)
    },
    login({ commit }, { token, userInfo }) {
      commit('SET_TOKEN', token)
      commit('SET_USER_INFO', userInfo || {})
    },
    logout({ commit }) {
      commit('CLEAR_ALL')
    }
  },
  modules: {}
})
