<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="clearfix">
        <span>课程分类设置</span>
        <el-button
          type="primary"
          size="mini"
          icon="el-icon-plus"
          style="float: right"
          @click="handleAdd"
        >
          新增分类
        </el-button>
      </div>
      <el-table :data="list" border v-loading="loading" size="small">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="分类名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="videoCount" label="视频数量" width="120" align="center" />
        <el-table-column prop="sort" label="排序" width="100" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="160" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" icon="el-icon-delete" class="danger-text" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      :title="dialog.id ? '编辑分类' : '新增分类'"
      :visible.sync="dialog.visible"
      width="420px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="30" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" controls-position="right" />
          <span class="form-tip">数字越小越靠前</span>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="submitForm">
          确 定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  videoCategories,
  addVideoCategory,
  updateVideoCategory,
  deleteVideoCategory
} from '@/api/setting'

export default {
  name: 'SettingVideoCategory',
  data() {
    return {
      loading: false,
      list: [],
      dialog: { visible: false, id: null, submitting: false },
      form: { name: '', sort: 0 },
      rules: {
        name: [
          { required: true, message: '请输入分类名称', trigger: 'blur' },
          { validator: this.validateName, trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    validateName(rule, value, callback) {
      const trimmed = (value || '').trim()
      if (!trimmed) return callback()
      const editId = this.dialog.id
      const duplicate = this.list.some(
        (item) => item.name && item.name.trim() === trimmed && item.id !== editId
      )
      if (duplicate) {
        return callback(new Error('该分类名称已存在，不允许重复创建'))
      }
      callback()
    },
    fetchList() {
      this.loading = true
      videoCategories()
        .then((res) => {
          this.list = res.data || []
        })
        .catch(() => {
          this.list = []
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleAdd() {
      this.dialog.id = null
      this.form = { name: '', sort: 0 }
      this.dialog.visible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialog.id = row.id
      this.form = { name: row.name, sort: row.sort || 0 }
      this.dialog.visible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return
        this.dialog.submitting = true
        const payload = { ...this.form }
        const action = this.dialog.id
          ? updateVideoCategory({ ...payload, id: this.dialog.id })
          : addVideoCategory(payload)
        action
          .then(() => {
            this.$message.success(this.dialog.id ? '更新成功' : '新增成功')
            this.dialog.visible = false
            this.fetchList()
          })
          .finally(() => {
            this.dialog.submitting = false
          })
      })
    },
    handleDelete(row) {
      this.$confirm(`确定要删除分类 "${row.name}" 吗?`, '删除确认', { type: 'warning' })
        .then(() => {
          deleteVideoCategory(row.id)
            .then(() => {
              this.$message.success('删除成功')
              this.fetchList()
            })
            .catch((err) => {
              if (err && err.message && err.message.indexOf('引用') > -1) {
                this.$message.error('该分类下存在视频，不可删除')
              }
            })
        })
        .catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.danger-text {
  color: #f56c6c;
}

.form-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
</style>
