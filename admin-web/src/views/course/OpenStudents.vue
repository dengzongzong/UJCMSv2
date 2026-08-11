<template>
  <el-dialog
    title="开通学生管理"
    :visible.sync="dialogVisible"
    width="760px"
    append-to-body
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-loading="loading">
      <el-alert
        :title="displayTitle"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-tabs v-model="activeTab">
        <el-tab-pane label="已开通学生" name="opened">
          <el-input
            v-model="openedQuery.phone"
            placeholder="搜索手机号"
            clearable
            size="small"
            style="width: 220px; margin-bottom: 10px"
            @keyup.enter.native="fetchOpened"
          >
            <el-button slot="append" icon="el-icon-search" @click="fetchOpened" />
          </el-input>
          <el-input
            v-model="openedQuery.idCard"
            placeholder="搜索身份证号"
            clearable
            size="small"
            style="width: 220px; margin-bottom: 10px; margin-left: 10px"
            @keyup.enter.native="fetchOpened"
          >
            <el-button slot="append" icon="el-icon-search" @click="fetchOpened" />
          </el-input>
          <el-input
            v-model="openedQuery.exactCount"
            placeholder="显示最新N条"
            clearable
            size="small"
            style="width: 180px; margin-bottom: 10px; margin-left: 10px"
            @keyup.enter.native="fetchOpened"
          >
            <el-button slot="append" icon="el-icon-search" @click="fetchOpened" />
          </el-input>
          <el-select
            v-model="openedQuery.profession"
            placeholder="选择专业"
            clearable
            filterable
            size="small"
            style="width: 200px; margin-bottom: 10px; margin-left: 10px"
            @change="fetchOpened"
          >
            <el-option
              v-for="item in professionOptions"
              :key="item.id"
              :label="item.name"
              :value="item.name"
            />
          </el-select>
          <el-table :data="opened" border size="mini" max-height="360">
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="name" label="姓名" min-width="100" show-overflow-tooltip>
              <template slot-scope="{ row }">{{ row.name || row.realName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="professionName" label="专业" min-width="120" show-overflow-tooltip>
              <template slot-scope="{ row }">{{ row.professionName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" min-width="140" />
            <el-table-column prop="createTime" label="创建时间" min-width="160" show-overflow-tooltip>
              <template slot-scope="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template slot-scope="{ row }">
                <el-button
                  type="text"
                  icon="el-icon-close"
                  class="danger-text"
                  @click="handleCloseStudent(row)"
                >
                  取消开通
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            small
            :current-page="openedQuery.page"
            :page-sizes="[10, 20, 50, 100]"
            :page-size="openedQuery.size"
            :total="openedTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="handleOpenedSizeChange"
            @current-change="handleOpenedPage"
            style="margin-top: 10px; text-align: right"
          />
        </el-tab-pane>
        <el-tab-pane label="新增开通" name="add">
          <el-input
            v-model="addQuery.phone"
            placeholder="搜索学生手机号"
            clearable
            size="small"
            style="width: 260px; margin-bottom: 10px"
            @keyup.enter.native="fetchUnopened"
          >
            <el-button slot="append" icon="el-icon-search" @click="fetchUnopened" />
          </el-input>
          <el-input
            v-model="addQuery.idCard"
            placeholder="搜索身份证号"
            clearable
            size="small"
            style="width: 220px; margin-bottom: 10px; margin-left: 10px"
            @keyup.enter.native="fetchUnopened"
          >
            <el-button slot="append" icon="el-icon-search" @click="fetchUnopened" />
          </el-input>
          <el-input
            v-model="addQuery.exactCount"
            placeholder="显示最新N条"
            clearable
            size="small"
            style="width: 180px; margin-bottom: 10px; margin-left: 10px"
            @keyup.enter.native="fetchUnopened"
          >
            <el-button slot="append" icon="el-icon-search" @click="fetchUnopened" />
          </el-input>
          <el-select
            v-model="addQuery.profession"
            placeholder="选择专业"
            clearable
            filterable
            size="small"
            style="width: 200px; margin-bottom: 10px; margin-left: 10px"
            @change="fetchUnopened"
          >
            <el-option
              v-for="item in professionOptions"
              :key="item.id"
              :label="item.name"
              :value="item.name"
            />
          </el-select>
          <el-table
            :data="unopened"
            border
            size="mini"
            max-height="360"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="50" align="center" />
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="name" label="姓名" min-width="100" show-overflow-tooltip>
              <template slot-scope="{ row }">{{ row.name || row.realName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="professionName" label="专业" min-width="120" show-overflow-tooltip>
              <template slot-scope="{ row }">{{ row.professionName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" min-width="140" />
            <el-table-column prop="createTime" label="创建时间" min-width="160" show-overflow-tooltip>
              <template slot-scope="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
          <el-pagination
            small
            :current-page="addQuery.page"
            :page-sizes="[10, 20, 50, 100]"
            :page-size="addQuery.size"
            :total="unopenedTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="handleUnopenedSizeChange"
            @current-change="handleUnopenedPage"
            style="margin-top: 10px; text-align: right"
          />
        </el-tab-pane>
      </el-tabs>
    </div>
    <div slot="footer">
      <el-button @click="dialogVisible = false">关 闭</el-button>
      <el-button
        v-if="activeTab === 'add'"
        type="primary"
        :loading="submitting"
        :disabled="selected.length === 0"
        @click="submitOpen"
      >
        确认开通({{ selected.length }})
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getCourseStudents, openCourseStudents, closeCourseStudent } from '@/api/course'
import { professions } from '@/api/setting'

export default {
  name: 'CourseOpenStudents',
  props: {
    visible: { type: Boolean, default: false },
    // 兼容旧调用(课程/考试): 传 courseId + courseName, 内部使用课程开通接口
    courseId: { type: [Number, String], default: null },
    courseName: { type: String, default: '' },
    // 通用调用(直播等): 传 resourceId/resourceName + 自定义API
    resourceId: { type: [Number, String], default: null },
    resourceName: { type: String, default: '' },
    resourceType: { type: String, default: '课程' },
    fetchApi: { type: Function, default: null },
    openApi: { type: Function, default: null },
    closeApi: { type: Function, default: null }
  },
  data() {
    return {
      loading: false,
      submitting: false,
      activeTab: 'opened',
      professionOptions: [],
      opened: [],
      openedTotal: 0,
      openedQuery: { page: 1, size: 10, phone: '', idCard: '', exactCount: null, profession: '' },
      unopened: [],
      unopenedTotal: 0,
      addQuery: { page: 1, size: 10, phone: '', idCard: '', exactCount: null, profession: '' },
      selected: []
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    },
    rid() {
      return this.resourceId !== null && this.resourceId !== undefined ? this.resourceId : this.courseId
    },
    displayTitle() {
      const name = this.resourceName || this.courseName || ''
      return `当前${this.resourceType}：${name}`
    }
  },
  watch: {
    visible: {
      handler(val) {
        if (val && this.rid) {
          this.activeTab = 'opened'
          this.openedQuery = { page: 1, size: 10, phone: '', idCard: '', exactCount: null, profession: '' }
          this.addQuery = { page: 1, size: 10, phone: '', idCard: '', exactCount: null, profession: '' }
          this.selected = []
          this.fetchOpened()
        }
      },
      immediate: true
    },
    activeTab(val) {
      // 切换到 "新增开通" tab 时,总是重新拉取学生列表(避免依赖 unopened.length === 0 漏触发)
      if (val === 'add') {
        this.fetchUnopened()
      }
    }
  },
  created() {
    this.fetchProfessions()
  },
  methods: {
    fetchProfessions() {
      professions().then(res => {
        this.professionOptions = res.data || []
      }).catch(() => {})
    },
    fetchOpened() {
      this.loading = true
      const params = {
        page: this.openedQuery.page,
        size: this.openedQuery.size,
        phone: this.openedQuery.phone,
        idCard: this.openedQuery.idCard,
        exactCount: this.openedQuery.exactCount,
        profession: this.openedQuery.profession
      }
      const fetchFn = this.fetchApi || getCourseStudents
      fetchFn(this.rid, params)
        .then((res) => {
          const data = res.data || {}
          this.opened = data.records || data.list || data.rows || []
          this.openedTotal = data.total || 0
        })
        .catch(() => {
          this.opened = []
          this.openedTotal = 0
        })
        .finally(() => {
          this.loading = false
        })
    },
    fetchUnopened() {
      this.loading = true
      const params = {
        page: this.addQuery.page,
        size: this.addQuery.size,
        phone: this.addQuery.phone,
        idCard: this.addQuery.idCard,
        exactCount: this.addQuery.exactCount,
        profession: this.addQuery.profession,
        unopened: 1
      }
      const fetchFn = this.fetchApi || getCourseStudents
      fetchFn(this.rid, params)
        .then((res) => {
          const data = res.data || {}
          this.unopened = data.records || data.list || data.rows || []
          this.unopenedTotal = data.total || 0
        })
        .catch(() => {
          this.unopened = []
          this.unopenedTotal = 0
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleOpenedPage(page) {
      this.openedQuery.page = page
      this.fetchOpened()
    },
    handleOpenedSizeChange(size) {
      this.openedQuery.size = size
      this.openedQuery.page = 1
      this.fetchOpened()
    },
    handleUnopenedPage(page) {
      this.addQuery.page = page
      this.fetchUnopened()
    },
    handleUnopenedSizeChange(size) {
      this.addQuery.size = size
      this.addQuery.page = 1
      this.fetchUnopened()
    },
    handleSelectionChange(rows) {
      this.selected = rows.map((r) => r.id)
    },
    submitOpen() {
      if (this.selected.length === 0) {
        this.$message.warning('请选择要开通的学生')
        return
      }
      this.submitting = true
      const openFn = this.openApi || ((rid, ids) => openCourseStudents({ courseId: rid, studentIds: ids }))
      openFn(this.rid, this.selected)
        .then(() => {
          this.$message.success('开通成功')
          this.selected = []
          this.activeTab = 'opened'
          this.openedQuery.page = 1
          this.fetchOpened()
          this.fetchUnopened()
        })
        .catch(() => {
          this.$message.error('开通失败，请重试')
        })
        .finally(() => {
          this.submitting = false
        })
    },
    handleCloseStudent(row) {
      this.$confirm(`确定取消为 "${row.phone}" 开通吗?`, '提示', { type: 'warning' })
        .then(() => {
          const closeFn = this.closeApi || closeCourseStudent
          closeFn(this.rid, row.id).then(() => {
            this.$message.success('已取消开通')
            this.fetchOpened()
          })
        })
        .catch(() => {})
    },
    handleClose() {
      this.$emit('update:visible', false)
    },
    formatTime(t) {
      if (!t) return '-'
      if (typeof t === 'string') return t.replace('T', ' ').substring(0, 19)
      return '-'
    }
  }
}
</script>

<style lang="scss" scoped>
.danger-text {
  color: #f56c6c;
}
</style>
