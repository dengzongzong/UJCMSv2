<template>
  <div class="task-center">
    <el-button class="task-fab" :type="hasActive ? 'warning' : 'info'" size="mini" @click="dialogVisible = true">
      <i class="el-icon-loading" v-if="hasActive"></i>
      <i class="el-icon-tickets" v-else></i>
      <span>任务中心</span>
      <el-badge v-if="activeList.length" :value="activeList.length" :max="9" class="task-badge" />
    </el-button>

    <el-dialog
      title="异步任务中心"
      :visible.sync="dialogVisible"
      width="720px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-tabs v-model="tab" @tab-click="onTabClick">
        <el-tab-pane label="进行中" name="active">
          <span slot="label">
            进行中 <el-badge v-if="activeList.length" :value="activeList.length" :max="9" class="tab-badge" />
          </span>
          <div v-if="activeList.length === 0" class="empty">暂无进行中的任务</div>
          <el-table v-else :data="activeList" border size="small">
            <el-table-column prop="bizName" label="任务" min-width="160" />
            <el-table-column label="进度" min-width="200">
              <template slot-scope="s">
                <el-progress :percentage="s.row.progress" :status="progressStatus(s.row)" />
                <div class="task-counter">
                  {{ s.row.processed || 0 }} / {{ s.row.total || 0 }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="100">
              <template slot-scope="s">{{ formatDuration(s.row) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="220">
              <template slot-scope="s">
                <el-button v-if="canDryRunCommit(s.row)" size="mini" type="primary" @click="onDryRunCommit(s.row)">确认导入</el-button>
                <el-button size="mini" type="danger" @click="onCancel(s.row.taskId)">取消</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="ws-status">
            <i :class="wsConnected ? 'el-icon-success' : 'el-icon-warning-outline'"></i>
            {{ wsConnected ? 'WebSocket 已连接' : 'WebSocket 未连接,使用轮询' }}
          </div>
        </el-tab-pane>

        <el-tab-pane label="已完成" name="finished">
          <div v-if="finishedList.length === 0" class="empty">暂无已完成任务</div>
          <el-table v-else :data="finishedList" border size="small">
            <el-table-column prop="bizName" label="任务" min-width="160" />
            <el-table-column label="状态" width="100">
              <template slot-scope="s">
                <el-tag v-if="s.row.status === 'success'" type="success" size="mini">完成</el-tag>
                <el-tag v-else-if="s.row.status === 'failed'" type="danger" size="mini">失败</el-tag>
                <el-tag v-else-if="s.row.status === 'cancelled'" type="info" size="mini">已取消</el-tag>
                <el-tag v-else size="mini">{{ s.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="结果" min-width="120">
              <template slot-scope="s">
                <span v-if="s.row.successCount != null">成功 {{ s.row.successCount }} 失败 {{ s.row.failCount }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="100">
              <template slot-scope="s">{{ formatDuration(s.row) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="240">
              <template slot-scope="s">
                <el-button v-if="canRetry(s.row)" size="mini" type="warning" @click="onRetry(s.row)">重试</el-button>
                <el-button v-if="canDryRunCommit(s.row)" size="mini" type="primary" @click="onDryRunCommit(s.row)">确认导入</el-button>
                <el-button v-if="s.row.resultFile" size="mini" type="primary" @click="onDownload(s.row)">下载结果</el-button>
                <el-button v-if="s.row.status === 'failed'" size="mini" type="text" @click="showError(s.row)">错误</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="finishedList.length" class="clear-bar">
            <span class="config-hint">保留 {{ taskConfig.retentionDays }} 天 / 僵尸判定 {{ taskConfig.zombieHours }}h</span>
            <el-button size="mini" @click="onClearFinished">清空已完成</el-button>
            <el-button size="mini" type="warning" @click="onCleanupNow">立即清理</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script>
import {
  listActiveTasks,
  listAllTasks,
  cancelTask,
  retryTask,
  cleanupTasksNow,
  getTaskConfig,
  clearFinishedTasks,
  downloadTaskResultUrl,
  getTask
} from '@/api/asyncTask'
import { apiBase } from '@/utils/apiBase'

export default {
  name: 'TaskCenter',
  data() {
    return {
      dialogVisible: false,
      tab: 'active',
      activeList: [],
      finishedList: [],
      timer: null,
      pollInterval: 5000,  // 5s 轮询兜底(WS 推送是主通道)
      ws: null,
      wsConnected: false,
      wsReconnectTimer: null,
      taskConfig: { retentionDays: 7, zombieHours: 2 }
    }
  },
  computed: {
    hasActive() {
      return this.activeList.length > 0
    }
  },
  mounted() {
    this.startPolling()
    this.connectWebSocket()
  },
  beforeDestroy() {
    this.stopPolling()
    this.closeWebSocket()
  },
  methods: {
    startPolling() {
      this.refresh()
      this.timer = setInterval(() => {
        this.refresh()
      }, this.pollInterval)
    },
    stopPolling() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },

    // ============ WebSocket ============
    connectWebSocket() {
      if (typeof WebSocket === 'undefined') return
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const base = apiBase().replace(/^https?:/, '')
      const url = protocol + '//' + window.location.host + base + '/ws/task'
      try {
        this.ws = new WebSocket(url)
      } catch (e) {
        // 失败继续走轮询
        return
      }
      this.ws.onopen = () => {
        this.wsConnected = true
        // 重连后,重新订阅所有 active 任务
        for (const t of this.activeList) this.subscribe(t.taskId)
      }
      this.ws.onclose = () => {
        this.wsConnected = false
        // 自动重连
        if (this.wsReconnectTimer) clearTimeout(this.wsReconnectTimer)
        this.wsReconnectTimer = setTimeout(() => this.connectWebSocket(), 5000)
      }
      this.ws.onerror = () => { /* ignore */ }
      this.ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data)
          if (msg.action === 'update' && msg.task) {
            this.applyTaskUpdate(msg.task, true)
          }
        } catch (e) { /* ignore */ }
      }
    },
    closeWebSocket() {
      if (this.wsReconnectTimer) clearTimeout(this.wsReconnectTimer)
      if (this.ws) {
        try { this.ws.close() } catch (e) {}
        this.ws = null
      }
    },
    subscribe(taskId) {
      if (this.ws && this.ws.readyState === 1) {
        this.ws.send(JSON.stringify({ action: 'subscribe', taskId }))
      }
    },
    unsubscribe(taskId) {
      if (this.ws && this.ws.readyState === 1) {
        this.ws.send(JSON.stringify({ action: 'unsubscribe', taskId }))
      }
    },

    // ============ Polling ============
    async refresh() {
      try {
        const res = await listActiveTasks()
        const list = res.data || []
        // 合并 WS 推送的实时数据
        const prevMap = new Map(this.activeList.map(t => [t.taskId, t]))
        this.activeList = list.map(t => {
          const prev = prevMap.get(t.taskId)
          if (prev) {
            // 用 prev 中更新的 progress 覆盖
            if (prev.progress > t.progress) t.progress = prev.progress
            if (prev.processed > t.processed) t.processed = prev.processed
            if (prev.successCount > (t.successCount || 0)) t.successCount = prev.successCount
            if (prev.failCount > (t.failCount || 0)) t.failCount = prev.failCount
          }
          return t
        })
        // 订阅所有 active
        for (const t of this.activeList) this.subscribe(t.taskId)
      } catch (e) {
        // 静默
      }
      if (this.tab === 'finished' && this.dialogVisible) {
        this.loadFinished()
      }
    },
    applyTaskUpdate(task, fromWs) {
      if (fromWs && (!task || !task.taskId)) return
      const idx = this.activeList.findIndex(t => t.taskId === task.taskId)
      if (idx >= 0) {
        // 合并
        this.activeList.splice(idx, 1, { ...this.activeList[idx], ...task })
        if (task.status === 'success' || task.status === 'failed' || task.status === 'cancelled') {
          // 移出 active 列表
          setTimeout(() => {
            this.activeList = this.activeList.filter(t => t.taskId !== task.taskId)
            this.$notify({
              title: '任务完成',
              message: (task.bizName || task.taskId) + ' - ' + (task.status === 'success' ? '成功' : (task.status === 'failed' ? '失败' : '已取消')),
              type: task.status === 'success' ? 'success' : (task.status === 'failed' ? 'error' : 'warning'),
              duration: 4500
            })
            // 已经在弹窗里则刷新 finished
            if (this.dialogVisible && this.tab === 'finished') this.loadFinished()
          }, 1500)
        }
      } else {
        // 新任务(WS 推送但本地还没有),加入 activeList
        this.activeList.push(task)
      }
    },

    async loadFinished() {
      try {
        const res = await listAllTasks()
        const all = res.data || []
        this.finishedList = all
          .filter(t => t.status === 'success' || t.status === 'failed' || t.status === 'cancelled')
          .slice(0, 20)
      } catch (e) {}
    },
    onTabClick() {
      if (this.tab === 'finished') this.loadFinished()
    },
    onCancel(taskId) {
      this.$confirm('确定要取消该任务吗?', '提示', { type: 'warning' }).then(() => {
        return cancelTask(taskId)
      }).then(() => {
        this.$message.success('已取消')
        this.refresh()
      }).catch(() => {})
    },
    onClearFinished() {
      this.$confirm('清空所有已完成任务?', '提示', { type: 'warning' }).then(() => {
        return clearFinishedTasks()
      }).then(() => {
        this.finishedList = []
      }).catch(() => {})
    },
    onCleanupNow() {
      this.$confirm('立即触发 TTL 清理(清理过期任务 + 标记僵尸任务),将删除 7 天前的数据。', '提示', { type: 'warning' })
        .then(() => cleanupTasksNow())
        .then(res => {
          const d = res.data || {}
          this.$message.success('清理完成: 删除 ' + (d.deletedExpired || 0) + ' 条过期,标记 ' + (d.markedZombie || 0) + ' 条僵尸')
          this.loadFinished()
        })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') this.$message.error('清理失败')
        })
    },
    onRetry(task) {
      this.$confirm('重试该任务?将提交一个等价的任务(新任务 ID 会显示在"进行中"列表)', '重试任务', { type: 'warning' })
        .then(() => retryTask(task.taskId))
        .then(res => {
          this.$message.success('已提交重试任务: ' + (res.data ? res.data.newTaskId : ''))
          this.refresh()
        })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') {
            const msg = (err && err.message) || '重试失败'
            this.$message.error(msg)
          }
        })
    },
    canRetry(task) {
      // 只有 failed/cancelled 状态,且 bizType 属于"可重试"范围
      if (task.status !== 'failed' && task.status !== 'cancelled') return false
      const t = task.bizType
      return t === 'certificate-import' || t === 'certificate-import-commit' || t === 'exam-qr-batch'
    },
    async loadConfig() {
      try {
        const res = await getTaskConfig()
        if (res.data) this.taskConfig = res.data
      } catch (e) { /* ignore */ }
    },
    onDownload(task) {
      const url = downloadTaskResultUrl(task.taskId)
      fetch(url, {
        headers: { Authorization: 'Bearer ' + (this.$store.getters.token || '') }
      }).then(r => {
        if (!r.ok) throw new Error('download failed')
        return r.blob().then(blob => {
          const cd = r.headers.get('Content-Disposition') || ''
          let fileName = task.resultFileName || 'download'
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
    showError(task) {
      this.$alert(task.errorMessage || '任务失败', '错误详情', { type: 'error' })
    },
    canDryRunCommit(task) {
      // 异步 dry-run 任务成功完成,且 extraJson 含 dryRunToken 即可走 commit
      return task.status === 'success' && task.bizType === 'certificate-import' && task.resultFileName == null
    },
    onDryRunCommit(task) {
      // 直接调 backend commitImport by taskId(后端从 extraJson 取 token)
      this.$confirm('确认将该批次数据导入数据库?', '确认导入', { type: 'warning' })
        .then(() => {
          return import('@/api/certificate').then(mod => mod.commitImport({ taskId: task.taskId }))
        })
        .then(res => {
          if (res.data && res.data.async) {
            this.$message.success('已提交确认导入任务,请在任务中心查看进度')
            this.refresh()
          } else {
            const r = res.data || {}
            this.$alert('成功 ' + (r.successCount || 0) + ' 条,失败 ' + (r.failCount || 0) + ' 条', '导入完成', {
              type: r.failCount > 0 ? 'warning' : 'success'
            })
            this.refresh()
          }
        })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') this.$message.error('操作失败')
        })
    },
    formatDuration(task) {
      if (!task.startTime) return '-'
      const start = new Date(task.startTime).getTime()
      const end = task.endTime ? new Date(task.endTime).getTime() : Date.now()
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
.task-center {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1000;
}
.task-fab {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  border-radius: 20px;
  padding: 8px 16px;
}
.task-badge {
  margin-left: 6px;
}
.tab-badge {
  margin-left: 4px;
}
.task-counter {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.empty {
  text-align: center;
  color: #909399;
  padding: 32px 0;
}
.clear-bar {
  text-align: right;
  margin-top: 12px;
}
.config-hint {
  margin-right: 12px;
  color: #909399;
  font-size: 12px;
}
.ws-status {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: right;
  i { margin-right: 4px; }
}
</style>
