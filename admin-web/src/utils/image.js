/**
 * 图片压缩工具(温和压缩,尽量不损失清晰度)
 * - 仅处理 JPEG(其他格式如 PNG/GIF/WEBP 原样返回,避免破坏透明通道或动图)
 * - 体积小于 minBytes 的图片不处理
 * - 长边超过 maxEdge 时等比缩小,否则只重新编码
 * - 压缩后体积反而变大时回退原图
 * 用法: const out = await compressImage(file)
 */
export function compressImage(file, options = {}) {
  const { maxEdge = 1600, quality = 0.85, minBytes = 300 * 1024 } = options
  return new Promise(resolve => {
    const isJpeg =
      (file && (file.type === 'image/jpeg' || file.type === 'image/jpg')) ||
      (file && /\.(jpe?g)$/i.test(file.name || ''))
    if (!file || !isJpeg || file.size <= minBytes || typeof document === 'undefined') {
      resolve(file)
      return
    }
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      try {
        const scale = Math.min(1, maxEdge / Math.max(img.width, img.height))
        const width = Math.max(1, Math.round(img.width * scale))
        const height = Math.max(1, Math.round(img.height * scale))
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        // JPEG 不支持透明,先铺白底,避免透明区域变黑
        ctx.fillStyle = '#ffffff'
        ctx.fillRect(0, 0, width, height)
        ctx.drawImage(img, 0, 0, width, height)
        canvas.toBlob(
          blob => {
            if (!blob || blob.size >= file.size) {
              resolve(file)
              return
            }
            const name = (file.name || 'photo').replace(/\.(jpe?g)$/i, '') + '.jpg'
            if (typeof File === 'function') {
              resolve(new File([blob], name, { type: 'image/jpeg', lastModified: Date.now() }))
            } else {
              blob.name = name
              resolve(blob)
            }
          },
          'image/jpeg',
          quality
        )
      } catch (e) {
        resolve(file)
      }
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      resolve(file)
    }
    img.src = url
  })
}

export default compressImage
