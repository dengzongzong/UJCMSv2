import request from '@/utils/request'

export function livePage(params) {
  return request({ url: '/admin/live/page', method: 'get', params })
}

export function liveDetail(id) {
  return request({ url: '/admin/live/' + id, method: 'get' })
}

export function addLive(data) {
  return request({ url: '/admin/live', method: 'post', data })
}

export function updateLive(data) {
  return request({ url: '/admin/live', method: 'put', data })
}

export function deleteLive(id) {
  return request({ url: '/admin/live/' + id, method: 'delete' })
}

export function startLive(id) {
  return request({ url: '/admin/live/' + id + '/start', method: 'post' })
}

export function stopLive(id) {
  return request({ url: '/admin/live/' + id + '/stop', method: 'post' })
}

export function setLiveReplay(id, replayUrl) {
  return request({ url: '/admin/live/' + id + '/replay', method: 'post', data: { replayUrl } })
}
