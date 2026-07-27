<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="filter-container">
        <el-input
          v-model="query.name"
          placeholder="视频名称"
          clearable
          class="filter-item"
          style="width: 200px"
          @keyup.enter.native="handleSearch"
        />
        <el-select
          v-model="query.professionId"
          placeholder="专业"
          clearable
          filterable
          class="filter-item"
          style="width: 180px"
        >
          <el-option
            v-for="item in professionOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-button type="primary" icon="el-icon-search" class="filter-item" @click="handleSearch">
          搜索
        </el-button>
        <el-button icon="el-icon-refresh" class="filter-item" @click="handleReset">重置</el-button>
        <el-button type="danger" icon="el-icon-delete" size="small" class="filter-item" :disabled="selection.length === 0" @click="handleBatchDelete">批量删除</el-button>
        <el-button
          type="success"
          icon="el-icon-plus"
          class="filter-item"
          style="float: right"
          @click="handleAdd"
        >
          新增视频
        </el-button>
        <el-button
          icon="el-icon-sort"
          class="filter-item"
          style="float: right"
          @click="handleSort"
        >
          按播放量排序
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%" @selection-change="rows => (selection = rows)">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="视频名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="封面" width="110" align="center">
          <template slot-scope="{ row }">
            <img v-if="row.coverUrl" :src="apiUrl(row.coverUrl)" class="table-thumb" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="所属课程" min-width="150" show-overflow-tooltip>
          <template slot-scope="{ row }">
            {{ row.courseName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="professionName" label="专业" width="140" align="center">
          <template slot-scope="{ row }">{{ row.professionName || '-' }}</template>
        </el-table-column>
        <el-table-column label="时长" width="100" align="center">
          <template slot-scope="{ row }">{{ formatDuration(row.duration) }}</template>
        </el-table-column>
        <el-table-column label="大小" width="110" align="center">
          <template slot-scope="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="170" align="center" />
        <el-table-column prop="playCount" label="播放量" width="100" align="center" sortable />
        <el-table-column prop="studyCount" label="学习人数" width="100" align="center" sortable />
        <el-table-column label="操作" width="320" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" icon="el-icon-user" @click="handleOpenStudents(row)">开通学生</el-button>
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
    <el-dialog title="视频详情" :visible.sync="detailDialog.visible" width="640px">
      <div v-loading="detailDialog.loading">
        <video
          v-if="detailDialog.data.url"
          :src="apiUrl(detailDialog.data.url)"
          controls
          style="width: 100%; max-height: 360px; background: #000"
        />
        <div v-if="detailDialog.data.coverUrl" style="margin: 12px 0; text-align: center">
          <img :src="apiUrl(detailDialog.data.coverUrl)" style="max-width: 200px; max-height: 120px; border-radius: 4px" />
        </div>
        <el-descriptions :column="2" border style="margin-top: 16px">
          <el-descriptions-item label="视频名称">{{ detailDialog.data.name }}</el-descriptions-item>
          <el-descriptions-item label="所属课程">{{ detailDialog.data.courseName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="时长">{{ formatDuration(detailDialog.data.duration) }}</el-descriptions-item>
          <el-descriptions-item label="大小">{{ formatSize(detailDialog.data.size) }}</el-descriptions-item>
          <el-descriptions-item label="播放量">{{ detailDialog.data.playCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="基础学习人数">{{ detailDialog.data.baseStudyCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="学习人数">{{ detailDialog.data.studyCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ detailDialog.data.uploadTime }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailDialog.data.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div slot="footer">
        <el-button @click="detailDialog.visible = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 开通学生弹窗 -->
    <el-dialog
      :title="'开通学生 - ' + openStudentsDialog.videoName"
      :visible.sync="openStudentsDialog.visible"
      width="800px"
      @close="openStudentsDialog.visible = false"
    >
      <div v-loading="openStudentsDialog.loading">
        <el-tabs v-model="openStudentsDialog.activeTab" @tab-change="onTabChange">
          <el-tab-pane label="已开通" name="opened">
            <div style="margin-bottom: 10px">
              <el-input
                v-model="openStudentsDialog.openedQuery.phone"
                placeholder="手机号搜索"
                clearable
                style="width: 200px; margin-right: 10px"
                @keyup.enter.native="fetchVideoOpened"
              />
              <el-input
                v-model="openStudentsDialog.openedQuery.idCard"
                placeholder="搜索身份证号"
                clearable
                style="width: 200px; margin-right: 10px"
                @keyup.enter.native="fetchVideoOpened"
              />
              <el-input-number
                v-model="openStudentsDialog.openedQuery.exactCount"
                :min="1"
                :max="10000"
                placeholder="显示最新N条"
                style="width: 160px; margin-right: 10px"
              />
              <el-button type="primary" size="small" @click="fetchVideoOpened">搜索</el-button>
            </div>
            <el-table :data="openStudentsDialog.opened" border size="small" style="width: 100%">
              <el-table-column type="index" label="序号" width="50" align="center" />
              <el-table-column prop="name" label="姓名" width="100" />
              <el-table-column prop="studentNo" label="学号" width="120" />
              <el-table-column prop="phone" label="手机号" width="130" />
              <el-table-column prop="professionName" label="专业" width="120" />
              <el-table-column label="操作" width="80" align="center">
                <template slot-scope="{ row }">
                  <el-button type="text" class="danger-text" @click="closeVideoStudent(row)">取消</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              :current-page="openStudentsDialog.openedQuery.page"
              :page-sizes="[10, 20, 50, 100]"
              :page-size="openStudentsDialog.openedQuery.size"
              :total="openStudentsDialog.openedTotal"
              layout="total, sizes, prev, pager, next"
              style="margin-top: 10px; text-align: right"
              @size-change="(s) => { openStudentsDialog.openedQuery.size = s; openStudentsDialog.openedQuery.page = 1; fetchVideoOpened() }"
              @current-change="(p) => { openStudentsDialog.openedQuery.page = p; fetchVideoOpened() }"
            />
          </el-tab-pane>
          <el-tab-pane label="新增开通" name="add">
            <div style="margin-bottom: 10px">
              <el-input
                v-model="openStudentsDialog.addQuery.phone"
                placeholder="手机号搜索"
                clearable
                style="width: 200px; margin-right: 10px"
                @keyup.enter.native="fetchVideoUnopened"
              />
              <el-input
                v-model="openStudentsDialog.addQuery.idCard"
                placeholder="搜索身份证号"
                clearable
                style="width: 200px; margin-right: 10px"
                @keyup.enter.native="fetchVideoUnopened"
              />
              <el-input-number
                v-model="openStudentsDialog.addQuery.exactCount"
                :min="1"
                :max="10000"
                placeholder="显示最新N条"
                style="width: 160px; margin-right: 10px"
              />
              <el-button type="primary" size="small" @click="fetchVideoUnopened">搜索</el-button>
              <el-button
                type="success"
                size="small"
                :disabled="openStudentsDialog.selected.length === 0"
                style="float: right"
                @click="openVideoStudents"
              >
                批量开通 ({{ openStudentsDialog.selected.length }})
              </el-button>
            </div>
            <el-table
              :data="openStudentsDialog.unopened"
              border
              size="small"
              style="width: 100%"
              @selection-change="(val) => openStudentsDialog.selected = val"
            >
              <el-table-column type="selection" width="45" align="center" />
              <el-table-column type="index" label="序号" width="50" align="center" />
              <el-table-column prop="name" label="姓名" width="100" />
              <el-table-column prop="studentNo" label="学号" width="120" />
              <el-table-column prop="phone" label="手机号" width="130" />
              <el-table-column prop="professionName" label="专业" width="120" />
            </el-table>
            <el-pagination
              :current-page="openStudentsDialog.addQuery.page"
              :page-sizes="[10, 20, 50, 100]"
              :page-size="openStudentsDialog.addQuery.size"
              :total="openStudentsDialog.addTotal"
              layout="total, sizes, prev, pager, next"
              style="margin-top: 10px; text-align: right"
              @size-change="(s) => { openStudentsDialog.addQuery.size = s; openStudentsDialog.addQuery.page = 1; fetchVideoUnopened() }"
              @current-change="(p) => { openStudentsDialog.addQuery.page = p; fetchVideoUnopened() }"
            />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { videoPage, videoDetail, deleteVideo, sortVideo, getVideoStudents, openVideoStudents, closeVideoStudent, batchDeleteVideos } from '@/api/video'
import { professions } from '@/api/setting'
import { formatFileSize, formatDuration } from '@/utils'
import { apiUrl } from '@/utils/apiBase'

export default {
  name: 'VideoList',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      selection: [],
      professionOptions: [],
      query: {
        page: 1,
        size: 10,
        name: '',
        professionId: undefined
      },
      detailDialog: {
        visible: false,
        loading: false,
        data: {}
      },
      openStudentsDialog: {
        visible: false,
        loading: false,
        videoId: undefined,
        videoName: '',
        activeTab: 'opened',
        opened: [],
        openedTotal: 0,
        openedQuery: { page: 1, size: 10, phone: '', idCard: '', exactCount: null },
        unopened: [],
        addTotal: 0,
        addQuery: { page: 1, size: 10, phone: '', idCard: '', exactCount: null },
        selected: []
      }
    }
  },
  created() {
    this.fetchProfessions()
    this.fetchList()
  },
  methods: {
    formatSize: formatFileSize,
    formatDuration,
    apiUrl,
    fetchProfessions() {
      professions()
        .then((res) => {
          this.professionOptions = res.data || []
        })
        .catch(() => {
          this.professionOptions = []
        })
    },
    fetchList() {
      this.loading = true
      videoPage(this.query)
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
      this.query = { page: 1, size: 10, name: '', professionId: undefined }
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
      this.$router.push('/video/edit').catch(() => {})
    },
    handleEdit(row) {
      this.$router.push(`/video/edit/${row.id}`).catch(() => {})
    },
    handleDetail(row) {
      this.detailDialog.visible = true
      this.detailDialog.loading = true
      this.detailDialog.data = {}
      videoDetail(row.id)
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
    handleSort() {
      this.$confirm('确定按播放量从高到低重新排序吗?', '提示', { type: 'info' })
        .then(() => {
          this.loading = true
          sortVideo({ order: 'playCount' })
            .then(() => {
              this.$message.success('排序成功')
              this.query.page = 1
              this.fetchList()
            })
            .finally(() => {
              this.loading = false
            })
        })
        .catch(() => {})
    },
    handleDelete(row) {
      this.$confirm(`确定要删除视频 "${row.name}" 吗?`, '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
        .then(() => {
          deleteVideo(row.id)
            .then(() => {
              this.$message.success('删除成功')
              if (this.list.length === 1 && this.query.page > 1) this.query.page--
              this.fetchList()
            })
            .catch((err) => {
              if (err && err.message && err.message.indexOf('引用') > -1) {
                this.$message.error('该视频已被课程引用，不可删除')
              }
            })
        })
        .catch(() => {})
    },
    handleBatchDelete() {
      if (this.selection.length === 0) return
      this.$confirm(`确认删除选中的 ${this.selection.length} 条数据?`, '提示', {
        type: 'warning'
      }).then(() => {
        const ids = this.selection.map(item => item.id)
        batchDeleteVideos(ids).then(() => {
          this.$message.success('删除成功')
          this.selection = []
          this.fetchList()
        }).catch(() => {})
      }).catch(() => {})
    },
    handleOpenStudents(row) {
      this.openStudentsDialog.videoId = row.id
      this.openStudentsDialog.videoName = row.name
      this.openStudentsDialog.visible = true
      this.openStudentsDialog.activeTab = 'opened'
      this.openStudentsDialog.openedQuery = { page: 1, size: 10, phone: '', idCard: '', exactCount: null }
      this.openStudentsDialog.addQuery = { page: 1, size: 10, phone: '', idCard: '', exactCount: null }
      this.openStudentsDialog.selected = []
      this.fetchVideoOpened()
    },
    onTabChange(tabName) {
      if (tabName === 'add') {
        this.fetchVideoUnopened()
      }
    },
    fetchVideoOpened() {
      this.openStudentsDialog.loading = true
      const params = {
        page: this.openStudentsDialog.openedQuery.page,
        size: this.openStudentsDialog.openedQuery.size,
        phone: this.openStudentsDialog.openedQuery.phone,
        idCard: this.openStudentsDialog.openedQuery.idCard,
        exactCount: this.openStudentsDialog.openedQuery.exactCount
      }
      getVideoStudents(this.openStudentsDialog.videoId, params)
        .then((res) => {
          const data = res.data || {}
          this.openStudentsDialog.opened = data.records || data.list || data.rows || []
          this.openStudentsDialog.openedTotal = data.total || 0
        })
        .catch(() => {
          this.openStudentsDialog.opened = []
          this.openStudentsDialog.openedTotal = 0
        })
        .finally(() => {
          this.openStudentsDialog.loading = false
        })
    },
    fetchVideoUnopened() {
      this.openStudentsDialog.loading = true
      const params = {
        page: this.openStudentsDialog.addQuery.page,
        size: this.openStudentsDialog.addQuery.size,
        phone: this.openStudentsDialog.addQuery.phone,
        idCard: this.openStudentsDialog.addQuery.idCard,
        exactCount: this.openStudentsDialog.addQuery.exactCount,
        unopened: 1
      }
      getVideoStudents(this.openStudentsDialog.videoId, params)
        .then((res) => {
          const data = res.data || {}
          this.openStudentsDialog.unopened = data.records || data.list || data.rows || []
          this.openStudentsDialog.addTotal = data.total || 0
        })
        .catch(() => {
          this.openStudentsDialog.unopened = []
          this.openStudentsDialog.addTotal = 0
        })
        .finally(() => {
          this.openStudentsDialog.loading = false
        })
    },
    openVideoStudents() {
      const ids = this.openStudentsDialog.selected.map((s) => s.id)
      openVideoStudents(this.openStudentsDialog.videoId, ids)
        .then(() => {
          this.$message.success('开通成功')
          this.openStudentsDialog.selected = []
          this.openStudentsDialog.addQuery.page = 1
          this.fetchVideoUnopened()
          this.fetchVideoOpened()
        })
        .catch(() => {
          this.$message.error('开通失败')
        })
    },
    closeVideoStudent(row) {
      closeVideoStudent(this.openStudentsDialog.videoId, row.id)
        .then(() => {
          this.$message.success('已取消开通')
          this.fetchVideoOpened()
        })
        .catch(() => {
          this.$message.error('操作失败')
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.danger-text {
  color: #f56c6c;
}

.table-thumb {
  width: 80px;
  height: 50px;
  object-fit: cover;
  border-radius: 4px;
  vertical-align: middle;
}
</style>
