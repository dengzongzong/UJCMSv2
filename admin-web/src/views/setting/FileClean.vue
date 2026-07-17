<template>
  <div class="file-clean-page">
    <el-card>
      <div slot="header" class="header">
        <span>文件清理</span>
        <div class="actions">
          <el-button type="primary" size="small" :loading="scanning" @click="handleScan">扫描孤儿文件</el-button>
          <el-button type="warning" size="small" :loading="cleaningCache" @click="handleCleanCache">清空证书预览缓存</el-button>
        </div>
      </div>

      <el-alert
        v-if="scanResult"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      >
        <template slot="title">
          扫描完成：上传目录共 {{ scanResult.total }} 个文件，其中
          <b>{{ scanResult.orphanCount }}</b> 个未被任何业务数据引用（孤儿文件）。
        </template>
      </el-alert>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
        title="说明：孤儿文件是指上传后未被任何考试/课程/视频/证书/题目等业务数据引用的图片或视频。删除前会再次校验引用情况，仍被引用的文件会自动跳过，请放心使用。"
      />

      <div v-if="selection.length" style="margin-bottom: 12px">
        <el-button type="danger" size="small" :loading="cleaning" @click="handleCleanSelected">
          删除选中（{{ selection.length }}）
        </el-button>
        <el-button type="danger" plain size="small" :loading="cleaning" @click="handleCleanAll">
          删除全部孤儿（{{ scanResult.orphanCount }}）
        </el-button>
      </div>

      <el-table
        v-loading="scanning"
        :data="orphanList"
        border
        stripe
        @selection-change="onSelectionChange"
        empty-text="暂无数据，请先点击「扫描孤儿文件」"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column label="文件名" prop="filename" min-width="220" show-overflow-tooltip />
        <el-table-column label="大小" width="120">
          <template slot-scope="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column label="上传时间" width="180">
          <template slot-scope="{ row }">{{ formatTime(row.lastModified) }}</template>
        </el-table-column>
        <el-table-column label="路径" prop="path" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { scanOrphanFiles, cleanOrphanFiles, cleanPreviewCache } from '@/api/upload'

export default {
  name: 'FileClean',
  data() {
    return {
      scanning: false,
      cleaning: false,
      cleaningCache: false,
      scanResult: null,
      selection: []
    }
  },
  computed: {
    orphanList() {
      return this.scanResult ? this.scanResult.orphans : []
    }
  },
  methods: {
    handleScan() {
      this.scanning = true
      this.scanResult = null
      scanOrphanFiles()
        .then((res) => {
          const data = res.data || res
          this.scanResult = data
          if (data.orphanCount === 0) {
            this.$message.success('没有发现孤儿文件')
          } else {
            this.$message.success('扫描完成，发现 ' + data.orphanCount + ' 个孤儿文件')
          }
        })
        .catch(() => {
          this.$message.error('扫描失败，请稍后重试')
        })
        .finally(() => {
          this.scanning = false
        })
    },
    onSelectionChange(rows) {
      this.selection = rows
    },
    handleCleanSelected() {
      const files = this.selection.map((r) => r.filename)
      this.doClean(files)
    },
    handleCleanAll() {
      const files = this.orphanList.map((r) => r.filename)
      this.doClean(files)
    },
    doClean(files) {
      this.$confirm('确认删除选中的 ' + files.length + ' 个孤儿文件？删除后不可恢复。', '提示', {
        type: 'warning'
      })
        .then(() => {
          this.cleaning = true
          cleanOrphanFiles(files)
            .then((res) => {
              const data = res.data || res
              this.$message.success('已删除 ' + data.deleted + ' 个文件')
              if (data.failed && data.failed.length) {
                this.$message.warning(data.failed.length + ' 个文件未能删除')
              }
              this.handleScan()
            })
            .catch(() => {
              this.$message.error('删除失败，请稍后重试')
            })
            .finally(() => {
              this.cleaning = false
            })
        })
        .catch(() => {})
    },
    handleCleanCache() {
      this.$confirm('清空证书预览缓存？下次预览证书时会自动重新渲染。', '提示', { type: 'info' })
        .then(() => {
          this.cleaningCache = true
          cleanPreviewCache()
            .then((res) => {
              const data = res.data || res
              this.$message.success('已清理 ' + (data.deleted || 0) + ' 个预览缓存文件')
            })
            .catch(() => {
              this.$message.error('清理失败')
            })
            .finally(() => {
              this.cleaningCache = false
            })
        })
        .catch(() => {})
    },
    formatSize(bytes) {
      if (bytes == null) return '-'
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / 1024 / 1024).toFixed(2) + ' MB'
    },
    formatTime(ts) {
      if (!ts) return '-'
      const d = new Date(ts)
      const p = (n) => (n < 10 ? '0' + n : n)
      return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) +
        ' ' + p(d.getHours()) + ':' + p(d.getMinutes())
    }
  }
}
</script>

<style scoped>
.file-clean-page {
  padding: 16px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.actions {
  display: flex;
  gap: 8px;
}
</style>
