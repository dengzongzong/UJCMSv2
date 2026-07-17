const getters = {
  token: (state) => state.admin.token,
  adminInfo: (state) => state.admin.adminInfo,
  sidebarCollapsed: (state) => state.app.sidebarCollapsed
}

export default getters
