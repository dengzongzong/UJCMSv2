<template>
  <div class="task-history">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filter" size="small" @submit.native.prevent>
        <el-form-item label="业务类型">
          <el-select v-model="filter.bizType" clearable placeholder="全部" style="width:200px">
            <el-option v-for="o in bizTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" clearable placeholder="全部" style="width:160px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="任务名称" clearable style="width:180px" @clear="onSearch" @keyup.enter.native="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="onSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card class="table-card" shadow="never">
      <div slot="header" class="table-header">
        <span>异步任务历史</span>
        <div class="header-info">
          <el-tooltip content="TTL 配置:任务在 DB 中保留的天数,过期自动清理。僵尸任务:running 状态超过该小时数未结束,自动标记 failed" placement="top">
            <span class="config-tag">保留 {{ taskConfig.retentionDays }} 天 / 僵尸 {{ taskConfig.zombieHours }}h</span>
          </el-tooltip>
          <el-button size="mini" :disabled="loading" @click="onRefresh">刷新</el-button>
          <el-button size="mini" type="warning" @click="onCleanupNow">立即清理</el-button>
          <el-button size="mini" type="danger" @click="onClear">清空已完成</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border stripe size="small">
        <el-table-column prop="taskId" label="任务 ID" width="220" show-overflow-tooltip />
        <el-table-column prop="bizName" label="任务" min-width="180" show-overflow-tooltip />
        <el-table-column prop="bizType" label="类型" width="180">
          <template slot-scope="s">
            <el-tag size="mini">{{ bizTypeLabel(s.row.bizType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="s">
            <el-tag v-if="s.row.status === 'success'" type="success" size="mini">成功</el-tag>
            <el-tag v-else-if="s.row.status === 'failed'" type="danger" size="mini">失败</el-tag>
            <el-tag v-else-if="s.row.status === 'cancelled'" type="info" size="mini">已取消</el-tag>
            <el-tag v-else-if="s.row.status === 'running'" type="warning" size="mini">进行中</el-tag>
            <el-tag v-else size="mini">{{ s.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="180">
          <template slot-scope="s">
            <el-progress :percentage="s.row.progress || 0" :status="progressStatus(s.row)" />
            <div class="task-counter">
              {{ s.row.processed || 0 }} / {{ s.row.total || 0 }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="160">
          <template slot-scope="s">
            <span v-if="s.row.successCount != null || s.row.failCount != null">
              成功 {{ s.row.successCount || 0 }} / 失败 {{ s.row.failCount || 0 }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100">
          <template slot-scope="s">{{ formatDuration(s.row) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template slot-scope="s">
            <el-button v-if="isPending(s.row)" size="mini" type="danger" @click="onCancel(s.row)">取消</el-button>
            <el-button v-if="canRetry(s.row)" size="mini" type="warning" @click="onRetry(s.row)">重试</el-button>
            <el-button v-if="hasResult(s.row)" size="mini" type="primary" @click="onDownload(s.row)">下载结果</el-button>
            <el-button v-if="s.row.status === 'failed'" size="mini" @click="onShowError(s.row)">错误</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        :current-page.sync="filter.page"
        :page-size.sync="filter.size"
        :page-sizes="[20, 50, 100, 200]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="loadList"
        @current-change="loadList"
      />
    </el-card>
  </div>
</template>

<script>
import { pageTasks, cancelTask, clearFinishedTasks, downloadTaskResultUrl } from '@/api/asyncTask'

export default {
  name: 'TaskHistory',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      filter: {
        bizType: '',
        status: '',
        keyword: '',
        page: 1,
        size: 20
      },
      bizTypeOptions: [
        { value: 'certificate-import', label: '证书导入' },
        { value: 'certificate-batch-generate', label: '证书批量生成' },
        { value: 'exam-qr-batch', label: '考试二维码批处理' }
      ],
      statusOptions: [
        { value: 'pending', label: '待执行' },
        { value: 'running', label: '进行中' },
        { value: 'success', label: '成功' },
        { value: 'failed', label: '失败' },
        { value: 'cancelled', label: '已取消' }
      ],
      taskConfig: { retentionDays: 7, zombieHours: 2 }
    }
  },
  mounted() {
    this.loadList()
    this.loadConfig()
  },
  methods: {
    async loadList() {
      this.loading = true
      try {
        const params = {
          page: this.filter.page,
          size: this.filter.size
        }
        if (this.filter.bizType) params.bizType = this.filter.bizType
        if (this.filter.status) params.status = this.filter.status
        const res = await pageTasks(params)
        let records = (res.data && res.data.records) || []
        // 前端关键词过滤(任务名包含)
        if (this.filter.keyword) {
          const kw = this.filter.keyword.toLowerCase()
          records = records.filter(r => (r.bizName || '').toLowerCase().includes(kw))
        }
        this.list = records
        this.total = (res.data && res.data.total) || 0
      } finally {
        this.loading = false
      }
    },
    onSearch() {
      this.filter.page = 1
      this.loadList()
    },
    onReset() {
      this.filter = { bizType: '', status: '', keyword: '', page: 1, size: 20 }
      this.loadList()
    },
    onRefresh() {
      this.loadList()
    },
    onClear() {
      this.$confirm('将清理 ' + this.taskConfig.retentionDays + ' 天前的已完成任务(仅服务端),内存中的活跃任务不受影响,确认?', '提示', { type: 'warning' })
        .then(() => clearFinishedTasks())
        .then(() => {
          this.$message.success('已清理')
          this.loadList()
        })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') this.$message.error('清理失败')
        })
    },
    onCleanupNow() {
      this.$confirm('立即触发 TTL 清理 + 僵尸任务标记(删除 ' + this.taskConfig.retentionDays + ' 天前的过期任务)', '提示', { type: 'warning' })
        .then(() => cleanupTasksNow())
        .then(res => {
          const d = res.data || {}
          this.$message.success('清理完成: 删除 ' + (d.deletedExpired || 0) + ' 条过期,标记 ' + (d.markedZombie || 0) + ' 条僵尸')
          this.loadList()
        })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') this.$message.error('清理失败')
        })
    },
    onRetry(row) {
      this.$confirm('重试该任务?将提交一个等价的任务(新任务 ID 会显示在列表中)', '重试任务', { type: 'warning' })
        .then(() => retryTask(row.taskId))
        .then(res => {
          this.$message.success('已提交重试任务: ' + (res.data ? res.data.newTaskId : ''))
          this.loadList()
        })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') {
            this.$message.error((err && err.message) || '重试失败')
          }
        })
    },
    canRetry(row) {
      if (row.status !== 'failed' && row.status !== 'cancelled') return false
      const t = row.bizType
      return t === 'certificate-import' || t === 'certificate-import-commit' || t === 'exam-qr-batch'
    },
    async loadConfig() {
      try {
        const res = await getTaskConfig()
        if (res.data) this.taskConfig = res.data
      } catch (e) { /* ignore */ }
    },
    onCancel(row) {
      this.$confirm('确定取消该任务?', '提示', { type: 'warning' })
        .then(() => cancelTask(row.taskId))
        .then(() => {
          this.$message.success('已取消')
          this.loadList()
        })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') this.$message.error('取消失败')
        })
    },
    onShowError(row) {
      this.$alert(row.errorMessage || '任务失败,无详细错误', '错误详情', { type: 'error' })
    },
    onDownload(row) {
      const url = downloadTaskResultUrl(row.taskId)
      fetch(url, {
        headers: { Authorization: 'Bearer ' + (this.$store.getters.token || '') }
      }).then(r => {
        if (!r.ok) throw new Error('download failed')
        return r.blob().then(blob => {
          const cd = r.headers.get('Content-Disposition') || ''
          let fileName = row.resultFileName || 'download'
          const m = cd.match(/filename\*=utf-8''([^;]+)/)
          if (m) fileName = decodeURIComponent(m[1])
          const a = document.createElement('a')
          a.href = URL.createObjectURL(blob)
          a.download = fileName
          a.click()
          URL.revokeObjectURL(a.href)
        })
      }).catch(() => this.$message.error('下载失败'))
    },
    bizTypeLabel(v) {
      const o = this.bizTypeOptions.find(x => x.value === v)
      return o ? o.label : v
    },
    isPending(row) {
      return row.status === 'pending'
    },
    hasResult(row) {
      return row.status === 'success' && (row.resultFileName || (row.resultFilePath))
    },
    formatDuration(row) {
      if (!row.startTime) return '-'
      const start = new Date(row.startTime).getTime()
      const end = row.endTime ? new Date(row.endTime).getTime() : Date.now()
      const sec = Math.max(0, Math.floor((end - start) / 1000))
      if (sec < 60) return sec + 's'
      return Math.floor(sec / 60) + 'm' + (sec % 60) + 's'
    },
    progressStatus(row) {
      if (row.status === 'success') return 'success'
      if (row.status === 'failed') return 'exception'
      if (row.status === 'cancelled') return 'warning'
      return ''
    }
  }
}
</script>

<style lang="scss" scoped>
.task-history {
  padding: 0;
}
.filter-card {
  margin-bottom: 12px;
}
.table-card {
  .table-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
.task-counter {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.pagination {
  margin-top: 16px;
  text-align: right;
}
.header-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.config-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  border-radius: 4px;
  margin-right: 4px;
}
</style>
