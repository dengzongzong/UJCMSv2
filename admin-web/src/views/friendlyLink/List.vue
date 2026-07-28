<template>
  <div class="app-container">
    <!-- 筛选区 -->
    <el-form :inline="true" :model="query" size="small" class="filter-form">
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width:120px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="onSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" icon="el-icon-plus" size="small" @click="openDialog()">新增</el-button>
      <el-button type="danger" icon="el-icon-delete" size="small" :disabled="selection.length === 0" @click="onDelete">批量删除</el-button>
      <span class="hint">提示:学员端首页底部以 2 行 5 列的形式展示,最多 10 个。</span>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :fit="false"
      :data="list"
      border
      stripe
      @selection-change="rows => (selection = rows)"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column label="图标" width="100" align="center">
        <template slot-scope="s">
          <el-image
            v-if="s.row.imageUrl"
            :src="resolveImg(s.row.imageUrl)"
            :preview-src-list="[resolveImg(s.row.imageUrl)]"
            style="width:60px;height:60px"
            fit="contain"
          />
          <span v-else>无图</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="linkUrl" label="链接URL" min-width="240" />
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column prop="remark" label="备注" min-width="160" />
      <el-table-column label="状态" width="80" align="center">
        <template slot-scope="s">
          <el-tag :type="s.row.status === 1 ? 'success' : 'info'" size="mini">
            {{ s.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template slot-scope="s">
          <el-button size="mini" type="primary" @click="openDialog(s.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="onDeleteOne(s.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :current-page.sync="query.page"
      :page-size.sync="query.size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="loadList"
      @size-change="loadList"
    />

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="form.id ? '编辑友链' : '新增友链'"
      :visible.sync="dialogVisible"
      width="560px"
      :close-on-click-modal="false"
      @closed="onDialogClosed"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" size="small">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="友链名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="链接URL" prop="linkUrl">
          <el-input v-model="form.linkUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="图标">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="onUpload"
            accept="image/*"
            action="#"
          >
            <el-button type="success" icon="el-icon-upload" :loading="uploading">
              {{ form.imageUrl ? '重新上传' : '选择图标' }}
            </el-button>
          </el-upload>
          <el-image
            v-if="form.imageUrl"
            :src="resolveImg(form.imageUrl)"
            :preview-src-list="[resolveImg(form.imageUrl)]"
            style="width:80px;height:80px;margin-top:8px"
            fit="contain"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  friendlyLinkPage,
  addFriendlyLink,
  updateFriendlyLink,
  deleteFriendlyLink
} from '@/api/friendlyLink'
import { uploadFile as uploadRequest } from '@/api/upload'
import { apiUrl } from '@/utils/apiBase'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'FriendlyLinkList',
  mixins: [tableMaxHeight],
  data() {
    return {
      loading: false,
      query: { page: 1, size: 10, status: null },
      list: [],
      total: 0,
      selection: [],
      dialogVisible: false,
      saving: false,
      uploading: false,
      form: this.initForm(),
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        linkUrl: [{ required: true, message: '请输入链接URL', trigger: 'blur' }]
      }
    }
  },
  mounted() {
    this.loadList()
  },
  methods: {
    apiUrl,
    resolveImg(u) {
      if (!u) return ''
      if (u.startsWith('http')) return u
      return apiUrl(u)
    },
    initForm() {
      return {
        id: null,
        name: '',
        imageUrl: '',
        linkUrl: '',
        sort: 0,
        status: 1,
        remark: ''
      }
    },
    onSearch() {
      this.query.page = 1
      this.loadList()
    },
    onReset() {
      this.query = { page: 1, size: 10, status: null }
      this.loadList()
    },
    async loadList() {
      this.loading = true
      try {
        const res = await friendlyLinkPage(this.query)
        this.list = res.data.records || []
        this.total = res.data.total || 0
      } finally {
        this.loading = false
      }
    },
    openDialog(row) {
      if (row) {
        this.form = { ...row }
      } else {
        this.form = this.initForm()
      }
      this.dialogVisible = true
    },
    onDialogClosed() {
      this.form = this.initForm()
      this.saving = false
    },
    beforeUpload(file) {
      const isImg = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isImg) { this.$message.error('只能上传图片'); return false }
      if (!isLt5M) { this.$message.error('图片不能超过 5MB'); return false }
      return true
    },
    async onUpload({ file }) {
      this.uploading = true
      try {
        const fd = new FormData()
        fd.append('file', file)
        const res = await uploadRequest(fd)
        this.form.imageUrl = res.data
        this.$message.success('上传成功')
      } catch (e) {
        this.$message.error('上传失败: ' + (e.message || '未知错误'))
      } finally {
        this.uploading = false
      }
    },
    async onSubmit() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        this.saving = true
        try {
          if (this.form.id) {
            await updateFriendlyLink(this.form)
          } else {
            const payload = { ...this.form }
            delete payload.id
            await addFriendlyLink(payload)
          }
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.loadList()
        } catch (e) {
          this.$message.error('保存失败: ' + (e.message || '未知错误'))
        } finally {
          this.saving = false
        }
      })
    },
    onDeleteOne(id) {
      this.$confirm('确定删除该友链吗?', '删除确认', {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }).then(() => deleteFriendlyLink([id]))
        .then(() => { this.$message.success('删除成功'); this.loadList() })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') this.$message.error('删除失败')
        })
    },
    onDelete() {
      const ids = this.selection.map(s => s.id)
      this.$confirm('确定删除选中的 ' + ids.length + ' 条友链吗?', '删除确认', {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }).then(() => deleteFriendlyLink(ids))
        .then(() => { this.$message.success('删除成功'); this.loadList() })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') this.$message.error('删除失败')
        })
    }
  }
}
</script>

<style scoped>
.filter-form { margin-bottom: 12px; }
.toolbar { margin-bottom: 12px; }
.toolbar .el-button { margin-right: 8px; }
.hint { margin-left: 12px; color: #909399; font-size: 12px; }
.pagination { margin-top: 16px; text-align: right; }
</style>
