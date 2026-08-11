<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="clearfix">
        <span>专业管理</span>
        <el-button
          type="primary"
          size="mini"
          icon="el-icon-plus"
          style="float: right"
          @click="handleAddProfession"
        >
          新增专业
        </el-button>
      </div>
      <el-table :data="professions" border v-loading="professionLoading" size="small">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="专业名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="100" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" width="160" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-edit" @click="handleEditProfession(row)">编辑</el-button>
            <el-button type="text" icon="el-icon-delete" class="danger-text" @click="handleDeleteProfession(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 专业弹窗 -->
    <el-dialog
      :title="professionDialog.id ? '编辑专业' : '新增专业'"
      :visible.sync="professionDialog.visible"
      width="420px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form ref="professionForm" :model="professionForm" :rules="professionRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="professionForm.name" placeholder="请输入专业名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="professionForm.sort" :min="0" controls-position="right" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="professionDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="professionDialog.submitting" @click="submitProfession">
          确 定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  professions,
  addProfession,
  updateProfession,
  deleteProfession
} from '@/api/setting'

export default {
  name: 'SettingProfession',
  data() {
    return {
      professionLoading: false,
      professions: [],
      professionDialog: { visible: false, id: null, submitting: false },
      professionForm: { name: '', sort: 0 },
      professionRules: {
        name: [
          { required: true, message: '请输入专业名称', trigger: 'blur' },
          { validator: this.validateName, trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.fetchProfessions()
  },
  methods: {
    validateName(rule, value, callback) {
      const trimmed = (value || '').trim()
      if (!trimmed) return callback()
      const editId = this.professionDialog.id
      const duplicate = this.professions.some(
        (item) => item.name && item.name.trim() === trimmed && item.id !== editId
      )
      if (duplicate) {
        return callback(new Error('该专业名称已存在，不允许重复创建'))
      }
      callback()
    },
    fetchProfessions() {
      this.professionLoading = true
      professions()
        .then((res) => {
          this.professions = res.data || []
        })
        .catch(() => {
          this.professions = []
        })
        .finally(() => {
          this.professionLoading = false
        })
    },
    handleAddProfession() {
      this.professionDialog.id = null
      this.professionForm = { name: '', sort: 0 }
      this.professionDialog.visible = true
      this.$nextTick(() => this.$refs.professionForm && this.$refs.professionForm.clearValidate())
    },
    handleEditProfession(row) {
      this.professionDialog.id = row.id
      this.professionForm = { name: row.name, sort: row.sort || 0 }
      this.professionDialog.visible = true
      this.$nextTick(() => this.$refs.professionForm && this.$refs.professionForm.clearValidate())
    },
    submitProfession() {
      this.$refs.professionForm.validate((valid) => {
        if (!valid) return
        this.professionDialog.submitting = true
        const payload = { ...this.professionForm }
        const action = this.professionDialog.id
          ? updateProfession({ ...payload, id: this.professionDialog.id })
          : addProfession(payload)
        action
          .then(() => {
            this.$message.success(this.professionDialog.id ? '更新成功' : '新增成功')
            this.professionDialog.visible = false
            this.fetchProfessions()
          })
          .finally(() => {
            this.professionDialog.submitting = false
          })
      })
    },
    handleDeleteProfession(row) {
      this.$confirm(`确定要删除专业 "${row.name}" 吗?`, '删除确认', {
        type: 'warning'
      })
        .then(() => {
          deleteProfession(row.id).then(() => {
            this.$message.success('删除成功')
            this.fetchProfessions()
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
