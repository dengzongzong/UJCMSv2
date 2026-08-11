<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="isEdit ? '编辑题目' : '新增题目'" class="page-header" />

    <el-card shadow="never" v-loading="loading">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="题型" prop="type">
              <el-select v-model="form.type" style="width: 100%" @change="handleTypeChange">
                <el-option label="单选题" :value="1" />
                <el-option label="多选题" :value="2" />
                <el-option label="填空题" :value="3" />
                <el-option label="判断题" :value="4" />
                <el-option label="简答题" :value="5" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="题目分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" filterable style="width: 100%">
                <el-option
                  v-for="item in categories"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分值" prop="score">
              <el-input-number v-model="form.score" :min="0" :max="100" :precision="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="所属专业" prop="professionId">
              <el-select v-model="form.professionId" placeholder="请选择专业" clearable filterable style="width: 100%">
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

        <el-form-item label="题干" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="请输入题干内容(支持纯文本)"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <!-- 单选/多选：选项管理 -->
        <el-form-item label="选项" required v-if="form.type === 1 || form.type === 2">
          <div class="options-wrapper">
            <div
              v-for="(option, index) in form.options"
              :key="index"
              class="option-item"
            >
              <el-checkbox
                v-if="form.type === 2"
                v-model="option.correct"
                class="option-correct"
              >
                正确
              </el-checkbox>
              <el-radio
                v-else
                v-model="singleCorrectIndex"
                :label="index"
                class="option-correct"
              >
                正确
              </el-radio>
              <el-input
                v-model="option.label"
                placeholder="如 A"
                style="width: 80px; margin-right: 10px"
              />
              <el-input
                v-model="option.content"
                placeholder="请输入选项内容"
                style="flex: 1"
              />
              <el-button
                type="text"
                icon="el-icon-arrow-up"
                :disabled="index === 0"
                @click="moveOption(index, -1)"
              />
              <el-button
                type="text"
                icon="el-icon-arrow-down"
                :disabled="index === form.options.length - 1"
                @click="moveOption(index, 1)"
              />
              <el-button
                type="text"
                icon="el-icon-delete"
                class="danger-text"
                :disabled="form.options.length <= 2"
                @click="removeOption(index)"
              />
            </div>
          </div>
          <el-button type="primary" plain icon="el-icon-plus" size="small" @click="addOption">
            添加选项
          </el-button>
          <div class="form-tip">
            <i class="el-icon-info"></i>
            {{ form.type === 1 ? '单选题请勾选一个正确选项' : '多选题请勾选所有正确选项' }}
          </div>
        </el-form-item>

        <!-- 填空题：正确答案 -->
        <el-form-item label="正确答案" required v-if="form.type === 3">
          <el-input
            v-model="form.correctAnswer"
            placeholder="请输入正确答案，多个答案用英文逗号分隔"
            maxlength="500"
            show-word-limit
          />
          <div class="form-tip">
            <i class="el-icon-info"></i>
            多个答案用英文逗号 , 分隔，例如：答案1,答案2
          </div>
        </el-form-item>

        <!-- 判断题：固定选项 + 单选 -->
        <el-form-item label="正确答案" required v-if="form.type === 4">
          <el-radio-group v-model="judgeCorrectIndex">
            <el-radio :label="0">A. 正确</el-radio>
            <el-radio :label="1">B. 错误</el-radio>
          </el-radio-group>
          <div class="form-tip">
            <i class="el-icon-info"></i>
            判断题固定为 A.正确 / B.错误 两个选项，请选择正确答案
          </div>
        </el-form-item>

        <!-- 简答题：参考答案（存入 analysis 字段） -->
        <el-form-item label="参考答案" required v-if="form.type === 5">
          <el-input
            v-model="form.analysis"
            type="textarea"
            :rows="4"
            placeholder="请输入参考答案"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <!-- 解析（简答题不显示，其参考答案已存入 analysis） -->
        <el-form-item label="解析" prop="analysis" v-if="form.type !== 5">
          <el-input
            v-model="form.analysis"
            type="textarea"
            :rows="4"
            placeholder="请输入答案解析"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="是否可用" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio :label="1">可用</el-radio>
            <el-radio :label="0">不可用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitForm">保 存</el-button>
          <el-button @click="goBack">取 消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 题干重复对比弹窗 -->
    <el-dialog
      title="题干重复提醒"
      :visible.sync="duplicateDialog.visible"
      width="720px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      >
        <span slot="title">
          检测到题干与库中已有 {{ duplicateDialog.list.length }} 条题目重复，请对比确认：
          若内容确实不同可"仍要保存"；若为重复题目建议"编辑已有"。
        </span>
      </el-alert>
      <div class="dup-compare">
        <!-- 当前正在编辑的题干 -->
        <div class="dup-block dup-current">
          <div class="dup-block-title">
            <el-tag type="warning" size="mini">当前题干</el-tag>
          </div>
          <div class="dup-content">{{ form.content }}</div>
        </div>
        <!-- 库中已有题目 -->
        <div
          v-for="(q, idx) in duplicateDialog.list"
          :key="q.id"
          class="dup-block"
        >
          <div class="dup-block-title">
            <el-tag type="info" size="mini">库中已有 #{{ q.id }}</el-tag>
            <span class="dup-meta">{{ typeText(q.type) }} · {{ q.professionName || '通用' }} · {{ q.score }}分</span>
            <el-button type="text" size="mini" @click="goEditDuplicate(q.id)">编辑已有</el-button>
          </div>
          <div class="dup-content">{{ q.content }}</div>
          <div v-if="q.options && q.options.length" class="dup-options">
            <span v-for="opt in q.options" :key="opt.id" :class="{ 'opt-correct': opt.isCorrect }">
              {{ opt.label }}. {{ opt.content }}
            </span>
          </div>
          <div v-if="q.correctAnswer" class="dup-answer">正确答案：{{ q.correctAnswer }}</div>
          <div v-if="q.analysis" class="dup-analysis">解析：{{ q.analysis }}</div>
        </div>
      </div>
      <div slot="footer">
        <el-button @click="duplicateDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmForceSave">仍要保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { questionDetail, addQuestion, updateQuestion, checkQuestionDuplicate } from '@/api/question'
