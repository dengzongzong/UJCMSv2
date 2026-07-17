import request from '@/utils/request'

export function dashboardStats() {
  return request({
    url: '/admin/dashboard/stats',
    method: 'get'
  })
}

export function dashboardAnnouncements() {
  return request({
    url: '/admin/announcement/page',
    method: 'get',
    params: { page: 1, size: 5, status: 1 }
  })
}
