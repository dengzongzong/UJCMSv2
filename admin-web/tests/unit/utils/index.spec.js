/**
 * utils/index 工具函数测试
 * - formatFileSize
 * - formatDuration
 */
import { formatFileSize, formatDuration } from '@/utils'

describe('utils/index', () => {
  describe('formatFileSize()', () => {
    test('0 / null / 负数 → "0 B"', () => {
      expect(formatFileSize(0)).toBe('0 B')
      expect(formatFileSize(null)).toBe('0 B')
      expect(formatFileSize(undefined)).toBe('0 B')
      expect(formatFileSize(-1)).toBe('0 B')
    })

    test('B 级(1-1023)', () => {
      expect(formatFileSize(512)).toBe('512.00 B')
    })

    test('KB 级', () => {
      expect(formatFileSize(1024)).toBe('1.00 KB')
      expect(formatFileSize(1536)).toBe('1.50 KB')
    })

    test('MB 级', () => {
      expect(formatFileSize(1024 * 1024)).toBe('1.00 MB')
      expect(formatFileSize(5 * 1024 * 1024)).toBe('5.00 MB')
    })

    test('GB 级', () => {
      expect(formatFileSize(1024 * 1024 * 1024)).toBe('1.00 GB')
    })

    test('TB 级', () => {
      expect(formatFileSize(1024 * 1024 * 1024 * 1024)).toBe('1.00 TB')
    })
  })

  describe('formatDuration()', () => {
    test('0 / null / 负数 → "00:00"', () => {
      expect(formatDuration(0)).toBe('00:00')
      expect(formatDuration(null)).toBe('00:00')
      expect(formatDuration(undefined)).toBe('00:00')
      expect(formatDuration(-5)).toBe('00:00')
    })

    test('< 1 分钟:00:SS', () => {
      expect(formatDuration(5)).toBe('00:05')
      expect(formatDuration(59)).toBe('00:59')
    })

    test('>= 1 分钟:MM:SS', () => {
      expect(formatDuration(60)).toBe('01:00')
      expect(formatDuration(125)).toBe('02:05')   // 2 分 5 秒
      expect(formatDuration(3599)).toBe('59:59')
    })

    test('>= 1 小时:HH:MM:SS', () => {
      expect(formatDuration(3600)).toBe('01:00:00')
      expect(formatDuration(3661)).toBe('01:01:01')
      expect(formatDuration(36000)).toBe('10:00:00')
    })

    test('不足 10 的分/秒自动补 0', () => {
      expect(formatDuration(65)).toBe('01:05')
      expect(formatDuration(3700)).toBe('01:01:40')
    })
  })
})
