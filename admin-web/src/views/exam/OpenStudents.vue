<template>
  <el-dialog
    title="开通学生管理"
    :visible.sync="dialogVisible"
    width="760px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-loading="loading">
      <el-alert
        :title="`当前考试：${examName}`"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-tabs v-model="activeTab">
        <el-tab-pane label="已开通学生" name="opened">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-input
                v-model="openedQuery.phone"
                placeholder="搜索手机号"
                clearable
                size="small"
                style="width: 220px"
                @keyup.enter.native="fetchOpened"
              >
                <el-button slot="append" icon="el-icon-search" @click="fetchOpened" />
              </el-input>
              <el-input
                v-model="openedQuery.idCard"
                placeholder="搜索身份证号"
                clearable
                size="small"
                style="width: 220px"
                @keyup.enter.native="fetchOpened"
              >
                <el-button slot="append" icon="el-icon-search" @click="fetchOpened" />
              </el-input>
              <el-input
                v-model="openedQuery.exactCount"
                placeholder="显示最新N条"
                clearable
                size="small"
                style="width: 160px"
                @keyup.enter.native="fetchOpened"
              >
                <el-button slot="append" icon="el-icon-search" @click="fetchOpened" />
              </el-input>
              <el-input
                v-model="openedQuery.profession"
                placeholder="搜索专业"
                clearable
                size="small"
                style="width: 200px"
                @keyup.enter.native="fetchOpened"
              >
                <el-button slot="append" icon="el-icon-search" @click="fetchOpened" />
              </el-input>
            </div>
            <div>
              <el-button
                :type="unexaminedFilter ? 'primary' : 'default'"
                size="small"
                @click="toggleUnexamined"
              >
                {{ unexaminedFilter ? '显示全部' : '未考试学生' }}
              </el-button>
              <el-button
                type="warning"
                size="small"
                :disabled="openedSelected.length === 0"
                @click="handleAutoExam"
              >
                自动考试({{ openedSelected.length }})
              </el-button>
            </div>
          </div>
          <el-table :data="opened" border size="mini" max-height="360" @selection-change="handleOpenedSelectionChange">
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
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 10px; flex-wrap: wrap;">
            <el-input
              v-model="addQuery.phone"
              placeholder="搜索学生手机号"
              clearable
              size="small"
              style="width: 260px"
              @keyup.enter.native="fetchUnopened"
            >
              <el-button slot="append" icon="el-icon-search" @click="fetchUnopened" />
            </el-input>
            <el-input
              v-model="addQuery.idCard"
              placeholder="搜索身份证号"
              clearable
              size="small"
              style="width: 260px"
              @keyup.enter.native="fetchUnopened"
            >
              <el-button slot="append" icon="el-icon-search" @click="fetchUnopened" />
            </el-input>
            <el-input
              v-model="addQuery.exactCount"
              placeholder="显示最新N条"
              clearable
              size="small"
              style="width: 160px"
              @keyup.enter.native="fetchUnopened"
            >
              <el-button slot="append" icon="el-icon-search" @click="fetchUnopened" />
            </el-input>
            <el-input
              v-model="addQuery.profession"
              placeholder="搜索专业"
              clearable
              size="small"
              style="width: 200px"
              @keyup.enter.native="fetchUnopened"
            >
              <el-button slot="append" icon="el-icon-search" @click="fetchUnopened" />
            </el-input>
          </div>
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
import { getExamStudents, openExamStudents, closeExamStudent, autoExam } from '@/api/exam'

export default {
  name: 'ExamOpenStudents',
  props: {
    visible: { type: Boolean, default: false },
    examId: { type: [Number, String], default: null },
    examName: { type: String, default: '' }
  },
  data() {
    return {
      loading: false,
      submitting: false,
      activeTab: 'opened',
      opened: [],
      openedTotal: 0,
      openedQuery: { page: 1, size: 10, phone: '', idCard: '', exactCount: null, profession: '' },
      unexaminedFilter: false,
      unopened: [],
      unopenedTotal: 0,
      addQuery: { page: 1, size: 10, phone: '', idCard: '', exactCount: null, profession: '' },
      selected: [],
      openedSelected: []
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
    }
  },
  watch: {
    visible: {
      handler(val) {
        if (val && this.examId) {
          this.activeTab = 'opened'
          this.openedQuery = { page: 1, size: 10, phone: '', idCard: '', exactCount: null, profession: '' }
          this.addQuery = { page: 1, size: 10, phone: '', idCard: '', exactCount: null, profession: '' }
          this.selected = []
          this.openedSelected = []
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
  methods: {
    fetchOpened() {
      this.loading = true
      const params = {
        page: this.openedQuery.page,
        size: this.openedQuery.size,
        phone: this.openedQuery.phone,
        idCard: this.openedQuery.idCard,
        exactCount: this.openedQuery.exactCount,
        profession: this.openedQuery.profession,
        unexamined: this.unexaminedFilter ? 1 : undefined
      }
      getExamStudents(this.examId, params)
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
    toggleUnexamined() {
      this.unexaminedFilter = !this.unexaminedFilter
      this.openedQuery.page = 1
      this.fetchOpened()
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
      getExamStudents(this.examId, params)
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
      openExamStudents({ examId: this.examId, studentIds: this.selected })
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
      this.$confirm(`确定取消为 "${row.phone}" 开通该考试吗?`, '提示', { type: 'warning' })
        .then(() => {
          closeExamStudent(this.examId, row.id).then(() => {
            this.$message.success('已取消开通')
            this.fetchOpened()
          })
        })
        .catch(() => {})
    },
    handleOpenedSelectionChange(rows) {
      this.openedSelected = rows.map((r) => r.id)
    },
    handleAutoExam() {
      if (this.openedSelected.length === 0) {
        this.$message.warning('请选择要自动考试的学生')
        return
      }
      this.$confirm(
        `确定为选中的 ${this.openedSelected.length} 名学生自动完成考试吗？成绩将在70-98分之间随机生成。`,
        '提示',
        { type: 'warning' }
      )
        .then(() => {
          this.loading = true
          autoExam({ examId: this.examId, studentIds: this.openedSelected })
            .then(() => {
              this.$message.success('自动考试完成')
              this.openedSelected = []
              this.fetchOpened()
            })
            .catch(() => {
              this.$message.error('自动考试失败，请重试')
            })
            .finally(() => {
              this.loading = false
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
