import request from '@/utils/request'
import { apiUrl } from '@/utils/apiBase'

// 异步任务查询
export function getTask(taskId) {
  return request({ url: '/admin/task/' + taskId, method: 'get' })
}
export function listActiveTasks() {
  return request({ url: '/admin/task/active', method: 'get' })
}
export function listAllTasks() {
  return request({ url: '/admin/task/list', method: 'get' })
}
export function pageTasks(params) {
  // params: { page, size, bizType, status }
  return request({ url: '/admin/task/page', method: 'get', params })
}
export function cancelTask(taskId) {
  return request({ url: '/admin/task/' + taskId + '/cancel', method: 'post' })
}
export function retryTask(taskId) {
  return request({ url: '/admin/task/' + taskId + '/retry', method: 'post' })
}
export function cleanupTasksNow() {
  return request({ url: '/admin/task/cleanup', method: 'post' })
}
export function getTaskConfig() {
  return request({ url: '/admin/task/config', method: 'get' })
}
export function clearFinishedTasks() {
  return request({ url: '/admin/task/finished', method: 'delete' })
}
export function downloadTaskResultUrl(taskId) {
  return apiUrl('/admin/task/' + taskId + '/download')
}
