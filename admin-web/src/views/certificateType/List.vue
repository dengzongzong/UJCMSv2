<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="clearfix">
        <span>证书类型管理</span>
        <el-button
          type="primary"
          size="mini"
          icon="el-icon-plus"
          style="float: right"
          @click="handleAdd"
        >
          新增类型
        </el-button>
      </div>
      <el-table :data="list" border v-loading="loading" size="small" :fit="false">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="类型名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="code" label="编码(mcode)" width="140" align="center" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" width="160" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" icon="el-icon-delete" class="danger-text" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="dialog.id ? '编辑证书类型' : '新增证书类型'"
      :visible.sync="dialog.visible"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="类型名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入类型名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="编码(mcode)" prop="code">
          <el-input v-model="form.code" placeholder="请输入编码,如3" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="submit">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { certificateTypeList, addCertificateType, updateCertificateType, deleteCertificateType } from '@/api/certificateType'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'CertificateTypeList',
  mixins: [tableMaxHeight],
  data() {
    return {
      loading: false,
      list: [],
      dialog: { visible: false, id: null, submitting: false },
      form: { name: '', code: '', sort: 0, status: 1 },
      rules: {
        name: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      this.loading = true
      certificateTypeList()
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
      this.form = { name: '', code: '', sort: 0, status: 1 }
      this.dialog.visible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialog.id = row.id
      this.form = { name: row.name, code: row.code || '', sort: row.sort || 0, status: row.status }
      this.dialog.visible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    submit() {
      this.$refs.form.validate((valid) => {
        if (!valid) return
        this.dialog.submitting = true
        const action = this.dialog.id
          ? updateCertificateType({ ...this.form, id: this.dialog.id })
          : addCertificateType(this.form)
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
      this.$confirm(`确定要删除证书类型 "${row.name}" 吗?`, '删除确认', { type: 'warning' })
        .then(() => {
          deleteCertificateType(row.id).then(() => {
            this.$message.success('删除成功')
            this.fetchList()
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
</style>
