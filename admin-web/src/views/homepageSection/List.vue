<template>
  <div class="app-container">
    <el-card>
      <!-- 查询栏 -->
      <div class="filter-container">
        <el-input v-model="query.title" placeholder="标题" clearable style="width: 200px" @keyup.enter.native="handleSearch" />
        <el-select v-model="query.type" placeholder="类型" clearable style="width: 140px" @change="handleSearch">
          <el-option label="政策法规" :value="1" />
          <el-option label="信息公开" :value="2" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="handleSearch">
          <el-option label="显示" :value="1" />
          <el-option label="隐藏" :value="0" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增</el-button>
        <el-button type="danger" icon="el-icon-delete" :disabled="selection.length === 0" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" border stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.type === 1 ? 'danger' : 'warning'" size="mini">
              {{ row.type === 1 ? '政策法规' : '信息公开' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" style="color: #f56c6c" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          :current-page="query.page"
          :page-sizes="[10, 20, 50]"
          :page-size="query.size"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="editDialog.isEdit ? '编辑内容' : '新增内容'" :visible.sync="editDialog.visible" width="70%" :close-on-click-modal="false">
      <el-form ref="editForm" :model="editDialog.form" :rules="editDialog.rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="editDialog.form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="类型" prop="type">
              <el-select v-model="editDialog.form.type" style="width: 100%">
                <el-option label="政策法规" :value="1" />
                <el-option label="信息公开" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-select v-model="editDialog.form.status" style="width: 100%">
                <el-option label="显示" :value="1" />
                <el-option label="隐藏" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="editDialog.form.sort" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="内容" prop="content">
          <RichEditor v-model="editDialog.form.content" :height="380" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="editDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="editDialog.submitting" @click="submitEdit">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { homepageSectionPage, addHomepageSection, updateHomepageSection, deleteHomepageSection, batchDeleteHomepageSection } from '@/api/homepageSection'
import RichEditor from '@/components/RichEditor'

export default {
  name: 'HomepageSectionList',
  components: { RichEditor },
  data() {
    return {
      list: [],
      total: 0,
      loading: false,
      selection: [],
      query: { page: 1, size: 10, title: '', type: undefined, status: undefined },
      editDialog: {
        visible: false,
        isEdit: false,
        submitting: false,
        form: { title: '', content: '', type: 1, status: 1, sort: 0 },
        rules: {
          title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
          type: [{ required: true, message: '请选择类型', trigger: 'change' }],
          status: [{ required: true, message: '请选择状态', trigger: 'change' }]
        }
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        const res = await homepageSectionPage(this.query)
        const d = res.data || res
        this.list = d.records || d.list || []
        this.total = d.total || 0
      } catch (e) {
        this.list = []
        this.total = 0
      } finally {
        this.loading = false
      }
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
      this.fetchList()
    },
    handleCurrentChange(page) {
      this.query.page = page
      this.fetchList()
    },
    onSelectionChange(rows) {
      this.selection = rows
    },
    handleAdd() {
      this.editDialog.isEdit = false
      this.editDialog.form = { title: '', content: '', type: 1, status: 1, sort: 0 }
      this.editDialog.visible = true
      this.$nextTick(() => {
        this.$refs.editForm && this.$refs.editForm.clearValidate()
      })
    },
    handleEdit(row) {
      this.editDialog.isEdit = true
      this.editDialog.form = { ...row }
      this.editDialog.visible = true
      this.$nextTick(() => {
        this.$refs.editForm && this.$refs.editForm.clearValidate()
      })
    },
    submitEdit() {
      this.$refs.editForm.validate(async (valid) => {
        if (!valid) return
        this.editDialog.submitting = true
        try {
          const api = this.editDialog.isEdit ? updateHomepageSection : addHomepageSection
          await api(this.editDialog.form)
          this.$message.success(this.editDialog.isEdit ? '修改成功' : '新增成功')
          this.editDialog.visible = false
          this.fetchList()
        } catch (e) {
          this.$message.error('操作失败')
        } finally {
          this.editDialog.submitting = false
        }
      })
    },
    handleDelete(row) {
      this.$confirm('确认删除「' + row.title + '」吗？', '提示', { type: 'warning' })
        .then(async () => {
          await deleteHomepageSection(row.id)
          this.$message.success('删除成功')
          if (this.list.length === 1 && this.query.page > 1) this.query.page--
          this.fetchList()
        })
        .catch(() => {})
    },
    handleBatchDelete() {
      const ids = this.selection.map(r => r.id)
      this.$confirm('确认删除选中的 ' + ids.length + ' 条记录吗？', '提示', { type: 'warning' })
        .then(async () => {
          await batchDeleteHomepageSection(ids)
          this.$message.success('删除成功')
          if (this.list.length === ids.length && this.query.page > 1) this.query.page--
          this.fetchList()
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.filter-container {
  margin-bottom: 16px;
}
.filter-container .el-input,
.filter-container .el-select {
  margin-right: 8px;
}
.pagination-container {
  margin-top: 16px;
  text-align: right;
}
</style>
