<template>
  <div class="app-container">
    <!-- 筛选区 -->
    <el-form :inline="true" :model="query" size="small" class="filter-form">
      <el-form-item label="关键词">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="姓名/手机号/学号"
          style="width: 220px"
          @keyup.enter.native="onSearch"
        />
      </el-form-item>
      <el-form-item label="身份证号">
        <el-input
          v-model="query.idCard"
          clearable
          placeholder="身份证号"
          style="width: 220px"
          @keyup.enter.native="onSearch"
        />
      </el-form-item>
      <el-form-item label="证书类型">
        <el-select
          v-model="query.certType"
          clearable
          filterable
          placeholder="全部"
          style="width: 200px"
          @change="onSearch"
        >
          <el-option v-for="item in certTypeOptions" :key="item.id" :label="item.name" :value="item.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="导入时间">
        <el-date-picker
          v-model="query.dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="导入开始时间"
          end-placeholder="导入结束时间"
          value-format="yyyy-MM-dd HH:mm:ss"
          style="width: 380px"
        />
      </el-form-item>
      <el-form-item label="显示条数">
        <el-input-number
          v-model="query.exactCount"
          :min="1"
          :max="10000"
          placeholder="最新N条"
          controls-position="right"
          style="width: 140px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="onSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" icon="el-icon-plus" size="small" @click="openAddDialog">新增证书用户</el-button>
      <el-button type="warning" icon="el-icon-refresh" size="small" :loading="syncing" @click="onSync">
        从学生管理同步
      </el-button>
      <el-button type="success" icon="el-icon-document" size="small" :loading="exporting" @click="onExport">
        导出数据
      </el-button>
      <span class="sync-tip">点击后将从「学生管理」同步学员数据到证书用户（按专业维度，每个专业一条记录）</span>
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="姓名" min-width="120" show-overflow-tooltip>
        <template slot-scope="s">{{ s.row.name || '-' }}</template>
      </el-table-column>
      <el-table-column prop="idCard" label="身份证号" min-width="180" show-overflow-tooltip>
        <template slot-scope="s">{{ s.row.idCard || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" min-width="130">
        <template slot-scope="s">{{ s.row.phone || '-' }}</template>
      </el-table-column>
      <el-table-column prop="professionName" label="专业" min-width="140" show-overflow-tooltip>
        <template slot-scope="s">{{ s.row.professionName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="certType" label="证书类型" min-width="160" show-overflow-tooltip>
        <template slot-scope="s">
          <el-tag v-if="s.row.certType" size="mini" type="warning">{{ s.row.certType }}</el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="性别" width="80" align="center">
        <template slot-scope="s">
          <el-tag v-if="s.row.gender === 1" type="primary" size="mini">男</el-tag>
          <el-tag v-else-if="s.row.gender === 2" type="danger" size="mini">女</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="同步时间" width="170" align="center">
        <template slot-scope="s">{{ formatDateCN(s.row.syncTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template slot-scope="s">
          <el-button type="text" icon="el-icon-edit" @click="openEditDialog(s.row)">编辑</el-button>
          <el-button type="text" icon="el-icon-delete" class="danger-text" @click="handleDelete(s.row)">删除</el-button>
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

    <!-- 新增/编辑证书用户弹窗 -->
    <el-dialog
      :title="dialog.isEdit ? '编辑证书用户' : '新增证书用户'"
      :visible.sync="dialog.visible"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="dialogForm" :model="dialog.form" :rules="dialog.rules" label-width="100px" size="small">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="dialog.form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="dialog.form.idCard" placeholder="选填" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="dialog.form.phone" placeholder="选填" maxlength="11" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="dialog.form.professionName" placeholder="选填" />
        </el-form-item>
        <el-form-item label="证书类型">
          <el-select v-model="dialog.form.certType" placeholder="请选择证书类型" clearable filterable allow-create style="width: 100%">
            <el-option v-for="item in certTypeOptions" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
          <div class="form-tip">可选择或手动输入，可为空</div>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="dialog.form.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="submitDialog">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { certificateUserPage, certificateUserSync, addCertificateUser, updateCertificateUser, deleteCertificateUser, downloadFile, triggerDownload } from '@/api/certificate'
import { certificateTypeList } from '@/api/certificateType'

export default {
  name: 'CertificateUserList',
  data() {
    return {
      loading: false,
      syncing: false,
      exporting: false,
      list: [],
      total: 0,
      certTypeOptions: [],
      query: { page: 1, size: 10, keyword: '', idCard: '', certType: '', dateRange: [], exactCount: undefined },
      dialog: {
        visible: false,
        isEdit: false,
        submitting: false,
        form: {
          id: null,
          name: '',
          idCard: '',
          phone: '',
          professionName: '',
          certType: '',
          gender: null
        },
        rules: {
          name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
        }
      }
    }
  },
  mounted() {
    this.fetchCertTypes()
    this.loadList()
  },
  methods: {
    fetchCertTypes() {
      certificateTypeList()
        .then((res) => {
          this.certTypeOptions = res.data || []
        })
        .catch(() => {
          this.certTypeOptions = []
        })
    },
    formatDateTime(d) {
      if (!d) return '-'
      if (typeof d === 'string') {
        const idx = d.indexOf('T')
        if (idx > 0) {
          return d.substring(0, 19).replace('T', ' ')
        }
        return d.substring(0, 19)
      }
      const dt = new Date(d)
      const pad = (n) => (n < 10 ? '0' + n : '' + n)
      return (
        dt.getFullYear() + '-' +
        pad(dt.getMonth() + 1) + '-' +
        pad(dt.getDate()) + ' ' +
        pad(dt.getHours()) + ':' +
        pad(dt.getMinutes()) + ':' +
        pad(dt.getSeconds())
      )
    },
    // 中文日期格式(与用户证书查询界面一致: yyyy年MM月dd日)
    formatDateCN(d) {
      if (!d) return '—'
      let dt
      if (typeof d === 'string') {
        dt = new Date(d.replace('T', ' ').replace(/-/g, '/'))
      } else {
        dt = new Date(d)
      }
      if (isNaN(dt.getTime())) return '—'
      const pad = (n) => (n < 10 ? '0' + n : '' + n)
      return dt.getFullYear() + '年' + pad(dt.getMonth() + 1) + '月' + pad(dt.getDate()) + '日'
    },
    onSearch() {
      this.query.page = 1
      this.loadList()
    },
    onReset() {
      this.query = { page: 1, size: 10, keyword: '', idCard: '', certType: '', dateRange: [], exactCount: undefined }
      this.loadList()
    },
    loadList() {
      this.loading = true
      const params = {
        page: this.query.page,
        size: this.query.size,
        keyword: this.query.keyword,
        idCard: this.query.idCard,
        certType: this.query.certType
      }
      if (this.query.dateRange && this.query.dateRange.length === 2) {
        params.importTimeStart = this.query.dateRange[0]
        params.importTimeEnd = this.query.dateRange[1]
      }
      if (this.query.exactCount && this.query.exactCount > 0) {
        params.exactCount = this.query.exactCount
      }
      certificateUserPage(params)
        .then((res) => {
          const data = (res && res.data) || {}
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
    onSync() {
      this.$confirm('确定要从「学生管理」同步学员数据到证书用户吗？（按专业维度，每个专业创建一条记录）', '同步确认', { type: 'warning' })
        .then(() => {
          this.syncing = true
          certificateUserSync()
            .then((res) => {
              const data = (res && res.data) || {}
              const synced = data.synced != null ? data.synced : (data.count || 0)
              this.$message.success('同步完成，共同步 ' + synced + ' 条')
              this.loadList()
            })
            .catch(() => {
              this.$message.error('同步失败，请稍后重试')
            })
            .finally(() => {
              this.syncing = false
            })
        })
        .catch(() => {})
    },
    openAddDialog() {
      this.dialog.isEdit = false
      this.dialog.form = {
        id: null,
        name: '',
        idCard: '',
        phone: '',
        professionName: '',
        certType: '',
        gender: null
      }
      this.dialog.visible = true
      this.$nextTick(() => {
        this.$refs.dialogForm && this.$refs.dialogForm.clearValidate()
      })
    },
    openEditDialog(row) {
      this.dialog.isEdit = true
      this.dialog.form = {
        id: row.id,
        name: row.name || '',
        idCard: row.idCard || '',
        phone: row.phone || '',
        professionName: row.professionName || '',
        certType: row.certType || '',
        gender: row.gender || null
      }
      this.dialog.visible = true
      this.$nextTick(() => {
        this.$refs.dialogForm && this.$refs.dialogForm.clearValidate()
      })
    },
    submitDialog() {
      this.$refs.dialogForm.validate((valid) => {
        if (!valid) return
        this.dialog.submitting = true
        const data = { ...this.dialog.form }
        const api = this.dialog.isEdit ? updateCertificateUser : addCertificateUser
        api(data)
          .then(() => {
            this.$message.success(this.dialog.isEdit ? '保存成功' : '新增成功')
            this.dialog.visible = false
            this.loadList()
          })
          .catch((err) => {
            this.$message.error((err && err.message) || '操作失败')
          })
          .finally(() => {
            this.dialog.submitting = false
          })
      })
    },
    handleDelete(row) {
      this.$confirm(`确定要删除证书用户 "${row.name || ''}" 吗？`, '提示', { type: 'warning' })
        .then(() => {
          deleteCertificateUser(row.id)
            .then(() => {
              this.$message.success('删除成功')
              this.loadList()
            })
            .catch((err) => {
              this.$message.error((err && err.message) || '删除失败')
            })
        })
        .catch(() => {})
    },
    async onExport() {
      this.exporting = true
      try {
        const url = '/admin/certificate/user/export'
        const queryParts = []
        if (this.query.keyword) queryParts.push('keyword=' + encodeURIComponent(this.query.keyword))
        if (this.query.idCard) queryParts.push('idCard=' + encodeURIComponent(this.query.idCard))
        if (this.query.certType) queryParts.push('certType=' + encodeURIComponent(this.query.certType))
        const fullUrl = queryParts.length > 0 ? url + '?' + queryParts.join('&') : url
        const { blob, fileName } = await downloadFile(fullUrl)
        triggerDownload(blob, fileName || '证书用户数据下载.xlsx')
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败: ' + (e.message || '未知错误'))
      } finally {
        this.exporting = false
      }
    }
  }
}
</script>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}
.toolbar {
  margin-bottom: 12px;
}
.toolbar .sync-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
.pagination {
  margin-top: 16px;
  text-align: right;
}
.danger-text {
  color: #f56c6c;
}
.text-muted {
  color: #c0c4cc;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 4px;
}
</style>
