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
          placeholder="试卷名称"
          clearable
          class="filter-item"
          style="width: 220px"
          @keyup.enter.native="handleSearch"
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
          新增试卷
        </el-button>
        <el-button
          type="primary"
          icon="el-icon-magic-stick"
          class="filter-item"
          style="float: right"
          @click="openGenerateDialog"
        >
          一键组卷
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" :max-height="tableMaxHeight" :fit="false" border stripe style="width: 100%" @selection-change="rows => (selection = rows)">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="试卷名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="questionCount" label="题目数量" width="100" align="center">
          <template slot-scope="{ row }">{{ row.questionCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="90" align="center">
          <template slot-scope="{ row }">{{ row.totalScore || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '已发布' : '未发布' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
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
    <el-dialog title="试卷详情" :visible.sync="detailDialog.visible" width="760px">
      <div v-loading="detailDialog.loading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="试卷名称">{{ detailDialog.data.name }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ detailDialog.data.totalScore }}</el-descriptions-item>
          <el-descriptions-item label="题目数量">{{ detailDialog.data.questionCount }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detailDialog.data.status === 1 ? '已发布' : '未发布' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailDialog.data.createTime }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detailDialog.data.description || detailDialog.data.intro || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 16px 0 8px">题目列表</h4>
        <el-table :data="detailDialog.data.questions || detailDialog.data.questionList || []" border size="mini">
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
        </el-table>
      </div>
      <div slot="footer">
        <el-button @click="detailDialog.visible = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 一键组卷弹窗 -->
    <el-dialog
      title="一键抽题组卷"
      :visible.sync="generateDialog.visible"
      width="640px"
      :close-on-click-modal="false"
      @closed="onGenerateDialogClosed"
    >
      <el-form ref="generateForm" :model="generateDialog.form" :rules="generateDialog.rules" label-width="100px">
        <el-form-item label="试卷名称" prop="name">
          <el-input v-model="generateDialog.form.name" placeholder="请输入试卷名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="试卷描述">
          <el-input
            v-model="generateDialog.form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入试卷描述(选填)"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="generateDialog.form.status" style="width: 100%">
                <el-option label="未发布" :value="0" />
                <el-option label="已发布" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业">
              <el-select
                v-model="generateDialog.form.professionId"
                placeholder="选填"
                filterable
                clearable
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
        </el-row>
        <el-form-item label="题目分类">
          <el-select
            v-model="generateDialog.form.categoryId"
            placeholder="选填"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in categories"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-divider content-position="left">
          <span>组卷模式</span>
          <el-radio-group v-model="generateDialog.generateMode" style="margin-left: 12px" size="small" @change="onGenerateModeChange">
            <el-radio-button label="byCount">按数量抽题</el-radio-button>
            <el-radio-button label="byScore">按总分组卷</el-radio-button>
          </el-radio-group>
        </el-divider>
        <!-- 按总分组卷 -->
        <template v-if="generateDialog.generateMode === 'byScore'">
          <el-form-item label="目标总分" prop="targetScore">
            <el-input-number v-model="generateDialog.form.targetScore" :min="1" :max="9999" controls-position="right" style="width: 100%" placeholder="请输入目标总分" />
            <div style="font-size: 12px; color: #909399; margin-top: 4px">系统将从题库中自动选取题目组合，使总分恰好等于此值</div>
          </el-form-item>
        </template>
        <!-- 按数量抽题(原有) -->
        <template v-else>
          <el-divider content-position="left">
            <span>抽题数量</span>
            <el-tooltip content="开启后，将选取题库中所有可用题目组卷，不再按数量随机抽取" placement="top">
              <el-switch
                v-model="generateDialog.selectAll"
                active-text="全选题库所有题目"
                style="margin-left: 12px"
                @change="onSelectAllChange"
              />
            </el-tooltip>
          </el-divider>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="单选题" label-width="80px">
                <el-input-number v-model="generateDialog.form.singleCount" :min="generateDialog.selectAll ? -1 : 0" :max="500" :disabled="generateDialog.selectAll" controls-position="right" style="width: 100%" />
                <div v-if="generateDialog.selectAll" class="all-tip">全部</div>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="多选题" label-width="80px">
                <el-input-number v-model="generateDialog.form.multiCount" :min="generateDialog.selectAll ? -1 : 0" :max="500" :disabled="generateDialog.selectAll" controls-position="right" style="width: 100%" />
                <div v-if="generateDialog.selectAll" class="all-tip">全部</div>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="填空题" label-width="80px">
                <el-input-number v-model="generateDialog.form.fillCount" :min="generateDialog.selectAll ? -1 : 0" :max="500" :disabled="generateDialog.selectAll" controls-position="right" style="width: 100%" />
                <div v-if="generateDialog.selectAll" class="all-tip">全部</div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="判断题" label-width="80px">
                <el-input-number v-model="generateDialog.form.judgeCount" :min="generateDialog.selectAll ? -1 : 0" :max="500" :disabled="generateDialog.selectAll" controls-position="right" style="width: 100%" />
                <div v-if="generateDialog.selectAll" class="all-tip">全部</div>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="简答题" label-width="80px">
                <el-input-number v-model="generateDialog.form.shortCount" :min="generateDialog.selectAll ? -1 : 0" :max="500" :disabled="generateDialog.selectAll" controls-position="right" style="width: 100%" />
                <div v-if="generateDialog.selectAll" class="all-tip">全部</div>
              </el-form-item>
            </el-col>
          </el-row>
        </template>
      </el-form>
      <div slot="footer">
        <el-button @click="generateDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="generateDialog.submitting" @click="submitGenerate">
          开始组卷
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { paperPage, paperDetail, deletePaper, autoGeneratePaper, batchDeletePapers } from '@/api/paper'
import { professions, questionCategories } from '@/api/setting'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'PaperList',
  mixins: [tableMaxHeight],
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      selection: [],
      professionOptions: [],
      categories: [],
      query: {
        page: 1,
        size: 10,
        professionId: undefined,
        name: '',
        status: undefined
      },
      detailDialog: {
        visible: false,
        loading: false,
        data: {}
      },
      generateDialog: {
        visible: false,
        submitting: false,
        generateMode: 'byCount',
        selectAll: false,
        form: {
          name: '',
          description: '',
          status: 0,
          professionId: undefined,
          categoryId: undefined,
          targetScore: 100,
          singleCount: 0,
          multiCount: 0,
          fillCount: 0,
          judgeCount: 0,
          shortCount: 0
        },
        rules: {
          name: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
          status: [{ required: true, message: '请选择状态', trigger: 'change' }],
          targetScore: [{ required: true, message: '请输入目标总分', trigger: 'blur' }]
        }
      }
    }
  },
  created() {
    this.fetchProfessions()
    this.fetchCategories()
    this.fetchList()
  },
  methods: {
    fetchProfessions() {
      professions()
        .then((res) => {
          this.professionOptions = res.data || []
        })
        .catch(() => {
          this.professionOptions = []
        })
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
        status: this.query.status
      }
      paperPage(params)
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
      this.query = { page: 1, size: 10, professionId: undefined, name: '', status: undefined }
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
      this.$router.push('/paper/edit').catch(() => {})
    },
    handleEdit(row) {
      this.$router.push(`/paper/edit/${row.id}`).catch(() => {})
    },
    handleDetail(row) {
      this.detailDialog.visible = true
      this.detailDialog.loading = true
      this.detailDialog.data = {}
      paperDetail(row.id)
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
    handleDelete(row) {
      this.$confirm(`确定要删除试卷 "${row.name}" 吗?`, '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
        .then(() => {
          deletePaper(row.id).then(() => {
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
        batchDeletePapers(ids).then(() => {
          this.$message.success('删除成功')
          this.selection = []
          this.fetchList()
        }).catch(() => {})
      }).catch(() => {})
    },
    resetGenerateForm() {
      this.generateDialog.selectAll = false
      this.generateDialog.generateMode = 'byCount'
      this.generateDialog.form = {
        name: '',
        description: '',
        status: 0,
        professionId: undefined,
        categoryId: undefined,
        targetScore: 100,
        singleCount: 0,
        multiCount: 0,
        fillCount: 0,
        judgeCount: 0,
        shortCount: 0
      }
    },
    onGenerateModeChange() {
      // 切换模式时清除验证提示
      this.$refs.generateForm && this.$refs.generateForm.clearValidate()
    },
    // 全选开关切换：开启时所有题型至少选一道（置为 -1 提交全选语义），关闭时恢复为 0
    onSelectAllChange(val) {
      const fields = ['singleCount', 'multiCount', 'fillCount', 'judgeCount', 'shortCount']
      if (val) {
        fields.forEach((k) => { this.generateDialog.form[k] = -1 })
      } else {
        fields.forEach((k) => { this.generateDialog.form[k] = 0 })
      }
    },
    openGenerateDialog() {
      this.resetGenerateForm()
      this.generateDialog.visible = true
      this.$nextTick(() => {
        this.$refs.generateForm && this.$refs.generateForm.clearValidate()
      })
    },
    onGenerateDialogClosed() {
      this.resetGenerateForm()
      this.$refs.generateForm && this.$refs.generateForm.clearValidate()
    },
    submitGenerate() {
      this.$refs.generateForm.validate((valid) => {
        if (!valid) return
        const f = this.generateDialog.form
        const mode = this.generateDialog.generateMode

        if (mode === 'byScore') {
          if (!f.targetScore || f.targetScore <= 0) {
            this.$message.warning('请输入目标总分')
            return
          }
        } else {
          const allMode = this.generateDialog.selectAll
          if (!allMode) {
            const total =
              (f.singleCount || 0) +
              (f.multiCount || 0) +
              (f.fillCount || 0) +
              (f.judgeCount || 0) +
              (f.shortCount || 0)
            if (total <= 0) {
              this.$message.warning('请至少设置一种题型的抽题数量(大于0)')
              return
            }
          }
        }

        this.generateDialog.submitting = true
        const allMode = this.generateDialog.selectAll
        const data = {
          name: f.name,
          description: f.description,
          status: f.status,
          professionId: f.professionId,
          categoryId: f.categoryId,
          generateMode: mode,
          targetScore: mode === 'byScore' ? f.targetScore : null,
          singleCount: allMode ? -1 : (f.singleCount || 0),
          multiCount: allMode ? -1 : (f.multiCount || 0),
          fillCount: allMode ? -1 : (f.fillCount || 0),
          judgeCount: allMode ? -1 : (f.judgeCount || 0),
          shortCount: allMode ? -1 : (f.shortCount || 0)
        }
        autoGeneratePaper(data)
          .then((res) => {
            const resData = (res && res.data) || res || {}
            const id = resData.id || resData.paperId || null
            this.$message.success('组卷成功')
            this.generateDialog.visible = false
            this.fetchList()
            if (id) {
              this.$router.push('/paper/edit/' + id).catch(() => {})
            }
          })
          .catch((err) => {
            // 后端会返回具体错误(如某题型题库无可用题目)，优先展示后端错误信息
            const msg = (err && (err.message || err.msg)) || '组卷失败，请稍后重试'
            this.$message.error(msg)
          })
          .finally(() => {
            this.generateDialog.submitting = false
          })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.danger-text {
  color: #f56c6c;
}
/* 全选题库模式下的"全部"提示 */
.all-tip {
  font-size: 12px;
  color: #67c23a;
  text-align: center;
  margin-top: 2px;
  font-weight: 600;
}
</style>
