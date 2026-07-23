const getters = {
  token: (state) => state.admin.token,
  adminInfo: (state) => state.admin.adminInfo,
  sidebarCollapsed: (state) => state.app.sidebarCollapsed,
  certTypes: (state) => state.app.certTypes
}

export default getters
