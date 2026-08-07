<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="filter-container">
        <el-input
          v-model="query.username"
          placeholder="账号"
          clearable
          class="filter-item"
          style="width: 180px"
          @keyup.enter.native="handleSearch"
        />
        <el-input
          v-model="query.roleName"
          placeholder="角色名称"
          clearable
          class="filter-item"
          style="width: 180px"
          @keyup.enter.native="handleSearch"
        />
        <el-select
          v-model="query.status"
          placeholder="状态"
          clearable
          class="filter-item"
          style="width: 140px"
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" class="filter-item" @click="handleSearch">
          搜索
        </el-button>
        <el-button icon="el-icon-refresh" class="filter-item" @click="handleReset">重置</el-button>
        <el-button
          type="success"
          icon="el-icon-plus"
          class="filter-item"
          style="float: right"
          @click="handleAdd"
        >
          新增子管理员
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        :fit="false"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="账号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="roleName" label="角色名称" min-width="120" show-overflow-tooltip />
        <el-table-column label="权限" min-width="200">
          <template slot-scope="{ row }">
            <el-tag
              v-for="perm in formatPermissions(row.permissions)"
              :key="perm"
              size="mini"
              class="perm-tag"
            >
              {{ perm }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="200" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button
              type="text"
              :icon="row.status === 1 ? 'el-icon-turn-off' : 'el-icon-open'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
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

    <!-- 修改当前管理员密码 -->
    <el-card shadow="never" style="margin-top: 16px">
      <div slot="header"><span>账号安全</span></div>
      <el-form :inline="true" size="small">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" style="width: 200px" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码(至少6位)" style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="pwdForm.submitting" @click="onChangePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog
      :title="dialog.id ? '编辑子管理员' : '新增子管理员'"
      :visible.sync="dialog.visible"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form
        ref="form"
        :model="form"
        :rules="rules"
        label-width="100px"
        :disabled="dialog.submitting"
      >
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入登录账号"
            :disabled="!!dialog.id"
            maxlength="30"
          />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" maxlength="30" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="dialog.id ? '不修改请留空' : '请输入密码'"
            maxlength="30"
          />
        </el-form-item>
        <el-form-item label="权限" prop="permissions">
          <el-checkbox-group v-model="form.permissions">
            <el-checkbox
              v-for="item in permissionOptions"
              :key="item.value"
              :label="item.value"
              class="perm-checkbox"
            >
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
          <div class="perm-actions">
            <el-button type="text" @click="selectAllPermissions">全选</el-button>
            <el-button type="text" @click="form.permissions = []">清空</el-button>
          </div>
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
        <el-button
          type="primary"
          :loading="dialog.submitting"
          @click="submitForm"
        >
          确 定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  adminPage,
  addAdmin,
  updateAdmin,
  deleteAdmin,
  getCurrentAdmin,
  changeAdminPassword
} from '@/api/admin'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'AdminList',
  mixins: [tableMaxHeight],
  data() {
    const validatePassword = (rule, value, callback) => {
      if (!this.dialog.id && !value) {
        callback(new Error('请输入密码'))
      } else if (value && value.length < 6) {
        callback(new Error('密码长度不能少于6位'))
      } else {
        callback()
      }
    }
    return {
      loading: false,
      list: [],
      total: 0,
      query: {
        page: 1,
        size: 10,
        username: '',
        roleName: '',
        status: undefined
      },
      permissionOptions: [
        { value: 'dashboard', label: '仪表盘' },
        { value: 'admin', label: '子管理员管理' },
        { value: 'student', label: '学生管理' },
        { value: 'video', label: '视频管理' },
        { value: 'course', label: '课程管理' },
        { value: 'question', label: '题库管理' },
        { value: 'paper', label: '试卷管理' },
        { value: 'exam', label: '考试管理' },
        { value: 'certificate', label: '证书管理' },
        { value: 'live', label: '直播管理' },
        { value: 'order', label: '订单管理' },
        { value: 'setting', label: '系统设置' },
        { value: 'delete', label: '数据删除（需单独开启）' }
      ],
      dialog: {
        visible: false,
        id: null,
        submitting: false
      },
      form: {
        username: '',
        roleName: '',
        password: '',
        permissions: [],
        status: 1
      },
      pwdForm: { oldPassword: '', newPassword: '', submitting: false },
      rules: {
        username: [
          { required: true, message: '请输入登录账号', trigger: 'blur' },
          { min: 3, max: 30, message: '账号长度为3-30位', trigger: 'blur' }
        ],
        roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
        password: [{ validator: validatePassword, trigger: 'blur' }],
        permissions: [
          {
            type: 'array',
            required: true,
            message: '请至少选择一项权限',
            trigger: 'change'
          }
        ],
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
      adminPage(this.query)
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
        username: '',
        roleName: '',
        status: undefined
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
    formatPermissions(perms) {
      if (!perms) return []
      if (Array.isArray(perms)) return perms
      if (typeof perms === 'string') {
        try {
          const arr = JSON.parse(perms)
          return Array.isArray(arr) ? arr : perms.split(',')
        } catch (e) {
          return perms.split(',')
        }
      }
      return []
    },
    handleAdd() {
      this.dialog.id = null
      this.form = {
        username: '',
        roleName: '',
        password: '',
        permissions: [],
        status: 1
      }
      this.dialog.visible = true
      this.$nextTick(() => {
        this.$refs.form && this.$refs.form.clearValidate()
      })
    },
    handleEdit(row) {
      this.dialog.id = row.id
      this.form = {
        username: row.username,
        roleName: row.roleName,
        password: '',
        permissions: this.formatPermissions(row.permissions),
        status: row.status
      }
      this.dialog.visible = true
      this.$nextTick(() => {
        this.$refs.form && this.$refs.form.clearValidate()
      })
    },
    selectAllPermissions() {
      this.form.permissions = this.permissionOptions.map((i) => i.value)
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return
        this.dialog.submitting = true
        const payload = { ...this.form }
        if (this.dialog.id) {
          payload.id = this.dialog.id
          if (!payload.password) delete payload.password
        }
        const action = this.dialog.id ? updateAdmin(payload) : addAdmin(payload)
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
    resetForm() {
      this.dialog.id = null
      this.$refs.form && this.$refs.form.resetFields()
    },
    handleToggleStatus(row) {
      const target = row.status === 1 ? 0 : 1
      const text = target === 1 ? '启用' : '禁用'
      this.$confirm(`确定要${text}账号 "${row.username}" 吗?`, '提示', {
        type: 'warning'
      })
        .then(() => {
          updateAdmin({ id: row.id, status: target })
            .then(() => {
              this.$message.success(`${text}成功`)
              this.fetchList()
            })
        })
        .catch(() => {})
    },
    handleDelete(row) {
      this.$confirm(`确定要删除账号 "${row.username}" 吗? 该操作不可恢复`, '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
        .then(() => {
          deleteAdmin(row.id).then(() => {
            this.$message.success('删除成功')
            if (this.list.length === 1 && this.query.page > 1) {
              this.query.page--
            }
            this.fetchList()
          })
        })
        .catch(() => {})
    },
    onChangePassword() {
      if (!this.pwdForm.oldPassword) { this.$message.warning('请输入原密码'); return }
      if (!this.pwdForm.newPassword) { this.$message.warning('请输入新密码'); return }
      if (this.pwdForm.newPassword.length < 6) { this.$message.warning('新密码长度不能少于6位'); return }
      if (this.pwdForm.oldPassword === this.pwdForm.newPassword) { this.$message.warning('新密码不能与原密码相同'); return }
      this.pwdForm.submitting = true
      changeAdminPassword({
        oldPassword: this.pwdForm.oldPassword,
        newPassword: this.pwdForm.newPassword
      }).then(() => {
        this.$message.success('密码修改成功，请重新登录')
        this.pwdForm = { oldPassword: '', newPassword: '', submitting: false }
        // 清除token跳转到登录页
        localStorage.removeItem('admin_token')
        this.$store.dispatch('admin/resetToken')
        this.$router.push('/login')
      }).catch(e => {
        this.$message.error('修改失败: ' + (e.message || '未知错误'))
      }).finally(() => {
        this.pwdForm.submitting = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.perm-tag {
  margin: 2px 4px 2px 0;
}

.perm-checkbox {
  margin-left: 0 !important;
  margin-right: 16px;
  margin-bottom: 8px;
}

.perm-actions {
  margin-top: 4px;
}

.danger-text {
  color: #f56c6c;
}
</style>
