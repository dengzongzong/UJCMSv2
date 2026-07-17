import request from '@/utils/request'

/**
 * 获取个人信息
 */
export function getProfile() {
  return request({
    url: '/user/profile/info',
    method: 'get'
  })
}

/**
 * 更新个人信息
 */
export function updateProfile(data) {
  return request({
    url: '/user/profile/update',
    method: 'put',
    data
  })
}

/**
 * 上传文件
 * @param {FormData} formData
 */
export function uploadFile(formData) {
  return request({
    url: '/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取关于我们
 */
export function getAboutUs() {
  return request({
    url: '/public/about',
    method: 'get'
  })
}
