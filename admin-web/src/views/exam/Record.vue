<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="filter-container">
        <el-input
          v-model="query.examName"
          placeholder="考试名称"
          clearable
          class="filter-item"
          style="width: 200px"
          @keyup.enter.native="handleSearch"
        />
        <el-input
          v-model="query.phone"
          placeholder="学生手机号"
          clearable
          class="filter-item"
          style="width: 180px"
          @keyup.enter.native="handleSearch"
        />
        <el-select
          v-model="query.submitStatus"
          placeholder="提交状态"
          clearable
          class="filter-item"
          style="width: 140px"
        >
          <el-option label="已提交" :value="1" />
          <el-option label="未提交" :value="0" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" class="filter-item" @click="handleSearch">
          搜索
        </el-button>
        <el-button icon="el-icon-refresh" class="filter-item" @click="handleReset">重置</el-button>
        <el-button type="danger" icon="el-icon-delete" size="small" class="filter-item" :disabled="selection.length === 0" @click="handleBatchDelete">批量删除</el-button>
        <el-button
          type="warning"
          icon="el-icon-download"
          class="filter-item"
          style="float: right"
          :loading="exporting"
          @click="handleExport"
        >
          导出Excel
        </el-button>
      </div>

      <el-table v-loading="loading" :max-height="tableMaxHeight" :data="list" border stripe style="width: 100%" @selection-change="rows => (selection = rows)">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="封面" width="90" align="center">
          <template slot-scope="{ row }">
            <img v-if="row.coverUrl" :src="apiUrl(row.coverUrl)" class="table-thumb" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="examName" label="考试名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="questionCount" label="题数" width="80" align="center" />
        <el-table-column prop="totalScore" label="总分" width="80" align="center" />
        <el-table-column label="时长" width="90" align="center">
          <template slot-scope="{ row }">{{ Math.round((row.duration || 0) / 60) }}分</template>
        </el-table-column>
        <el-table-column prop="studentName" label="学生姓名" width="110" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" align="center" />
        <el-table-column label="考试分数" width="100" align="center">
          <template slot-scope="{ row }">
            <span v-if="row.score !== null && row.score !== undefined" :class="scoreClass(row.score, row.totalScore)">
              {{ row.score }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="170" align="center" />
        <el-table-column label="待批改" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.pendingCount > 0" type="warning" size="mini">{{ row.pendingCount }}题</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="证书" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.certificate" type="success" size="mini">已发放</el-tag>
            <el-tag v-else type="info" size="mini">无</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
            <el-button type="text" icon="el-icon-edit" @click="handleGrade(row)" v-if="row.pendingCount > 0">批改</el-button>
            <el-button type="text" icon="el-icon-delete" class="danger-text" @click="handleDelete(row)">删除</el-button>
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

    <!-- 作答详情弹窗 -->
    <el-dialog title="考试作答详情" :visible.sync="detailDialog.visible" width="780px">
      <div v-loading="detailDialog.loading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="考试名称">{{ detailDialog.data.examName }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ detailDialog.data.professionName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学生姓名">{{ detailDialog.data.studentName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detailDialog.data.phone }}</el-descriptions-item>
          <el-descriptions-item label="考试分数">{{ detailDialog.data.score }} / {{ detailDialog.data.totalScore }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ detailDialog.data.submitTime }}</el-descriptions-item>
          <el-descriptions-item label="证书">{{ detailDialog.data.certificate ? '已发放' : '无' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 16px 0 8px">作答明细</h4>
        <el-table :data="detailDialog.data.answers || []" border size="mini" max-height="360">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="题型" width="80" align="center">
            <template slot-scope="{ row }">
              <el-tag size="mini" :type="typeTagType(row.type)">
                {{ typeText(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="题干" min-width="200" show-overflow-tooltip />
          <el-table-column prop="studentAnswer" label="学生作答" width="120" align="center" />
          <el-table-column prop="correctAnswer" label="正确答案" width="120" align="center" />
          <el-table-column label="结果" width="80" align="center">
            <template slot-scope="{ row }">
              <el-tag :type="row.isCorrect === 1 ? 'success' : (row.isCorrect === 2 ? 'warning' : 'danger')" size="mini">
                {{ row.isCorrect === 1 ? '正确' : (row.isCorrect === 2 ? '待批改' : '错误') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="得分" width="80" align="center" />
        </el-table>
      </div>
      <div slot="footer">
        <el-button @click="detailDialog.visible = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 批改弹窗 -->
    <el-dialog title="人工批改" :visible.sync="gradeDialog.visible" width="800px" :close-on-click-modal="false">
      <div v-loading="gradeDialog.loading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="考试名称">{{ gradeDialog.data.examName }}</el-descriptions-item>
          <el-descriptions-item label="学生姓名">{{ gradeDialog.data.studentName }}</el-descriptions-item>
          <el-descriptions-item label="当前分数">{{ gradeDialog.data.score }}</el-descriptions-item>
          <el-descriptions-item label="待批改题数">{{ gradeDialog.data.pendingCount }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 16px 0 8px">简答题批改</h4>
        <div v-for="(item, idx) in gradeDialog.data.pendingAnswers" :key="item.id" class="grade-item">
          <div class="grade-question">
            <span class="q-no">第{{ item.sort }}题</span>
            <span class="q-type el-tag el-tag--mini">简答题</span>
          </div>
          <div class="grade-content">{{ item.content }}</div>
          <div class="grade-answer">
            <span class="answer-label">学生作答：</span>
            <div class="answer-text">{{ item.studentAnswer || '（未作答）' }}</div>
          </div>
          <div class="grade-input">
            <span class="input-label">评分：</span>
            <el-input-number v-model="item.score" :min="0" :max="item.maxScore || 100" :precision="2" :step="0.5" size="small" />
            <el-radio-group v-model="item.isCorrect" size="mini" style="margin-left: 12px">
              <el-radio-button :label="1">正确</el-radio-button>
              <el-radio-button :label="0">错误</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div v-if="!gradeDialog.data.pendingAnswers || gradeDialog.data.pendingAnswers.length === 0" class="no-pending">
          没有需要批改的简答题
        </div>
      </div>
      <div slot="footer">
        <el-button @click="gradeDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="gradeDialog.submitting" @click="submitGrade">提交批改</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { examRecordPage, examRecordDetail, exportExamRecord, gradeExamRecord, deleteExamRecord, batchDeleteExamRecords } from '@/api/examRecord'
import { downloadBlob } from '@/utils'
import { apiUrl } from '@/utils/apiBase'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'ExamRecord',
  mixins: [tableMaxHeight],
  data() {
    return {
      loading: false,
      exporting: false,
      list: [],
      total: 0,
      selection: [],
      query: {
        page: 1,
        size: 10,
        examName: '',
        phone: '',
        submitStatus: undefined
      },
      detailDialog: {
        visible: false,
        loading: false,
        data: {}
      },
      gradeDialog: {
        visible: false,
        loading: false,
        submitting: false,
        data: { pendingAnswers: [] }
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    apiUrl,
    scoreClass(score, total) {
      if (total && score / total >= 0.6) return 'score-pass'
      return 'score-fail'
    },
    typeText(type) {
      const map = { 1: '单选', 2: '多选', 3: '填空', 4: '判断', 5: '简答' }
      return map[type] || '未知'
    },
    typeTagType(type) {
      const map = { 1: '', 2: 'success', 3: 'warning', 4: 'info', 5: 'danger' }
      return map[type] || ''
    },
    fetchList() {
      this.loading = true
      examRecordPage(this.query)
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
      this.query = { page: 1, size: 10, examName: '', phone: '', submitStatus: undefined }
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
    handleDetail(row) {
      this.detailDialog.visible = true
      this.detailDialog.loading = true
      this.detailDialog.data = {}
      examRecordDetail(row.id)
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
    handleExport() {
      this.exporting = true
      exportExamRecord(this.query)
        .then((response) => {
          downloadBlob(response, '考试记录.xlsx')
          this.$message.success('导出成功')
        })
        .catch(() => {
          this.$message.error('导出失败')
        })
        .finally(() => {
          this.exporting = false
        })
    },
    handleGrade(row) {
      this.gradeDialog.visible = true
      this.gradeDialog.loading = true
      this.gradeDialog.data = { pendingAnswers: [] }
      examRecordDetail(row.id)
        .then((res) => {
          const data = res.data || {}
          this.gradeDialog.data = {
            recordId: row.id,
            examName: data.examName || row.examName,
            studentName: data.studentName || row.studentName,
            score: data.score,
            pendingCount: data.pendingCount,
            pendingAnswers: (data.answers || []).filter(a => a.isCorrect === 2).map(a => ({
              id: a.id,
              sort: a.sort,
              content: a.question ? a.question.content : '',
              studentAnswer: a.studentAnswer,
              score: a.score || 0,
              isCorrect: 1,
              maxScore: a.question ? a.question.score : 100
            }))
          }
        })
        .catch(() => {
          this.$message.error('获取批改数据失败')
        })
        .finally(() => {
          this.gradeDialog.loading = false
        })
    },
    submitGrade() {
      const grades = this.gradeDialog.data.pendingAnswers.map(a => ({
        answerId: a.id,
        score: a.score,
        isCorrect: a.isCorrect
      }))
      if (grades.length === 0) {
        this.$message.warning('没有需要批改的题目')
        return
      }
      this.gradeDialog.submitting = true
      // 找到 recordId - 需要从 gradeDialog.data 或 row 获取
      const recordId = this.gradeDialog.data.recordId || this.gradeDialog.data.id
      gradeExamRecord(recordId, { grades })
        .then(() => {
          this.$message.success('批改成功，分数已更新')
          this.gradeDialog.visible = false
          this.fetchList()
        })
        .catch(() => {
          this.$message.error('批改失败')
        })
        .finally(() => {
          this.gradeDialog.submitting = false
        })
    },
    handleDelete(row) {
      this.$confirm('确定要删除该考试记录吗?', '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
        .then(() => {
          deleteExamRecord(row.id).then(() => {
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
        batchDeleteExamRecords(ids).then(() => {
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
.score-pass {
  color: #67c23a;
  font-weight: 600;
}

.score-fail {
  color: #f56c6c;
  font-weight: 600;
}

.danger-text {
  color: #f56c6c;
}

.grade-item {
  padding: 12px 16px;
  background: #f9f9f9;
  border-radius: 6px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;

  .grade-question {
    margin-bottom: 8px;
    .q-no { font-weight: 600; color: #303133; margin-right: 8px; }
  }
  .grade-content {
    font-size: 13px;
    color: #606266;
    margin-bottom: 8px;
    line-height: 1.5;
  }
  .grade-answer {
    margin-bottom: 8px;
    .answer-label { font-size: 12px; color: #909399; }
    .answer-text {
      font-size: 13px;
      color: #303133;
      padding: 8px 12px;
      background: #fff;
      border-radius: 4px;
      margin-top: 4px;
      min-height: 40px;
      white-space: pre-wrap;
    }
  }
  .grade-input {
    display: flex;
    align-items: center;
    .input-label { font-size: 13px; color: #303133; margin-right: 4px; }
  }
}
.no-pending {
  text-align: center;
  padding: 40px;
  color: #909399;
}
</style>
