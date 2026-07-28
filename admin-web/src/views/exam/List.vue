<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="filter-container">
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
        <el-input
          v-model="query.name"
          placeholder="考试名称"
          clearable
          class="filter-item"
          style="width: 200px"
          @keyup.enter.native="handleSearch"
        />
        <el-input
          v-model="query.category"
          placeholder="考试分类"
          clearable
          class="filter-item"
          style="width: 180px"
          @keyup.enter.native="handleSearch"
        />
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="创建开始日期"
          end-placeholder="创建结束日期"
          value-format="yyyy-MM-dd"
          class="filter-item"
          style="width: 280px"
        />
        <el-select
          v-model="query.status"
          placeholder="状态"
          clearable
          class="filter-item"
          style="width: 140px"
        >
          <el-option label="已发布" :value="1" />
          <el-option label="未发布" :value="0" />
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
          新增考试
        </el-button>
      </div>

      <el-table v-loading="loading" :max-height="tableMaxHeight" :data="list" border stripe style="width: 100%" @selection-change="rows => (selection = rows)">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="封面图" width="100" align="center">
          <template slot-scope="{ row }">
            <img v-if="row.coverUrl" :src="apiUrl(row.coverUrl)" class="table-thumb" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="考试名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="分类" width="120" align="center">
          <template slot-scope="{ row }">{{ row.category || '-' }}</template>
        </el-table-column>
        <el-table-column prop="professionName" label="专业" width="140" align="center">
          <template slot-scope="{ row }">{{ row.professionName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题目数量" width="100" align="center" />
        <el-table-column prop="totalScore" label="总分" width="90" align="center" />
        <el-table-column label="总时长" width="100" align="center">
          <template slot-scope="{ row }">{{ row.duration || 0 }} 分钟</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '已发布' : '未发布' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
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
    <el-dialog title="考试详情" :visible.sync="detailDialog.visible" width="720px">
      <div v-loading="detailDialog.loading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="考试名称">{{ detailDialog.data.name }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ detailDialog.data.totalScore }}</el-descriptions-item>
          <el-descriptions-item label="题目数量">{{ detailDialog.data.questionCount }}</el-descriptions-item>
          <el-descriptions-item label="总时长">{{ detailDialog.data.duration }} 分钟</el-descriptions-item>
          <el-descriptions-item label="关联试卷">{{ detailDialog.data.paperName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detailDialog.data.status === 1 ? '已发布' : '未发布' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailDialog.data.createTime }}</el-descriptions-item>
          <el-descriptions-item label="简介" :span="2">{{ detailDialog.data.intro || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 16px 0 8px">题目列表</h4>
        <el-table :data="detailDialog.data.questions || []" border size="mini">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="题型" width="80" align="center">
            <template slot-scope="{ row }">
              <el-tag size="mini" :type="typeTagType(row.type)">
                {{ typeText(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="题干" min-width="240" show-overflow-tooltip />
          <el-table-column prop="score" label="分值" width="80" align="center" />
        </el-table>
      </div>
      <div slot="footer">
        <el-button @click="detailDialog.visible = false">关 闭</el-button>
      </div>
    </el-dialog>

    <exam-open-students
      v-if="openStudentsDialog.visible"
      :visible.sync="openStudentsDialog.visible"
      :exam-id="openStudentsDialog.examId"
      :exam-name="openStudentsDialog.examName"
    />
  </div>
</template>

<script>
import { examPage, examDetail, deleteExam, batchDeleteExams } from '@/api/exam'
import { professions } from '@/api/setting'
import ExamOpenStudents from './OpenStudents.vue'
import { apiUrl } from '@/utils/apiBase'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'ExamList',
  mixins: [tableMaxHeight],
  components: { ExamOpenStudents },
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
        professionId: undefined,
        name: '',
        category: '',
        status: undefined,
        dateRange: []
      },
      detailDialog: {
        visible: false,
        loading: false,
        data: {}
      },
      openStudentsDialog: {
        visible: false,
        examId: null,
        examName: ''
      }
    }
  },
  created() {
    this.fetchProfessions()
    this.fetchList()
  },
  methods: {
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
    typeText(type) {
      const map = { 1: '单选题', 2: '多选题', 3: '填空题', 4: '判断题', 5: '简答题' }
      return map[type] || '未知'
    },
    typeTagType(type) {
      const map = { 1: '', 2: 'success', 3: 'warning', 4: 'info', 5: 'danger' }
      return map[type] || ''
    },
    fetchList() {
      this.loading = true
      const params = {
        page: this.query.page,
        size: this.query.size,
        professionId: this.query.professionId,
        name: this.query.name,
        category: this.query.category,
        status: this.query.status
      }
      if (this.query.dateRange && this.query.dateRange.length === 2) {
        params.startDate = this.query.dateRange[0]
        params.endDate = this.query.dateRange[1]
      }
      examPage(params)
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
      this.query = { page: 1, size: 10, professionId: undefined, name: '', category: '', status: undefined, dateRange: [] }
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
      this.$router.push('/exam/edit').catch(() => {})
    },
    handleEdit(row) {
      this.$router.push(`/exam/edit/${row.id}`).catch(() => {})
    },
    handleDetail(row) {
      this.detailDialog.visible = true
      this.detailDialog.loading = true
      this.detailDialog.data = {}
      examDetail(row.id)
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
    handleOpenStudents(row) {
      this.openStudentsDialog.examId = row.id
      this.openStudentsDialog.examName = row.name
      this.openStudentsDialog.visible = true
    },
    handleDelete(row) {
      this.$confirm(`确定要删除考试 "${row.name}" 吗?`, '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
        .then(() => {
          deleteExam(row.id).then(() => {
            this.$message.success('删除成功')
            if (this.list.length === 1 && this.query.page > 1) this.query.page--
            this.fetchList()
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
        batchDeleteExams(ids).then(() => {
          this.$message.success('删除成功')
          this.selection = []
          this.fetchList()
        }).catch(() => {})
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.danger-text {
  color: #f56c6c;
}
</style>
