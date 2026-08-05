const getters = {
  token: (state) => state.admin.token,
  adminInfo: (state) => state.admin.adminInfo,
  permissions: (state) => (state.admin.adminInfo && state.admin.adminInfo.permissions) || [],
  isSuper: (state) => (state.admin.adminInfo && state.admin.adminInfo.isSuper) || 0,
  sidebarCollapsed: (state) => state.app.sidebarCollapsed,
  certTypes: (state) => state.app.certTypes
}

export default getters
