import request from '@/utils/request'

export function announcementPage(params) {
  return request({ url: '/admin/announcement/page', method: 'get', params })
}

export function addAnnouncement(data) {
  return request({ url: '/admin/announcement', method: 'post', data })
}

export function updateAnnouncement(data) {
  return request({ url: '/admin/announcement', method: 'put', data })
}

export function deleteAnnouncement(id) {
  return request({ url: '/admin/announcement/' + id, method: 'delete' })
}

export function batchDeleteAnnouncements(ids) {
  return request({ url: '/admin/announcement/batch', method: 'delete', data: ids })
}
