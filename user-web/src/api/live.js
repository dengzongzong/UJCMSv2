import request from '@/utils/request'

/**
 * 直播大厅列表(公开)
 */
export function getLiveList() {
  return request({ url: '/user/live/public/list', method: 'get' })
}

/**
 * 直播间详情(公开, 已登录自动带 token 判断是否开通该直播)
 * 已开通该直播才返回播放地址
 */
export function getLiveDetail(id) {
  return request({ url: '/user/live/public/' + id, method: 'get' })
}

/**
 * 进入直播间(校验直播开通 + 累计观看人次)
 */
export function enterLive(id) {
  return request({ url: '/user/live/' + id + '/enter', method: 'post' })
}

/**
 * 拉取最近聊天记录(公开, HTTP 兜底)
 */
export function getLiveMessages(id, limit) {
  return request({
    url: '/user/live/public/' + id + '/messages',
    method: 'get',
    params: { limit }
  })
}

/**
 * 发送聊天消息(HTTP 兜底, 主要走 WebSocket)
 */
export function sendLiveMessage(id, content) {
  return request({ url: '/user/live/' + id + '/message', method: 'post', data: { content } })
}
