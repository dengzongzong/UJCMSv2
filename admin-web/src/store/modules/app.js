const state = {
  sidebarCollapsed: localStorage.getItem('sidebar_collapsed') === 'true'
}

const mutations = {
  TOGGLE_SIDEBAR: (state) => {
    state.sidebarCollapsed = !state.sidebarCollapsed
    localStorage.setItem('sidebar_collapsed', state.sidebarCollapsed)
  }
}

const actions = {
  toggleSidebar({ commit }) {
    commit('TOGGLE_SIDEBAR')
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
