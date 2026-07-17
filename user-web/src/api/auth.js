import request from '@/utils/request'

/**
 * 用户登录
 * @param {Object} data - { phone, password, role }
 */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 用户注册
 * @param {Object} data - { phone, captchaKey, captchaCode, password, confirmPassword, agreement }
 */
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/**
 * 重置密码(未登录态, 走图形验证码, 走 login 页)
 * @param {Object} data - { phone, captchaKey, captchaCode, newPassword }
 */
export function resetPassword(data) {
  return request({
    url: '/auth/reset-password',
    method: 'post',
    data
  })
}

/**
 * 登录态修改密码(已登录态, 需要原密码, 走"我的"页)
 * @param {Object} data - { oldPassword, newPassword }
 */
export function changePassword(data) {
  return request({
    url: '/auth/change-password',
    method: 'post',
    data
  })
}

/**
 * 获取图形验证码
 * <p>返回 { captchaKey, imageBase64, expireMillis }</p>
 */
export function getCaptcha() {
  return request({
    url: '/public/captcha/generate',
    method: 'get'
  })
}

/**
 * 选择专业
 * @param {Object} data - { professionId }
 */
export function chooseSubject(data) {
  return request({
    url: '/auth/choose-subject',
    method: 'post',
    data
  })
}

/**
 * 获取专业科目列表
 */
export function getSubjectList() {
  return request({
    url: '/public/professions',
    method: 'get'
  })
}
