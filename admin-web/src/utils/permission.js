import store from '@/store'

// 判断当前管理员是否拥有指定权限
// 超级管理员(isSuper === 1)拥有全部权限
export function hasPermission(perm) {
  const adminInfo = store.getters.adminInfo
  if (!adminInfo) return false
  if (adminInfo.isSuper === 1) return true
  const permissions = adminInfo.permissions || []
  return permissions.includes(perm)
}

// 判断当前管理员是否拥有数据删除权限
export function canDelete() {
  return hasPermission('delete')
}
