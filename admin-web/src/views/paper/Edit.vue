<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="isEdit ? '编辑试卷' : '新增试卷'" class="page-header" />

    <el-card shadow="never" v-loading="loading">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="试卷名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入试卷名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">已发布</el-radio>
                <el-radio :label="0">未发布</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="试卷描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入试卷描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-divider content-position="left">
          <span>题目选择 (已选 {{ selectedQuestionIds.length }} 题，合计 {{ totalScore }} 分)</span>
        </el-divider>

        <div class="question-filter">
          <el-select
            v-model="questionQuery.type"
            placeholder="题型"
            clearable
            size="small"
            style="width: 120px"
            @change="handleQuestionSearch"
          >
            <el-option label="单选题" :value="1" />
            <el-option label="多选题" :value="2" />
            <el-option label="填空题" :value="3" />
            <el-option label="判断题" :value="4" />
            <el-option label="简答题" :value="5" />
          </el-select>
          <el-select
            v-model="questionQuery.categoryId"
            placeholder="分类"
            clearable
            filterable
            size="small"
            style="width: 180px"
            @change="handleQuestionSearch"
          >
            <el-option
              v-for="item in categories"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-input
            v-model="questionQuery.keyword"
            placeholder="关键词"
            clearable
            size="small"
            style="width: 220px"
            @keyup.enter.native="handleQuestionSearch"
          />
          <el-button type="primary" size="small" icon="el-icon-search" @click="handleQuestionSearch">
            搜索
          </el-button>
          <el-button size="small" @click="resetQuestionQuery">重置</el-button>
        </div>

        <el-table
          :data="questions"
          border
          size="small"
          max-height="380"
          v-loading="questionLoading"
          @selection-change="handleSelectionChange"
          :row-key="rowKey"
          ref="questionTable"
        >
          <el-table-column
            type="selection"
            :reserve-selection="true"
            width="50"
            align="center"
          />
          <el-table-column label="题型" width="80" align="center">
            <template slot-scope="{ row }">
              <el-tag size="mini" :type="typeTagType(row.type)">
                {{ typeText(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" width="120" align="center" />
          <el-table-column prop="content" label="题干" min-width="240" show-overflow-tooltip />
          <el-table-column prop="score" label="分值" width="80" align="center" />
        </el-table>
        <el-pagination
          small
          :current-page="questionQuery.page"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="questionQuery.size"
          :total="questionTotal"
          layout="total, sizes, prev, pager, next"
          @size-change="handleQuestionSizeChange"
          @current-change="handleQuestionPage"
          style="margin-top: 10px; text-align: right"
        />

        <!-- 已选题目列表 -->
        <el-divider content-position="left">
          <span>已选题目 ({{ selectedQuestions.length }} 题)</span>
          <el-button
            type="text"
            size="mini"
            icon="el-icon-delete"
            class="danger-text"
            style="margin-left: 12px"
            @click="clearSelected"
          >
            清空
          </el-button>
        </el-divider>
        <el-table :data="selectedQuestions" border size="small" max-height="300">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="题型" width="80" align="center">
            <template slot-scope="{ row }">
              <el-tag size="mini" :type="typeTagType(row.type)">
                {{ typeText(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" width="120" align="center" />
          <el-table-column prop="content" label="题干" min-width="240" show-overflow-tooltip />
          <el-table-column prop="score" label="分值" width="80" align="center" />
          <el-table-column label="操作" width="80" align="center">
            <template slot-scope="{ row }">
              <el-button
                type="text"
                icon="el-icon-close"
                class="danger-text"
                @click="removeSelected(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-form-item style="margin-top: 24px">
          <el-button type="primary" :loading="submitting" @click="submitForm">保 存</el-button>
          <el-button @click="goBack">取 消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { paperDetail, addPaper, updatePaper } from '@/api/paper'
import { questionPage } from '@/api/question'
import { questionCategories } from '@/api/setting'

export default {
  name: 'PaperEdit',
  data() {
    return {
      loading: false,
      submitting: false,
      isEdit: false,
      categories: [],
      questions: [],
      questionTotal: 0,
      questionLoading: false,
      questionQuery: {
        page: 1,
        size: 10,
        type: undefined,
        categoryId: undefined,
        keyword: ''
      },
      // 已选题目（跨页保留），使用 Map 以 id 为键存储题目详情
      selectedMap: {},
      form: {
        id: undefined,
        name: '',
        description: '',
        status: 0,
        questionIds: []
      },
      rules: {
        name: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    selectedQuestionIds() {
      return Object.keys(this.selectedMap).map((id) => Number(id))
    },
    selectedQuestions() {
      return Object.values(this.selectedMap)
    },
    totalScore() {
      let total = 0
      Object.values(this.selectedMap).forEach((q) => {
        total += Number(q.score || 0)
      })
      return total
    }
  },
  created() {
    this.fetchCategories()
    this.fetchQuestions()
    const id = this.$route.params.id
    if (id) {
      this.isEdit = true
      this.fetchDetail(id)
    }
  },
  methods: {
    typeText(type) {
      const map = { 1: '单选题', 2: '多选题', 3: '填空题', 4: '判断题', 5: '简答题' }
      return map[type] || '未知'
    },
    typeTagType(type) {
      const map = { 1: '', 2: 'success', 3: 'warning', 4: 'info', 5: 'danger' }
      return map[type] || ''
    },
    rowKey(row) {
      return row.id
    },
    fetchCategories() {
      questionCategories()
        .then((res) => {
          this.categories = res.data || []
        })
        .catch(() => {
          this.categories = []
        })
    },
    fetchQuestions() {
      this.questionLoading = true
      questionPage(this.questionQuery)
        .then((res) => {
          const data = res.data || {}
          this.questions = data.records || data.list || data.rows || []
          this.questionTotal = data.total || 0
          this.$nextTick(() => {
            this.questions.forEach((q) => {
              if (this.selectedMap[q.id]) {
                this.$refs.questionTable && this.$refs.questionTable.toggleRowSelection(q, true)
              }
            })
          })
        })
        .catch(() => {
          this.questions = []
          this.questionTotal = 0
        })
        .finally(() => {
          this.questionLoading = false
        })
    },
    handleQuestionSearch() {
      this.questionQuery.page = 1
      this.fetchQuestions()
    },
    resetQuestionQuery() {
      this.questionQuery = {
        page: 1,
        size: 10,
        type: undefined,
        categoryId: undefined,
        keyword: ''
      }
      this.fetchQuestions()
    },
    handleQuestionPage(page) {
      this.questionQuery.page = page
      this.fetchQuestions()
    },
    handleQuestionSizeChange(size) {
      this.questionQuery.size = size
      this.questionQuery.page = 1
      this.fetchQuestions()
    },
    handleSelectionChange(rows) {
      // 跨页选择：根据当前页的题目增删 selectedMap
      const currentPageIds = this.questions.map((q) => q.id)
      const selectedIds = rows.map((r) => r.id)
      // 当前页未选中的，从 selectedMap 中移除
      currentPageIds.forEach((id) => {
        if (!selectedIds.includes(id)) {
          this.$delete(this.selectedMap, id)
        }
      })
      // 当前页选中的，加入 selectedMap
      rows.forEach((r) => {
        this.$set(this.selectedMap, r.id, r)
      })
    },
    removeSelected(row) {
      this.$delete(this.selectedMap, row.id)
      this.$nextTick(() => {
        // 同步取消表格选中状态
        const target = this.questions.find((q) => q.id === row.id)
        if (target) {
          this.$refs.questionTable && this.$refs.questionTable.toggleRowSelection(target, false)
        }
      })
    },
    clearSelected() {
      if (this.selectedQuestionIds.length === 0) return
      this.$confirm('确定要清空所有已选题目吗?', '提示', { type: 'warning' })
        .then(() => {
          this.selectedMap = {}
          this.$refs.questionTable && this.$refs.questionTable.clearSelection()
        })
        .catch(() => {})
    },
    fetchDetail(id) {
      this.loading = true
      paperDetail(id)
        .then((res) => {
          const data = res.data || {}
          this.form.id = data.id
          this.form.name = data.name || ''
          this.form.description = data.description || ''
          this.form.status = data.status || 0
          // 回显已选题目
          const questionIds = data.questionIds || []
          const questions = data.questions || []
          this.selectedMap = {}
          questions.forEach((q) => {
            this.$set(this.selectedMap, q.id, q)
          })
          // 刷新题目列表后回显选中状态
          this.fetchQuestions()
        })
        .catch(() => {
          this.$message.error('获取详情失败')
        })
        .finally(() => {
          this.loading = false
        })
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return
        if (this.selectedQuestionIds.length === 0) {
          this.$message.warning('请至少选择一道题目')
          return
        }
        this.submitting = true
        const payload = {
          ...this.form,
          questionIds: this.selectedQuestionIds
        }
        const action = this.isEdit ? updatePaper(payload) : addPaper(payload)
        action
          .then(() => {
            this.$message.success(this.isEdit ? '更新成功' : '新增成功')
            this.$router.push('/paper/list').catch(() => {})
          })
          .finally(() => {
            this.submitting = false
          })
      })
    },
    goBack() {
      this.$router.push('/paper/list').catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.page-header {
  margin-bottom: 16px;
}

.question-filter {
  margin-bottom: 12px;
}

.danger-text {
  color: #f56c6c;
}
</style>
