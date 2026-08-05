import request from '@/utils/request'

export function getFaceConfig() {
  return request({ url: '/user/face/config', method: 'get' })
}

export function getIdPhoto() {
  return request({ url: '/user/face/id-photo', method: 'get' })
}

export function submitVerifyResult(data) {
  return request({ url: '/user/face/verify', method: 'post', data })
}

/**
 * 后端人脸比对: 前端拍摄照片上传, 后端与证件照比对
 * 不在前端加载任何模型文件
 */
export function compareFace(data) {
  return request({ url: '/user/face/compare', method: 'post', data })
}

export function getFaceStatus(examId) {
  return request({
    url: '/user/face/status',
    method: 'get',
    params: { examId }
  })
}

export function getFaceVerifyInfo(examId) {
  return request({
    url: '/user/exam/face-verify-info',
    method: 'get',
    params: { examId }
  })
}
