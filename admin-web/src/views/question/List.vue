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
        <el-select
          v-model="query.type"
          placeholder="题型"
          clearable
          class="filter-item"
          style="width: 130px"
        >
          <el-option label="单选题" :value="1" />
          <el-option label="多选题" :value="2" />
          <el-option label="填空题" :value="3" />
          <el-option label="判断题" :value="4" />
          <el-option label="简答题" :value="5" />
        </el-select>
        <el-select
          v-model="query.categoryId"
          placeholder="题目分类"
          clearable
          filterable
          class="filter-item"
          style="width: 180px"
        >
          <el-option
            v-for="item in categories"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="关键词(题干/解析)"
          clearable
          class="filter-item"
          style="width: 220px"
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
          v-model="query.enabled"
          placeholder="是否可用"
          clearable
          class="filter-item"
          style="width: 130px"
        >
          <el-option label="可用" :value="1" />
          <el-option label="不可用" :value="0" />
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
          新增题目
        </el-button>
        <el-button
          type="warning"
          icon="el-icon-download"
          class="filter-item"
          style="float: right"
          :loading="exporting"
          @click="handleExport"
        >
          导出
        </el-button>
        <el-button
          icon="el-icon-upload2"
          class="filter-item"
          style="float: right"
          @click="importDialog.visible = true"
        >
          导入
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" :fit="false" border stripe style="width: 100%" @selection-change="rows => (selection = rows)">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="题型" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="typeTagType(row.type)" size="mini">
              {{ typeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="120" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" type="info">{{ row.categoryName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="professionName" label="专业" width="140" align="center">
          <template slot-scope="{ row }">{{ row.professionName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="content" label="题干" min-width="220" show-overflow-tooltip />
        <el-table-column label="选项/答案" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <span>{{ formatAnswer(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="analysis" label="解析" min-width="160" show-overflow-tooltip />
        <el-table-column prop="score" label="分值" width="80" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
        <el-table-column label="可用" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'danger'" size="mini">
              {{ row.enabled === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template slot-scope="{ row }">
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

    <!-- 导入弹窗 -->
    <el-dialog title="导入题目" :visible.sync="importDialog.visible" width="480px" append-to-body>
      <el-alert
        title="请上传 Excel 文件，系统将按模板解析题目"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <div style="margin-bottom: 12px">
        <el-button type="text" icon="el-icon-download" @click="handleDownloadTemplate">
          下载导入模板
        </el-button>
      </div>
      <el-upload
        ref="importUpload"
        :http-request="customUpload"
        :show-file-list="true"
        :limit="1"
        :auto-upload="false"
        :before-upload="beforeImportUpload"
        accept=".xlsx,.xls"
        drag
        action="#"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">仅支持 .xlsx / .xls 文件</div>
      </el-upload>
      <div slot="footer">
        <el-button @click="importDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="importDialog.submitting" @click="submitImport">
          开始导入
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  questionPage,
  deleteQuestion,
  exportQuestion,
  importQuestion,
  downloadTemplate,
  batchDeleteQuestions
} from '@/api/question'
import { questionCategories, professions } from '@/api/setting'
import { downloadBlob } from '@/utils'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'QuestionList',
  mixins: [tableMaxHeight],
  data() {
    return {
      loading: false,
      exporting: false,
      list: [],
      total: 0,
      selection: [],
      categories: [],
      professionOptions: [],
      query: {
        page: 1,
        size: 10,
        professionId: undefined,
        type: undefined,
        categoryId: undefined,
        keyword: '',
        enabled: undefined,
        dateRange: []
      },
      importDialog: {
        visible: false,
        submitting: false
      }
    }
  },
  created() {
    this.fetchCategories()
    this.fetchProfessions()
    this.fetchList()
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
    formatOptions(options) {
      if (!options) return '-'
      let arr = options
      if (typeof options === 'string') {
        try {
          arr = JSON.parse(options)
        } catch (e) {
          return options
        }
      }
      if (!Array.isArray(arr)) return '-'
      return arr.map((o) => `${o.label || o.key}. ${o.content || o.text || ''}`).join('  ')
    },
    typeText(type) {
      const map = { 1: '单选题', 2: '多选题', 3: '填空题', 4: '判断题', 5: '简答题' }
      return map[type] || '未知'
    },
    typeTagType(type) {
      const map = { 1: '', 2: 'success', 3: 'warning', 4: 'info', 5: 'danger' }
      return map[type] || ''
    },
    formatAnswer(row) {
      const type = row.type
      if (type === 3) {
        // 填空题：显示正确答案
        return row.correctAnswer || '-'
      }
      if (type === 4) {
        // 判断题：显示 正确/错误
        return '正确/错误'
      }
      if (type === 5) {
        // 简答题：参考答案（内容已在解析列展示）
        return '参考答案'
      }
      // 单选/多选：显示选项
      return this.formatOptions(row.options)
    },
    fetchList() {
      this.loading = true
      const params = {
        page: this.query.page,
        size: this.query.size,
        professionId: this.query.professionId,
        type: this.query.type,
        categoryId: this.query.categoryId,
        keyword: this.query.keyword,
        enabled: this.query.enabled
      }
      if (this.query.dateRange && this.query.dateRange.length === 2) {
        params.startDate = this.query.dateRange[0]
        params.endDate = this.query.dateRange[1]
      }
      questionPage(params)
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
      this.query = {
        page: 1,
        size: 10,
        professionId: undefined,
        type: undefined,
        categoryId: undefined,
        keyword: '',
        enabled: undefined,
        dateRange: []
      }
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
      this.$router.push('/question/edit').catch(() => {})
    },
    handleEdit(row) {
      this.$router.push(`/question/edit/${row.id}`).catch(() => {})
    },
    handleDelete(row) {
      this.$confirm('确定要删除该题目吗?', '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
        .then(() => {
          deleteQuestion(row.id).then(() => {
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
        batchDeleteQuestions(ids).then(() => {
          this.$message.success('删除成功')
          this.selection = []
          this.fetchList()
        }).catch(() => {})
      }).catch(() => {})
    },
    handleExport() {
      this.exporting = true
      const params = {
        type: this.query.type,
        categoryId: this.query.categoryId,
        keyword: this.query.keyword,
        enabled: this.query.enabled,
        professionId: this.query.professionId
      }
      exportQuestion(params)
        .then((response) => {
          downloadBlob(response, '题目列表.xlsx')
          this.$message.success('导出成功')
        })
        .catch(() => {
          this.$message.error('导出失败')
        })
        .finally(() => {
          this.exporting = false
        })
    },
    handleDownloadTemplate() {
      downloadTemplate()
        .then((response) => {
          downloadBlob(response, '题库导入模板.xlsx')
        })
        .catch(() => {
          this.$message.error('模板下载失败，请稍后重试')
        })
    },
    beforeImportUpload(file) {
      const isExcel = /\.(xlsx|xls)$/.test(file.name)
      if (!isExcel) {
        this.$message.error('只能上传 Excel 文件')
        return false
      }
      return true
    },
    submitImport() {
      this.$refs.importUpload.submit()
    },
    customUpload(options) {
      const { file } = options
      const formData = new FormData()
      formData.append('file', file)
      this.importDialog.submitting = true
      importQuestion(formData)
        .then((res) => {
          const data = (res && res.data) || {}
          const successCount = data.successCount || 0
          const failCount = data.failCount || 0
          const duplicateCount = data.duplicateCount || 0
          const failList = data.failList || []
          const duplicateList = data.duplicateList || []
          const hasIssue = failCount > 0 || duplicateCount > 0
          if (hasIssue) {
            // 构建明细 HTML：失败 + 重复（含库中已有题目对比）
            let html = `<div style="margin-bottom:8px;">导入完成：成功 <b style="color:#67c23a">${successCount}</b> 条`
            if (duplicateCount > 0) {
              html += `，题干重复 <b style="color:#e6a23c">${duplicateCount}</b> 条（已跳过，未重复创建）`
            }
            if (failCount > 0) {
              html += `，失败 <b style="color:#f56c6c">${failCount}</b> 条`
            }
            html += '</div>'
            // 失败明细
            if (failList.length > 0) {
              html += '<div style="margin:8px 0 4px;font-weight:600;color:#f56c6c">失败明细：</div>'
              failList.forEach((f) => {
                html += `<div style="font-size:12px;color:#606266;margin:2px 0">第${f.row}行：${this.escapeHtml(f.reason || '导入失败')}</div>`
              })
            }
            // 重复明细（含库中已有题目对比）
            if (duplicateList.length > 0) {
              html += '<div style="margin:10px 0 4px;font-weight:600;color:#e6a23c">题干重复明细（与库中已有题目对比）：</div>'
              duplicateList.forEach((d) => {
                html += `<div style="border:1px solid #ebeef5;border-radius:4px;padding:8px;margin:6px 0;background:#fafafa">`
                html += `<div style="font-size:12px;color:#909399">第${d.row}行 · ${this.escapeHtml(d.reason || '')}</div>`
                html += `<div style="margin:4px 0"><span style="font-size:12px;color:#e6a23c">导入题干：</span><span style="font-size:13px">${this.escapeHtml(d.content || '')}</span></div>`
                const existList = d.existingQuestions || []
                existList.forEach((q) => {
                  html += `<div style="border-left:2px solid #e6a23c;padding-left:8px;margin:4px 0">`
                  html += `<div style="font-size:12px;color:#909399">库中已有 #${q.id} · ${this.typeText(q.type)} · ${this.escapeHtml(q.professionName || '通用')} · ${q.score}分</div>`
                  html += `<div style="font-size:13px;color:#303133">${this.escapeHtml(q.content || '')}</div>`
                  if (q.options && q.options.length) {
                    const opts = q.options.map((o) => {
                      const cls = o.isCorrect ? 'color:#67c23a;font-weight:600' : 'color:#606266'
                      return `<span style="${cls};margin-right:12px">${this.escapeHtml(o.label)}. ${this.escapeHtml(o.content || '')}</span>`
                    }).join('')
                    html += `<div style="font-size:12px;margin-top:2px">${opts}</div>`
                  }
                  if (q.correctAnswer) {
                    html += `<div style="font-size:12px;color:#67c23a;margin-top:2px">正确答案：${this.escapeHtml(q.correctAnswer)}</div>`
                  }
                  html += `</div>`
                })
                html += `</div>`
              })
            }
            this.$alert(html, '导入结果', {
              confirmButtonText: '确定',
              type: 'warning',
              dangerouslyUseHTMLString: true,
              customClass: 'import-result-dialog'
            })
          } else {
            this.$message.success(`导入成功，共 ${successCount} 条`)
          }
          this.importDialog.visible = false
          this.$refs.importUpload.clearFiles()
          this.fetchList()
        })
        .catch(() => {
          this.$message.error('导入失败，请检查文件格式')
        })
        .finally(() => {
          this.importDialog.submitting = false
        })
    },
    escapeHtml(str) {
      if (str == null) return ''
      return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
    }
  }
}
</script>

<style lang="scss" scoped>
.danger-text {
  color: #f56c6c;
}
</style>
