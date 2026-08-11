<template>
  <div class="app-container">
    <el-card shadow="never">
      <!-- 筛选区 -->
      <div class="filter-container">
        <el-input
          v-model="query.name"
          placeholder="模板名称"
          clearable
          class="filter-item"
          style="width: 220px"
          @keyup.enter.native="onSearch"
        />
        <el-select
          v-model="query.categoryId"
          placeholder="分类"
          clearable
          filterable
          class="filter-item"
          style="width: 180px"
        >
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
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
        <el-button type="primary" icon="el-icon-search" class="filter-item" @click="onSearch">
          搜索
        </el-button>
        <el-button icon="el-icon-refresh" class="filter-item" @click="onReset">重置</el-button>
        <el-button
          type="success"
          icon="el-icon-plus"
          class="filter-item"
          style="float: right"
          @click="openCreate"
        >
          新建模板
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="模板名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="分类" width="120" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" type="info">{{ row.categoryName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="专业" width="140" align="center">
          <template slot-scope="{ row }">{{ row.professionName || '-' }}</template>
        </el-table-column>
        <el-table-column label="题目数量" width="100" align="center">
          <template slot-scope="{ row }">
            {{ row.questionCount != null ? row.questionCount : (row.questions ? row.questions.length : 0) }}
          </template>
        </el-table-column>
        <el-table-column label="总分" width="90" align="center">
          <template slot-scope="{ row }">
            {{ row.totalScore != null ? row.totalScore : computeTotalScore(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="220" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="openView(row)">查看</el-button>
            <el-button type="text" icon="el-icon-edit" @click="openEdit(row)">编辑</el-button>
            <el-button
              type="text"
              icon="el-icon-delete"
              class="danger-text"
              @click="onDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          :current-page="query.page"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="query.size"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="onSizeChange"
          @current-change="onCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新建/编辑弹窗 -->
    <el-dialog
      :title="dialog.isEdit ? '编辑模板' : '新建模板'"
      :visible.sync="dialog.visible"
      width="820px"
      append-to-body
      :close-on-click-modal="false"
      @closed="resetDialogForm"
    >
      <el-form
        ref="dialogForm"
        v-loading="dialog.loading"
        :model="dialog.form"
        :rules="dialogRules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模板名称" prop="name">
              <el-input
                v-model="dialog.form.name"
                placeholder="请输入模板名称"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select
                v-model="dialog.form.categoryId"
                placeholder="请选择分类"
                clearable
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="专业" prop="professionId">
              <el-select
                v-model="dialog.form.professionId"
                placeholder="请选择专业"
                clearable
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="item in professionOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="题目数量">
              <el-input :value="dialog.form.questions.length + ' 题'" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="模板描述" prop="description">
          <el-input
            v-model="dialog.form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模板描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-divider content-position="left">
          <span>已选题目 ({{ dialog.form.questions.length }} 题，合计 {{ dialogTotalScore }} 分)</span>
          <el-button
            type="primary"
            plain
            size="mini"
            icon="el-icon-plus"
            style="margin-left: 12px"
            @click="openQuestionPicker"
          >
            选择题目
          </el-button>
          <el-button
            v-if="dialog.form.questions.length"
            type="text"
            size="mini"
            icon="el-icon-delete"
            class="danger-text"
            @click="clearQuestions"
          >
            清空
          </el-button>
        </el-divider>

        <el-table :data="dialog.form.questions" border size="small" max-height="300">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="题型" width="80" align="center">
            <template slot-scope="{ row }">
              <el-tag size="mini" :type="typeTagType(row.type)">{{ typeText(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" width="120" align="center">
            <template slot-scope="{ row }">{{ row.categoryName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="stem" label="题干" min-width="240" show-overflow-tooltip />
          <el-table-column prop="score" label="分值" width="80" align="center" />
          <el-table-column label="操作" width="80" align="center">
            <template slot-scope="{ row }">
              <el-button
                type="text"
                icon="el-icon-close"
                class="danger-text"
                @click="removeQuestion(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <div slot="footer">
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="onSave">保 存</el-button>
      </div>
    </el-dialog>

    <!-- 题目选择弹窗 -->
    <el-dialog
      title="选择题目"
      :visible.sync="questionPicker.visible"
      width="920px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="filter-container">
        <el-select
          v-model="questionPicker.query.type"
          placeholder="题型"
          clearable
          size="small"
          class="filter-item"
          style="width: 120px"
          @change="onPickerSearch"
        >
          <el-option label="单选题" :value="1" />
          <el-option label="多选题" :value="2" />
          <el-option label="填空题" :value="3" />
          <el-option label="判断题" :value="4" />
          <el-option label="简答题" :value="5" />
        </el-select>
        <el-select
          v-model="questionPicker.query.categoryId"
          placeholder="分类"
          clearable
          filterable
          size="small"
          class="filter-item"
          style="width: 160px"
          @change="onPickerSearch"
        >
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-input
          v-model="questionPicker.query.keyword"
          placeholder="关键词(题干/解析)"
          clearable
          size="small"
          class="filter-item"
          style="width: 220px"
          @keyup.enter.native="onPickerSearch"
        />
        <el-button
          type="primary"
          size="small"
          icon="el-icon-search"
          class="filter-item"
          @click="onPickerSearch"
        >
          搜索
        </el-button>
        <el-button size="small" class="filter-item" @click="onPickerReset">重置</el-button>
        <span class="filter-item" style="color: #909399; font-size: 12px">
          已选 {{ questionPicker.selected.length }} 题
        </span>
      </div>

      <el-table
        ref="pickerTable"
        v-loading="questionPicker.loading"
        :data="questionPicker.list"
        border
        size="small"
        max-height="380"
        :row-key="rowKey"
        @selection-change="onPickerSelectionChange"
      >
        <el-table-column type="selection" :reserve-selection="true" width="50" align="center" />
        <el-table-column label="题型" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="typeTagType(row.type)">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="120" align="center">
          <template slot-scope="{ row }">{{ row.categoryName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" min-width="260" show-overflow-tooltip />
        <el-table-column prop="score" label="分值" width="80" align="center" />
      </el-table>
      <el-pagination
        small
        style="margin-top: 10px; text-align: right"
        :current-page="questionPicker.query.page"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="questionPicker.query.size"
        :total="questionPicker.total"
        layout="total, sizes, prev, pager, next"
        @size-change="onPickerSizeChange"
        @current-change="onPickerPageChange"
      />
      <div slot="footer">
        <el-button @click="questionPicker.visible = false">取 消</el-button>
        <el-button type="primary" @click="confirmPickQuestions">
          确 认 ({{ questionPicker.selected.length }})
        </el-button>
      </div>
    </el-dialog>

    <!-- 查看弹窗 -->
    <el-dialog
      :title="'模板详情：' + viewDialog.name"
      :visible.sync="viewDialog.visible"
      width="800px"
      append-to-body
    >
      <el-table
        v-loading="viewDialog.loading"
        :data="viewDialog.list"
        border
        size="small"
        max-height="460"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="题型" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="typeTagType(row.type)">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="120" align="center">
          <template slot-scope="{ row }">{{ row.categoryName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" min-width="280" show-overflow-tooltip />
        <el-table-column prop="score" label="分值" width="80" align="center" />
      </el-table>
      <div slot="footer">
        <el-button @click="viewDialog.visible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  questionTemplatePage,
  questionTemplateDetail,
  addQuestionTemplate,
  updateQuestionTemplate,
  deleteQuestionTemplate
} from '@/api/questionTemplate'
import { questionPage } from '@/api/question'
import { questionCategories, professions } from '@/api/setting'

export default {
  name: 'QuestionTemplate',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      categoryOptions: [],
      professionOptions: [],
      query: {
        page: 1,
        size: 10,
        name: '',
        categoryId: undefined,
        professionId: undefined
      },
      dialog: {
        visible: false,
        isEdit: false,
        loading: false,
        submitting: false,
        form: {
          id: undefined,
          name: '',
          description: '',
          categoryId: undefined,
          professionId: undefined,
          questionIds: [],
          questions: []
        }
      },
      dialogRules: {
        name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
      },
      questionPicker: {
        visible: false,
        loading: false,
        list: [],
        total: 0,
        query: {
          page: 1,
          size: 10,
          keyword: '',
          type: undefined,
          categoryId: undefined
        },
        selected: []
      },
      viewDialog: {
        visible: false,
        loading: false,
        name: '',
        list: []
      }
    }
  },
  computed: {
    dialogTotalScore() {
      return this.dialog.form.questions.reduce((sum, q) => sum + Number(q.score || 0), 0)
    }
  },
  created() {
    this.loadCategoryOptions()
    this.loadProfessionOptions()
    this.loadList()
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
    computeTotalScore(row) {
      const qs = row.questions || []
      return qs.reduce((sum, q) => sum + Number(q.score || 0), 0)
    },
    loadCategoryOptions() {
      questionCategories()
        .then((res) => {
          this.categoryOptions = res.data || []
        })
        .catch(() => {
          this.categoryOptions = []
        })
    },
    loadProfessionOptions() {
      professions()
        .then((res) => {
          this.professionOptions = res.data || []
        })
        .catch(() => {
          this.professionOptions = []
        })
    },
    loadList() {
      this.loading = true
      const params = {
        page: this.query.page,
        size: this.query.size,
        name: this.query.name,
        categoryId: this.query.categoryId,
        professionId: this.query.professionId
      }
      questionTemplatePage(params)
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
    onSearch() {
      this.query.page = 1
      this.loadList()
    },
    onReset() {
      this.query = {
        page: 1,
        size: 10,
        name: '',
        categoryId: undefined,
        professionId: undefined
      }
      this.loadList()
    },
    onSizeChange(size) {
      this.query.size = size
      this.query.page = 1
      this.loadList()
    },
    onCurrentChange(page) {
      this.query.page = page
      this.loadList()
    },
    openCreate() {
      this.dialog.isEdit = false
      this.dialog.form = {
        id: undefined,
        name: '',
        description: '',
        categoryId: undefined,
        professionId: undefined,
        questionIds: [],
        questions: []
      }
      this.dialog.visible = true
    },
    openEdit(row) {
      this.dialog.isEdit = true
      this.dialog.loading = true
      this.dialog.visible = true
      questionTemplateDetail(row.id)
        .then((res) => {
          const data = res.data || {}
          const questions = data.questions || []
          this.dialog.form = {
            id: data.id,
            name: data.name || '',
            description: data.description || '',
            categoryId: data.categoryId,
            professionId: data.professionId,
            questionIds: data.questionIds || questions.map((q) => q.id),
            questions: questions
          }
        })
        .catch(() => {
          this.$message.error('获取详情失败')
          this.dialog.visible = false
        })
        .finally(() => {
          this.dialog.loading = false
        })
    },
    onSave() {
      this.$refs.dialogForm.validate((valid) => {
        if (!valid) return
        if (this.dialog.form.questions.length === 0) {
          this.$message.warning('请至少选择一道题目')
          return
        }
        this.dialog.submitting = true
        const payload = {
          id: this.dialog.form.id,
          name: this.dialog.form.name,
          description: this.dialog.form.description,
          categoryId: this.dialog.form.categoryId,
          professionId: this.dialog.form.professionId,
          questionIds: this.dialog.form.questions.map((q) => q.id)
        }
        const action = this.dialog.isEdit
          ? updateQuestionTemplate(payload)
          : addQuestionTemplate(payload)
        action
          .then(() => {
            this.$message.success(this.dialog.isEdit ? '更新成功' : '创建成功')
            this.dialog.visible = false
            this.loadList()
          })
          .finally(() => {
            this.dialog.submitting = false
          })
      })
    },
    onDelete(row) {
      this.$confirm('确定要删除该模板吗?', '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
        .then(() => deleteQuestionTemplate(row.id))
        .then(() => {
          this.$message.success('删除成功')
          if (this.list.length === 1 && this.query.page > 1) this.query.page--
          this.loadList()
        })
        .catch((err) => {
          if (err && err !== 'cancel' && err !== 'close') {
            this.$message.error('删除失败')
          }
        })
    },
    resetDialogForm() {
      this.$refs.dialogForm && this.$refs.dialogForm.clearValidate()
    },
    openQuestionPicker() {
      this.questionPicker.visible = true
      this.questionPicker.query = {
        page: 1,
        size: 10,
        keyword: '',
        type: undefined,
        categoryId: undefined
      }
      // 以当前已选题目初始化勾选状态
      this.questionPicker.selected = this.dialog.form.questions.map((q) => ({ ...q }))
      // 先清空列表，避免 clearSelection 触发的 selection-change 基于旧列表误删已选项
      this.questionPicker.list = []
      this.$nextTick(() => {
        this.$refs.pickerTable && this.$refs.pickerTable.clearSelection()
        this.loadQuestions()
      })
    },
    loadQuestions() {
      this.questionPicker.loading = true
      questionPage(this.questionPicker.query)
        .then((res) => {
          const data = res.data || {}
          this.questionPicker.list = data.records || data.list || data.rows || []
          this.questionPicker.total = data.total || 0
          this.$nextTick(() => {
            this.questionPicker.list.forEach((q) => {
              if (this.questionPicker.selected.find((s) => s.id === q.id)) {
                this.$refs.pickerTable && this.$refs.pickerTable.toggleRowSelection(q, true)
              }
            })
          })
        })
        .catch(() => {
          this.questionPicker.list = []
          this.questionPicker.total = 0
        })
        .finally(() => {
          this.questionPicker.loading = false
        })
    },
    onPickerSelectionChange(rows) {
      // 跨页选择：保留不在当前页的已选题目，再合并当前页的勾选
      const currentPageIds = this.questionPicker.list.map((q) => q.id)
      const kept = this.questionPicker.selected.filter(
        (q) => !currentPageIds.includes(q.id)
      )
      const merged = kept.slice()
      rows.forEach((r) => {
        if (!merged.find((q) => q.id === r.id)) {
          merged.push(r)
        }
      })
      this.questionPicker.selected = merged
    },
    onPickerSearch() {
      this.questionPicker.query.page = 1
      this.loadQuestions()
    },
    onPickerReset() {
      this.questionPicker.query = {
        page: 1,
        size: 10,
        keyword: '',
        type: undefined,
        categoryId: undefined
      }
      this.loadQuestions()
    },
    onPickerPageChange(page) {
      this.questionPicker.query.page = page
      this.loadQuestions()
    },
    onPickerSizeChange(size) {
      this.questionPicker.query.size = size
      this.questionPicker.query.page = 1
      this.loadQuestions()
    },
    confirmPickQuestions() {
      this.dialog.form.questions = this.questionPicker.selected.map((q) => ({ ...q }))
      this.dialog.form.questionIds = this.questionPicker.selected.map((q) => q.id)
      this.questionPicker.visible = false
      this.$refs.pickerTable && this.$refs.pickerTable.clearSelection()
      this.questionPicker.selected = []
    },
    removeQuestion(row) {
      this.dialog.form.questions = this.dialog.form.questions.filter((q) => q.id !== row.id)
      this.dialog.form.questionIds = this.dialog.form.questions.map((q) => q.id)
    },
    clearQuestions() {
      if (!this.dialog.form.questions.length) return
      this.$confirm('确定要清空所有已选题目吗?', '提示', { type: 'warning' })
        .then(() => {
          this.dialog.form.questions = []
          this.dialog.form.questionIds = []
        })
        .catch(() => {})
    },
    openView(row) {
      this.viewDialog.name = row.name || ''
      this.viewDialog.visible = true
      this.viewDialog.loading = true
      this.viewDialog.list = []
      questionTemplateDetail(row.id)
        .then((res) => {
          const data = res.data || {}
          this.viewDialog.list = data.questions || []
        })
        .catch(() => {
          this.$message.error('获取详情失败')
          this.viewDialog.list = []
        })
        .finally(() => {
          this.viewDialog.loading = false
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.danger-text {
  color: #f56c6c;
}
</style>
