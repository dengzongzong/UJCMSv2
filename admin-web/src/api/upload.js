import request from '@/utils/request'

export function uploadFile(data, onProgress) {
  return request({
    url: '/file/upload',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (e) => {
      if (onProgress) {
        const percent = Math.round((e.loaded * 100) / e.total)
        onProgress(percent)
      }
    }
  })
}

// 扫描未被业务数据引用的孤儿文件(图片/视频)
export function scanOrphanFiles() {
  return request({ url: '/admin/upload/orphans', method: 'get' })
}

// 删除指定的孤儿文件
export function cleanOrphanFiles(files) {
  return request({ url: '/admin/upload/clean', method: 'post', data: { files } })
}

// 清空证书预览缓存
export function cleanPreviewCache() {
  return request({ url: '/admin/upload/clean-preview-cache', method: 'post' })
}
