<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="filter-container">
        <el-input
          v-model="query.keyword"
          placeholder="直播标题"
          clearable
          class="filter-item"
          style="width: 200px"
          @keyup.enter.native="handleSearch"
        />
        <el-select
          v-model="query.status"
          placeholder="状态"
          clearable
          class="filter-item"
          style="width: 140px"
        >
          <el-option label="未开始" :value="0" />
          <el-option label="直播中" :value="1" />
          <el-option label="已结束" :value="2" />
          <el-option label="已取消" :value="3" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" class="filter-item" @click="handleSearch">
          搜索
        </el-button>
        <el-button icon="el-icon-refresh" class="filter-item" @click="handleReset">重置</el-button>
        <el-button
          type="success"
          icon="el-icon-plus"
          class="filter-item"
          style="float: right"
          @click="handleAdd"
        >
          新增直播
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="封面" width="100" align="center">
          <template slot-scope="{ row }">
            <img v-if="row.coverUrl" :src="apiUrl(row.coverUrl)" class="table-thumb" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="直播标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="anchorName" label="主播" width="110" align="center">
          <template slot-scope="{ row }">{{ row.anchorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="回放" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.replayUrl" type="success" size="small">有</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="onlineCount" label="在线" width="70" align="center" />
        <el-table-column prop="viewCount" label="观看" width="70" align="center" />
        <el-table-column label="操作" width="340" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
            <el-button type="text" icon="el-icon-user" @click="handleOpenStudents(row)">开通学生</el-button>
            <el-button v-if="row.status !== 1" type="text" icon="el-icon-video-play" @click="handleStart(row)">开始</el-button>
            <el-button v-if="row.status === 1" type="text" icon="el-icon-video-pause" class="warn-text" @click="handleStop(row)">结束</el-button>
            <el-button v-if="row.status === 2" type="text" icon="el-icon-refresh" @click="handleReplay(row)">回放</el-button>
            <el-button v-if="row.status !== 1" type="text" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" icon="el-icon-delete" class="danger-text" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          :current-page="query.page"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="query.size"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog title="直播详情" :visible.sync="detailDialog.visible" width="620px" append-to-body>
      <div v-loading="detailDialog.loading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="直播标题" :span="2">{{ detailDialog.data.title }}</el-descriptions-item>
          <el-descriptions-item label="主播">{{ detailDialog.data.anchorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ detailDialog.data.startTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detailDialog.data.status)" size="small">
              {{ statusText(detailDialog.data.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="在线人数">{{ detailDialog.data.onlineCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="观看人次">{{ detailDialog.data.viewCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="最高在线">{{ detailDialog.data.maxOnline || 0 }}</el-descriptions-item>
          <el-descriptions-item label="流名称" :span="2">
            {{ detailDialog.data.streamName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="推流地址" :span="2">
            <div class="url-cell">
              <span class="url-text">{{ detailDialog.data.pushUrl || '-' }}</span>
              <el-button
                v-if="detailDialog.data.pushUrl"
                type="text"
                size="small"
                icon="el-icon-document-copy"
                @click="copyText(detailDialog.data.pushUrl)"
              >复制</el-button>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="播放地址" :span="2">
            <div class="url-cell">
              <span class="url-text">{{ detailDialog.data.playUrl || '-' }}</span>
              <el-button
                v-if="detailDialog.data.playUrl"
                type="text"
                size="small"
                icon="el-icon-document-copy"
                @click="copyText(detailDialog.data.playUrl)"
              >复制</el-button>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="回放地址" :span="2">
            <div class="url-cell">
              <span class="url-text">{{ detailDialog.data.replayUrl || '未设置' }}</span>
              <el-button
                v-if="detailDialog.data.replayUrl"
                type="text"
                size="small"
                icon="el-icon-document-copy"
                @click="copyText(detailDialog.data.replayUrl)"
              >复制</el-button>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="直播简介" :span="2">{{ detailDialog.data.intro || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div slot="footer">
        <el-button @click="detailDialog.visible = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 开通学生弹窗(每场直播单独开通) -->
    <open-students
      v-if="openStudentsDialog.visible"
      :visible.sync="openStudentsDialog.visible"
      :resource-id="openStudentsDialog.liveId"
      :resource-name="openStudentsDialog.liveName"
      resource-type="直播"
      :fetch-api="fetchLiveStudents"
      :open-api="openLiveStudentsApi"
      :close-api="closeLiveStudentApi"
    />

    <!-- 回放地址弹窗 -->
    <el-dialog title="设置回放地址" :visible.sync="replayDialog.visible" width="520px" append-to-body>
      <el-input v-model="replayDialog.url" placeholder="请输入回放视频直链地址(m3u8/mp4), 用于直播结束后观看" clearable />
      <div class="el-upload__tip" style="margin-top: 6px">
        直播结束后将录制文件转存为可播放链接，填写后学生即可在直播间观看回放
      </div>
      <div slot="footer">
        <el-button @click="replayDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="replayDialog.loading" @click="submitReplay">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { livePage, liveDetail, deleteLive, startLive, stopLive, setLiveReplay, getLiveStudents, openLiveStudents, closeLiveStudent } from '@/api/live'
import OpenStudents from '@/views/course/OpenStudents.vue'
import { apiUrl } from '@/utils/apiBase'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'LiveList',
  components: { OpenStudents },
  mixins: [tableMaxHeight],
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      query: {
        page: 1,
        size: 10,
        keyword: '',
        status: undefined
      },
      detailDialog: {
        visible: false,
        loading: false,
        data: {}
      },
      openStudentsDialog: {
        visible: false,
        liveId: undefined,
        liveName: ''
      },
      replayDialog: {
        visible: false,
        loading: false,
        id: undefined,
        url: ''
      }
    }
  },
  created() {
    this.fetchList()
    // 自动刷新: 推流/录制回调会变更状态, 每30秒同步一次
    this.refreshTimer = setInterval(() => {
      this.fetchList()
    }, 30000)
  },
  beforeDestroy() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
    }
  },
  methods: {
    apiUrl,
    statusText(status) {
      return ['未开始', '直播中', '已结束', '已取消'][status] || '未知'
    },
    statusType(status) {
      return ['info', 'danger', 'success', 'info'][status] || 'info'
    },
    fetchList() {
      this.loading = true
      livePage(this.query)
        .then((res) => {
          const data = res.data || {}
          this.list = data.records || data.list || data.rows || []
          this.total = data.total || 0
        })
        .catch(() => {
          this.list = []
          this.total = 0
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleSearch() {
      this.query.page = 1
      this.fetchList()
    },
    handleReset() {
      this.query = { page: 1, size: 10, keyword: '', status: undefined }
      this.fetchList()
    },
    handleSizeChange(size) {
      this.query.size = size
      this.query.page = 1
      this.fetchList()
    },
    handleCurrentChange(page) {
      this.query.page = page
      this.fetchList()
    },
    handleAdd() {
      this.$router.push('/live/edit').catch(() => {})
    },
    handleEdit(row) {
      this.$router.push(`/live/edit/${row.id}`).catch(() => {})
    },
    handleOpenStudents(row) {
      // 每场直播单独开通学生
      this.openStudentsDialog.liveId = row.id
      this.openStudentsDialog.liveName = row.title
      this.openStudentsDialog.visible = true
    },
    // 注意: Vue2 模板中的裸标识符会被编译为实例属性访问,
    // import 的模块函数必须包装成实例方法才能作为 props 传函数引用
    fetchLiveStudents(liveId, params) {
      return getLiveStudents(liveId, params)
    },
    openLiveStudentsApi(liveId, studentIds) {
      return openLiveStudents(liveId, studentIds)
    },
    closeLiveStudentApi(liveId, studentId) {
      return closeLiveStudent(liveId, studentId)
    },
    handleDetail(row) {
      this.detailDialog.visible = true
      this.detailDialog.loading = true
      this.detailDialog.data = {}
      liveDetail(row.id)
        .then((res) => {
          this.detailDialog.data = res.data || row
        })
        .catch(() => {
          this.detailDialog.data = row
        })
        .finally(() => {
          this.detailDialog.loading = false
        })
    },
    handleStart(row) {
      this.$confirm(`确定开始直播 "${row.title}" 吗? 请确认 OBS 等推流软件已使用上方推流地址。`, '开始直播', {
        type: 'warning'
      })
        .then(() => {
          startLive(row.id)
            .then(() => {
              this.$message.success('直播已开始，请使用推流地址推流')
              this.fetchList()
            })
            .catch(() => {
              this.$message.error('操作失败')
            })
        })
        .catch(() => {})
    },
    handleStop(row) {
      this.$confirm(`确定结束直播 "${row.title}" 吗?`, '结束直播', {
        type: 'warning'
      })
        .then(() => {
          stopLive(row.id)
            .then(() => {
              this.$message.success('直播已结束，可设置回放地址供事后观看')
              this.fetchList()
            })
            .catch(() => {
              this.$message.error('操作失败')
            })
        })
        .catch(() => {})
    },
    handleReplay(row) {
      this.replayDialog.id = row.id
      this.replayDialog.url = row.replayUrl || ''
      this.replayDialog.visible = true
    },
    submitReplay() {
      if (!this.replayDialog.url || !/^https?:\/\//i.test(this.replayDialog.url)) {
        this.$message.warning('请输入以 http:// 或 https:// 开头的回放地址')
        return
      }
      this.replayDialog.loading = true
      setLiveReplay(this.replayDialog.id, this.replayDialog.url.trim())
        .then(() => {
          this.$message.success('回放地址已设置')
          this.replayDialog.visible = false
          this.fetchList()
        })
        .catch(() => {
          this.$message.error('保存失败')
        })
        .finally(() => {
          this.replayDialog.loading = false
        })
    },
    handleDelete(row) {
      this.$confirm(`确定要删除直播 "${row.title}" 吗? 相关聊天记录也会一并删除。`, '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
        .then(() => {
          deleteLive(row.id)
            .then(() => {
              this.$message.success('删除成功')
              if (this.list.length === 1 && this.query.page > 1) this.query.page--
              this.fetchList()
            })
            .catch(() => {
              this.$message.error('删除失败')
            })
        })
        .catch(() => {})
    },
    copyText(text) {
      if (!text) return
      const ta = document.createElement('textarea')
      ta.value = text
      document.body.appendChild(ta)
      ta.select()
      try {
        document.execCommand('copy')
        this.$message.success('已复制')
      } catch (e) {
        this.$message.warning('复制失败，请手动复制')
      }
      document.body.removeChild(ta)
    }
  }
}
</script>

<style lang="scss" scoped>
.danger-text {
  color: #f56c6c;
}

.warn-text {
  color: #e6a23c;
}

.table-thumb {
  width: 80px;
  height: 50px;
  object-fit: cover;
  border-radius: 4px;
  vertical-align: middle;
}

.url-cell {
  display: flex;
  align-items: center;
}

.url-text {
  flex: 1;
  word-break: break-all;
  margin-right: 8px;
}
</style>
