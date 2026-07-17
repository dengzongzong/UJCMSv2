import { Message } from 'element-ui'

export function downloadBlob(response, defaultName) {
  let fileName = defaultName || 'download'
  const disposition = response.headers && response.headers['content-disposition']
  if (disposition) {
    const match = disposition.match(/filename\*?=(?:UTF-8'')?([^;]+)/i)
    if (match && match[1]) {
      try {
        fileName = decodeURIComponent(match[1].replace(/["']/g, ''))
      } catch (e) {
        fileName = match[1].replace(/["']/g, '')
      }
    }
  }
  const data = response.data
  const blob = data instanceof Blob ? data : new Blob([data])
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export function formatFileSize(bytes) {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(2) + ' ' + units[i]
}

export function formatDuration(seconds) {
  if (!seconds || seconds <= 0) return '00:00'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  if (h > 0) {
    return `${pad(h)}:${pad(m)}:${pad(s)}`
  }
  return `${pad(m)}:${pad(s)}`
}

function pad(n) {
  return n < 10 ? '0' + n : '' + n
}

export function showMessage(message, type = 'success') {
  Message({ message, type, duration: 2500 })
}