import { questionCategories, professions } from '@/api/setting'

const LETTERS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J']

export default {
  name: 'QuestionEdit',
  data() {
    return {
      loading: false,
      submitting: false,
      isEdit: false,
      categories: [],
      professionOptions: [],
      singleCorrectIndex: 0,
      judgeCorrectIndex: 0,
      // 题干重复对比弹窗
      duplicateDialog: {
        visible: false,
        list: []
      },
      // 待提交的 payload（查重通过后或强制保存时使用）
      pendingPayload: null,
      form: {
        id: undefined,
        type: 1,
        categoryId: undefined,
        professionId: undefined,
        score: 5,
        content: '',
        options: [
          { label: 'A', content: '', correct: true },
          { label: 'B', content: '', correct: false },
          { label: 'C', content: '', correct: false },
          { label: 'D', content: '', correct: false }
        ],
        correctAnswer: '',
        analysis: '',
        enabled: 1
      },
      rules: {
        type: [{ required: true, message: '请选择题型', trigger: 'change' }],
        categoryId: [{ required: true, message: '请选择题目分类', trigger: 'change' }],
        score: [{ required: true, message: '请输入分值', trigger: 'blur' }],
        content: [{ required: true, message: '请输入题干', trigger: 'blur' }],
        enabled: [{ required: true, message: '请选择是否可用', trigger: 'change' }]
      }
    }
  },
  created() {
    this.fetchCategories()
    this.fetchProfessions()
    const id = this.$route.params.id
    if (id) {
      this.isEdit = true
      this.fetchDetail(id)
    }
  },
  methods: {
    fetchCategories() {
      questionCategories()
        .then((res) => {
          this.categories = res.data || []
        })
        .catch(() => {
          this.categories = []
        })
    },
    fetchProfessions() {
      professions()
        .then((res) => {
          this.professionOptions = res.data || []
        })
        .catch(() => {
          this.professionOptions = []
        })
    },
    fetchDetail(id) {
      this.loading = true
      questionDetail(id)
        .then((res) => {
          const data = res.data || {}
          let options = data.options || []
          if (typeof options === 'string') {
            try {
              options = JSON.parse(options)
            } catch (e) {
              options = []
            }
          }
          if (!Array.isArray(options)) options = []
          this.form = {
            id: data.id,
            type: data.type || 1,
            categoryId: data.categoryId,
            professionId: data.professionId,
            score: data.score || 5,
            content: data.content || '',
            options: options.map((o) => ({
              label: o.label || '',
              content: o.content || o.text || '',
              correct: !!o.isCorrect
            })),
            correctAnswer: data.correctAnswer || '',
            analysis: data.analysis || '',
            enabled: data.enabled === undefined ? 1 : data.enabled
          }
          if (this.form.type === 4) {
            // 判断题：根据选项正确性初始化单选
            const idx = this.form.options.findIndex((o) => o.correct)
            this.judgeCorrectIndex = idx >= 0 ? idx : 0
          } else if (this.form.type === 1) {
            const idx = this.form.options.findIndex((o) => o.correct)
            this.singleCorrectIndex = idx >= 0 ? idx : 0
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleTypeChange() {
      if (this.form.type === 4) {
        // 判断题：固定 A.正确 / B.错误，默认选 A
        this.judgeCorrectIndex = 0
      } else if (this.form.type === 1) {
        if (this.form.options.length < 2) {
          this.resetOptions()
        }
        this.singleCorrectIndex = this.form.options.findIndex((o) => o.correct)
        if (this.singleCorrectIndex < 0) this.singleCorrectIndex = 0
        this.form.options.forEach((o, i) => {
          o.correct = i === this.singleCorrectIndex
        })
      } else if (this.form.type === 2) {
        if (this.form.options.length < 2) {
          this.resetOptions()
        }
      }
      // 填空题(3)和简答题(5)不需要选项
    },
    resetOptions() {
      this.form.options = [
        { label: 'A', content: '', correct: true },
        { label: 'B', content: '', correct: false },
        { label: 'C', content: '', correct: false },
        { label: 'D', content: '', correct: false }
      ]
      this.singleCorrectIndex = 0
    },
    addOption() {
      const index = this.form.options.length
      this.form.options.push({
        label: LETTERS[index] || String(index + 1),
        content: '',
        correct: false
      })
    },
    removeOption(index) {
      this.form.options.splice(index, 1)
      this.form.options.forEach((o, i) => {
        o.label = LETTERS[i] || String(i + 1)
      })
      if (this.form.type === 1 && this.singleCorrectIndex === index) {
        this.singleCorrectIndex = 0
      }
    },
    moveOption(index, direction) {
      const target = index + direction
      if (target < 0 || target >= this.form.options.length) return
      const arr = this.form.options
      const tmp = arr[index]
      this.$set(arr, index, arr[target])
      this.$set(arr, target, tmp)
      arr.forEach((o, i) => {
        o.label = LETTERS[i] || String(i + 1)
      })
      if (this.form.type === 1) {
        const correctIdx = arr.findIndex((o) => o.correct)
        this.singleCorrectIndex = correctIdx >= 0 ? correctIdx : 0
      }
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return
        const type = this.form.type
        let options = []
        if (type === 1 || type === 2) {
          if (this.form.options.length < 2) {
            this.$message.warning('至少需要两个选项')
            return
          }
          const emptyOption = this.form.options.find((o) => !o.content)
          if (emptyOption) {
            this.$message.warning('请填写所有选项内容')
            return
          }
          options = this.form.options.map((o) => ({
            label: o.label,
            content: o.content,
            isCorrect: o.correct ? 1 : 0
          }))
          if (type === 1) {
            options = options.map((o, i) => ({ ...o, isCorrect: i === this.singleCorrectIndex ? 1 : 0 }))
          }
          const hasCorrect = options.some((o) => o.isCorrect === 1)
          if (!hasCorrect) {
            this.$message.warning('请设置正确答案')
            return
          }
        } else if (type === 3) {
          // 填空题
          if (!this.form.correctAnswer || !this.form.correctAnswer.trim()) {
            this.$message.warning('请输入正确答案')
            return
          }
          options = []
        } else if (type === 4) {
          // 判断题：根据单选生成固定选项
          options = [
            { label: 'A', content: '正确', isCorrect: this.judgeCorrectIndex === 0 ? 1 : 0 },
            { label: 'B', content: '错误', isCorrect: this.judgeCorrectIndex === 1 ? 1 : 0 }
          ]
        } else if (type === 5) {
          // 简答题：参考答案存入 analysis
          if (!this.form.analysis || !this.form.analysis.trim()) {
            this.$message.warning('请输入参考答案')
            return
          }
          options = []
        }
        this.submitting = true
        const payload = { ...this.form, options }
        this.pendingPayload = payload
        // 新增/编辑前进行题干重复检测：命中则弹对比框由用户决定是否强制保存
        this.checkDuplicateBeforeSave(payload)
      })
    },
    // 提交前的题干查重（编辑时排除自身 id）
    checkDuplicateBeforeSave(payload) {
      const content = (payload.content || '').trim()
      if (!content) {
        this.doSave(false)
        return
      }
      const params = { content }
      if (payload.categoryId) params.categoryId = payload.categoryId
      if (payload.professionId) params.professionId = payload.professionId
      if (payload.type != null) params.type = payload.type
      if (this.isEdit && payload.id) params.excludeId = payload.id
      checkQuestionDuplicate(params)
        .then((res) => {
          const list = (res && res.data) || []
          if (list.length > 0) {
            this.duplicateDialog.list = list
            this.duplicateDialog.visible = true
            this.submitting = false
          } else {
            this.doSave(false)
          }
        })
        .catch(() => {
          // 查重接口异常时不阻塞保存，降级为直接保存
          this.doSave(false)
        })
    },
    // 对比弹窗点击"仍要保存"：带 force=true 强制保存
    confirmForceSave() {
      this.doSave(true)
    },
    // 跳转到编辑已有重复题目
    goEditDuplicate(id) {
      this.duplicateDialog.visible = false
      this.$router.push(`/question/edit/${id}`).catch(() => {})
    },
    // 实际调用新增/更新接口
    doSave(force) {
      const payload = { ...this.pendingPayload }
      if (force) payload.force = true
      const action = this.isEdit ? updateQuestion(payload) : addQuestion(payload)
      action
        .then(() => {
          this.$message.success(this.isEdit ? '更新成功' : '新增成功')
          this.duplicateDialog.visible = false
          this.$router.push('/question/list').catch(() => {})
        })
        .catch(() => {})
        .finally(() => {
          this.submitting = false
        })
    },
    typeText(type) {
      const map = { 1: '单选题', 2: '多选题', 3: '填空题', 4: '判断题', 5: '简答题' }
      return map[type] || '未知'
    },
    goBack() {
      this.$router.push('/question/list').catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.page-header {
  margin-bottom: 16px;
}

.options-wrapper {
  margin-bottom: 12px;

  .option-item {
    display: flex;
    align-items: center;
    margin-bottom: 10px;

    .option-correct {
      width: 80px;
      margin-right: 10px;
    }
  }
}

.form-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
}

.danger-text {
  color: #f56c6c;
}

/* 题干重复对比弹窗样式 */
.dup-compare {
  max-height: 420px;
  overflow-y: auto;
}
.dup-block {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 10px 12px;
  margin-bottom: 10px;
  background: #fafafa;
}
.dup-block.dup-current {
  border-color: #e6a23c;
  background: #fdf6ec;
}
.dup-block-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.dup-meta {
  font-size: 12px;
  color: #909399;
}
.dup-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  word-break: break-all;
}
.dup-options {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
}
.dup-options span {
  display: inline-block;
  margin-right: 16px;
  padding: 2px 6px;
  border-radius: 3px;
}
.dup-options .opt-correct {
  background: #f0f9eb;
  color: #67c23a;
  font-weight: 600;
}
.dup-answer {
  margin-top: 4px;
  font-size: 13px;
  color: #67c23a;
}
.dup-analysis {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}
</style>
