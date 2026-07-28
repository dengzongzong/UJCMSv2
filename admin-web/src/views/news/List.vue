<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="filter-container">
        <el-input
          v-model="query.title"
          placeholder="标题"
          clearable
          class="filter-item"
          style="width: 180px"
          @keyup.enter.native="handleSearch"
        />
        <el-select
          v-model="query.type"
          placeholder="类型"
          clearable
          class="filter-item"
          style="width: 130px"
          @change="handleSearch"
        >
          <el-option label="新闻动态" :value="1" />
          <el-option label="重大活动" :value="2" />
          <el-option label="报考条件" :value="3" />
          <el-option label="热门工种" :value="4" />
          <el-option label="行业信息" :value="5" />
          <el-option label="校企合作" :value="6" />
          <el-option label="通知公告" :value="7" />
          <el-option label="政策法规" :value="8" />
          <el-option label="信息公开" :value="9" />
        </el-select>
        <el-select
          v-model="query.status"
          placeholder="状态"
          clearable
          class="filter-item"
          style="width: 100px"
        >
          <el-option label="显示" :value="1" />
          <el-option label="隐藏" :value="0" />
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
          新增新闻
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" :fit="false" border stripe style="width: 100%" @selection-change="rows => (selection = rows)">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="封面图" width="90" align="center">
          <template slot-scope="{ row }">
            <el-image
              v-if="row.coverUrl"
              :src="resolveUrl(row.coverUrl)"
              :preview-src-list="[resolveUrl(row.coverUrl)]"
              fit="cover"
              style="width: 50px; height: 50px; border-radius: 4px"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="newsTagType(row.type)" size="mini">
              {{ newsTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <span>{{ row.content ? stripHtml(row.content).substring(0, 80) : '-' }}{{ row.content && stripHtml(row.content).length > 80 ? '...' : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.isTop === 1 ? 'danger' : 'info'" size="mini">
              {{ row.isTop === 1 ? '置顶' : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="180" align="center">
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="editDialog.isEdit ? '编辑新闻' : '新增新闻'"
      :visible.sync="editDialog.visible"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form ref="editForm" :model="editDialog.form" :rules="editDialog.rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="editDialog.form.title" placeholder="请输入标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="封面图" prop="coverUrl">
          <el-input v-model="editDialog.form.coverUrl" placeholder="请输入封面图URL" maxlength="500" clearable />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <RichEditor v-model="editDialog.form.content" height="350px" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="类型" prop="type">
              <el-select v-model="editDialog.form.type" style="width: 100%">
                <el-option label="新闻动态" :value="1" />
                <el-option label="重大活动" :value="2" />
                <el-option label="报考条件" :value="3" />
                <el-option label="热门工种" :value="4" />
                <el-option label="行业信息" :value="5" />
                <el-option label="校企合作" :value="6" />
                <el-option label="通知公告" :value="7" />
                <el-option label="政策法规" :value="8" />
                <el-option label="信息公开" :value="9" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="editDialog.form.status">
                <el-radio :label="1">显示</el-radio>
                <el-radio :label="0">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="置顶" prop="isTop">
              <el-switch v-model="editDialog.form.isTop" :active-value="1" :inactive-value="0" active-text="置顶" inactive-text="普通" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="editDialog.form.sort" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="发布时间">
          <el-date-picker
            v-model="editDialog.form.publishTime"
            type="datetime"
            placeholder="选择发布时间(留空=立即发布)"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 100%"
          />
          <div style="font-size:12px;color:#909399;line-height:1.5;margin-top:4px">留空表示立即发布，选择时间后到时间才会显示</div>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="editDialog.form.createTime"
            type="datetime"
            placeholder="选择创建时间(留空=自动取当前时间)"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 100%"
          />
          <div style="font-size:12px;color:#909399;line-height:1.5;margin-top:4px">可手动修改创建时间，留空则自动取当前时间</div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="editDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="editDialog.submitting" @click="submitEdit">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { newsPage, addNews, updateNews, deleteNews, batchDeleteNews } from '@/api/news'
import { apiUrl } from '@/utils/apiBase'
import RichEditor from '@/components/RichEditor'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'NewsList',
  mixins: [tableMaxHeight],
  components: { RichEditor },
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      selection: [],
      query: {
        page: 1,
        size: 10,
        title: '',
        type: undefined,
        status: undefined
      },
      editDialog: {
        visible: false,
        isEdit: false,
        submitting: false,
        form: {
          id: undefined,
          title: '',
          coverUrl: '',
          content: '',
          type: 1,
          status: 1,
          sort: 0,
          isTop: 0,
          publishTime: undefined,
          createTime: undefined
        },
        rules: {
          title: [{ required: true, message: '请输入新闻标题', trigger: 'blur' }],
          content: [{ required: true, message: '请输入新闻内容', trigger: 'blur' }]
        }
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    newsTypeName(type) {
      const map = { 1: '新闻动态', 2: '重大活动', 3: '报考条件', 4: '热门工种', 5: '行业信息', 6: '校企合作', 7: '通知公告', 8: '政策法规', 9: '信息公开' }
      return map[type] || '新闻动态'
    },
    newsTagType(type) {
      const map = { 1: '', 2: 'danger', 3: 'warning', 4: 'success', 5: 'info', 6: 'warning', 7: 'info', 8: 'warning', 9: 'success' }
      return map[type] || ''
    },
    fetchList() {
      this.loading = true
      const params = {
        page: this.query.page,
        size: this.query.size,
        title: this.query.title,
        type: this.query.type,
        status: this.query.status
      }
      newsPage(params)
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
    // news 的封面图是用户手动输入 URL, 兼容:
    //   1) 完整 http:// 开头 -> 原样返回
    //   2) /uploads/xxx 开头 -> 通过 apiUrl 拼 baseURL(/api)
    resolveUrl(u) {
      if (!u) return ''
      if (/^https?:\/\//.test(u)) return u
      return apiUrl(u)
    },
    stripHtml(html) {
      if (!html) return ''
      const temp = document.createElement('div')
      temp.innerHTML = html
      return temp.textContent || temp.innerText || ''
    },
    handleSearch() {
      this.query.page = 1
      this.fetchList()
    },
    handleReset() {
      this.query = { page: 1, size: 10, title: '', type: undefined, status: undefined }
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
      this.editDialog.isEdit = false
      this.editDialog.form = { id: undefined, title: '', coverUrl: '', content: '', type: 1, status: 1, sort: 0, isTop: 0, publishTime: undefined, createTime: undefined }
      this.editDialog.visible = true
      this.$nextTick(() => {
        this.$refs.editForm && this.$refs.editForm.clearValidate()
      })
    },
    handleEdit(row) {
      this.editDialog.isEdit = true
      this.editDialog.form = { ...row }
      this.editDialog.form.publishTime = row.publishTime
      this.editDialog.form.isTop = row.isTop || 0
      this.editDialog.visible = true
      this.$nextTick(() => {
        this.$refs.editForm && this.$refs.editForm.clearValidate()
      })
    },
    submitEdit() {
      this.$refs.editForm.validate((valid) => {
        if (!valid) return
        this.editDialog.submitting = true
        const api = this.editDialog.isEdit ? updateNews : addNews
        api(this.editDialog.form)
          .then(() => {
            this.$message.success(this.editDialog.isEdit ? '修改成功' : '新增成功')
            this.editDialog.visible = false
            this.fetchList()
          })
          .catch((err) => {
            this.$message.error((err && err.message) || '操作失败')
          })
          .finally(() => {
            this.editDialog.submitting = false
          })
      })
    },
    handleDelete(row) {
      this.$confirm(`确定要删除新闻 "${row.title}" 吗?`, '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
        .then(() => {
          deleteNews(row.id)
            .then(() => {
              this.$message.success('删除成功')
              if (this.list.length === 1 && this.query.page > 1) this.query.page--
              this.fetchList()
            })
            .catch(() => {
              this.$message.error('删除失败')
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
        batchDeleteNews(ids).then(() => {
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
