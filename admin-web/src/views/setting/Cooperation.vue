<template>
  <div class="app-container">
    <el-card shadow="never" v-loading="loading">
      <div slot="header">
        <span>合作咨询设置</span>
        <el-tooltip content="用户在学员端首页点击『合作咨询』时弹窗内的展示内容" placement="top" style="margin-left: 8px">
          <i class="el-icon-question"></i>
        </el-tooltip>
      </div>

      <el-tabs v-model="activeTab" type="border-card">
        <!-- 弹窗内容配置 -->
        <el-tab-pane label="弹窗内容" name="content">
          <el-form ref="form" :model="form" :rules="rules" label-width="120px" style="max-width: 880px">
            <el-divider content-position="left">左栏:单位背景</el-divider>
            <el-form-item label="单位背景" prop="intro">
              <el-input v-model="form.intro" type="textarea" :rows="5" maxlength="1000" show-word-limit
                placeholder="左栏『单位背景』模块下的介绍文字" />
            </el-form-item>

            <el-divider content-position="left">中栏:流程 / 联系方式 / 意向表</el-divider>
            <el-form-item label="合作流程" prop="processDesc">
              <el-input v-model="form.processDesc" type="textarea" :rows="6" maxlength="2000" show-word-limit
                placeholder="合作流程说明(支持换行;每行对应一个流程步骤)" />
            </el-form-item>
            <el-form-item label="联系电话1" prop="phone1">
              <el-input v-model="form.phone1" placeholder="例如:010-67758599" maxlength="50" />
            </el-form-item>
            <el-form-item label="联系电话2" prop="phone2">
              <el-input v-model="form.phone2" placeholder="例如:010-53397379" maxlength="50" />
            </el-form-item>
            <el-form-item label="联系邮箱1" prop="email1">
              <el-input v-model="form.email1" placeholder="例如:hezuo@example.com" maxlength="100" />
            </el-form-item>
            <el-form-item label="联系邮箱2" prop="email2">
              <el-input v-model="form.email2" placeholder="例如:fuwu@example.com" maxlength="100" />
            </el-form-item>

            <el-divider content-position="left">意向表下载</el-divider>
            <el-form-item label="附件名" prop="attachmentName">
              <el-input v-model="form.attachmentName" placeholder="例如:测评服务合作意向表.docx" maxlength="200" />
            </el-form-item>
            <el-form-item label="附件" prop="attachmentUrl">
              <el-upload
                :show-file-list="false"
                :before-upload="beforeUpload"
                :http-request="onUpload"
                action="#"
                accept=".doc,.docx,.pdf,.xls,.xlsx"
              >
                <el-button type="success" icon="el-icon-upload" :loading="uploading">
                  {{ form.attachmentUrl ? '重新上传附件' : '上传附件' }}
                </el-button>
              </el-upload>
              <div v-if="form.attachmentUrl" class="attachment-preview">
                <i class="el-icon-document"></i>
                <a :href="resolveFile(form.attachmentUrl)" target="_blank" rel="noopener">
                  {{ form.attachmentName || form.attachmentUrl }}
                </a>
                <el-button type="text" class="danger-text" @click="clearAttachment">删除</el-button>
              </div>
              <div slot="tip" class="el-upload__tip">支持 doc/docx/pdf/xls/xlsx,大小不超过 20MB</div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 留言列表 -->
        <el-tab-pane label="合作咨询留言" name="list">
          <div class="filter-container">
            <el-input v-model="query.keyword" placeholder="单位/联系人/电话/内容" clearable
              class="filter-item" style="width: 220px" @keyup.enter.native="onSearch" />
            <el-select v-model="query.status" placeholder="处理状态" clearable
              class="filter-item" style="width: 120px">
              <el-option label="未处理" :value="0" />
              <el-option label="已处理" :value="1" />
            </el-select>
            <el-button type="primary" icon="el-icon-search" class="filter-item" @click="onSearch">搜索</el-button>
            <el-button icon="el-icon-refresh" class="filter-item" @click="onReset">重置</el-button>
            <el-button type="danger" icon="el-icon-delete" class="filter-item" :disabled="selection.length === 0"
              style="float: right" @click="onDelete">批量删除</el-button>
          </div>

          <el-table v-loading="listLoading" :data="list" border stripe
            @selection-change="rows => (selection = rows)">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="orgName" label="单位名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="contactName" label="联系人" width="100" />
            <el-table-column prop="phone" label="联系电话" width="140" />
            <el-table-column prop="email" label="邮箱" width="180" show-overflow-tooltip />
            <el-table-column prop="content" label="合作意向" min-width="240" show-overflow-tooltip>
              <template slot-scope="s">
                <span>{{ s.row.content ? s.row.content.substring(0, 60) : '' }}{{ s.row.content && s.row.content.length > 60 ? '...' : '' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template slot-scope="s">
                <el-tag :type="s.row.status === 1 ? 'success' : 'warning'" size="mini">
                  {{ s.row.status === 1 ? '已处理' : '未处理' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="170" align="center" />
            <el-table-column label="操作" width="180" align="center">
              <template slot-scope="s">
                <el-button type="text" @click="openDetail(s.row)">查看</el-button>
                <el-button v-if="s.row.status === 0" type="text" @click="onHandle(s.row)">标记已处理</el-button>
                <el-button type="text" class="danger-text" @click="onDeleteOne(s.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination :current-page="query.page" :page-sizes="[10, 20, 50]" :page-size="query.size"
              :total="total" layout="total, sizes, prev, pager, next, jumper" background
              @size-change="size => { query.size = size; query.page = 1; loadList() }"
              @current-change="page => { query.page = page; loadList() }" />
          </div>
        </el-tab-pane>

        <!-- 合作申请查询 -->
        <el-tab-pane label="合作申请" name="application">
          <div class="filter-container">
            <el-input v-model="applyQuery.unitName" placeholder="单位名称(必填)" clearable
              class="filter-item" style="width: 220px" @keyup.enter.native="onApplySearch" />
            <el-input v-model="applyQuery.authCode" placeholder="授权管理编号(必填)" clearable
              class="filter-item" style="width: 220px" @keyup.enter.native="onApplySearch" />
            <el-button type="primary" icon="el-icon-search" class="filter-item" @click="onApplySearch">查 询</el-button>
            <el-button icon="el-icon-refresh" class="filter-item" @click="onApplyReset">重 置</el-button>
          </div>

          <el-table v-loading="applyLoading" :data="applyList" border stripe>
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="unitName" label="单位名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="authCode" label="授权管理编号" width="160" show-overflow-tooltip />
            <el-table-column prop="legalPerson" label="法人" width="100" />
            <el-table-column prop="contactName" label="联系人" width="100" />
            <el-table-column prop="contactPhone" label="联系电话" width="130" />
            <el-table-column label="状态" width="100" align="center">
              <template slot-scope="{ row }">
                <el-tag v-if="row.status === 0" type="warning" size="mini">待审核</el-tag>
                <el-tag v-else-if="row.status === 1" type="success" size="mini">已通过</el-tag>
                <el-tag v-else-if="row.status === 2" type="danger" size="mini">已拒绝</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="申请时间" width="160" align="center" />
          </el-table>

          <div class="pagination-container">
            <el-pagination :current-page="applyQuery.page" :page-sizes="[10, 20, 50]" :page-size="applyQuery.size"
              :total="applyTotal" layout="total, sizes, prev, pager, next, jumper" background
              @size-change="s => { applyQuery.size = s; applyQuery.page = 1; loadApplyList() }"
              @current-change="p => { applyQuery.page = p; loadApplyList() }" />
          </div>

          <div v-if="!applyQueried" style="text-align: center; color: #909399; padding: 30px 0;">
            请输入单位名称和授权管理编号进行查询
          </div>
        </el-tab-pane>
      </el-tabs>

      <div v-show="activeTab === 'content'" class="form-footer">
        <el-button type="primary" :loading="submitting" @click="submitForm">保 存</el-button>
        <el-button @click="fetchDetail">重 置</el-button>
      </div>
    </el-card>

    <!-- 留言详情 -->
    <el-dialog title="留言详情" :visible.sync="detailDialog.visible" width="640px">
      <el-descriptions v-if="detailDialog.row" :column="1" border>
        <el-descriptions-item label="单位名称">{{ detailDialog.row.orgName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detailDialog.row.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailDialog.row.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detailDialog.row.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交IP">{{ detailDialog.row.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detailDialog.row.createTime }}</el-descriptions-item>
        <el-descriptions-item label="合作意向">
          <div style="white-space: pre-wrap;">{{ detailDialog.row.content }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="处理备注">
          <el-input v-model="detailDialog.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-descriptions-item>
      </el-descriptions>
      <div slot="footer">
        <el-button @click="detailDialog.visible = false">关 闭</el-button>
        <el-button type="primary" :loading="detailDialog.submitting" @click="submitHandle">标为已处理</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getCooperationSetting,
  updateCooperationSetting,
  feedbackPage,
  handleFeedback,
  deleteFeedback
} from '@/api/feedback'
import { uploadFile as uploadRequest } from '@/api/upload'
import { cooperationApplyPage } from '@/api/cooperationApply'
import { apiUrl } from '@/utils/apiBase'
import RichEditor from '@/components/RichEditor'

export default {
  name: 'SettingCooperation',
  components: { RichEditor },
  data() {
    return {
      activeTab: 'content',
      loading: false,
      submitting: false,
      uploading: false,
      form: this.initForm(),
      rules: {
        phone1: [{ required: true, message: '请输入联系电话1', trigger: 'blur' }],
        processDesc: [{ required: true, message: '请输入合作流程', trigger: 'blur' }],
        intro: [{ required: true, message: '请输入单位背景介绍', trigger: 'blur' }]
      },
      query: { page: 1, size: 10, keyword: '', status: undefined },
      listLoading: false,
      list: [],
      total: 0,
      selection: [],
      detailDialog: {
        visible: false,
        row: null,
        remark: '',
        submitting: false
      },
      applyQuery: { page: 1, size: 10, unitName: '', authCode: '' },
      applyList: [],
      applyTotal: 0,
      applyLoading: false,
      applyQueried: false
    }
  },
  watch: {
    activeTab(v) {
      if (v === 'list') this.loadList()
      if (v === 'content') this.fetchDetail()
      if (v === 'application') this.loadApplyList()
    }
  },
  created() {
    this.fetchDetail()
  },
  methods: {
    apiUrl,
    resolveFile(u) {
      if (!u) return ''
      if (u.startsWith('http')) return u
      return apiUrl(u)
    },
    initForm() {
      return {
        intro: '',
        processDesc: '',
        phone1: '',
        phone2: '',
        email1: '',
        email2: '',
        attachmentName: '',
        attachmentUrl: ''
      }
    },
    fetchDetail() {
      this.loading = true
      getCooperationSetting()
        .then((res) => {
          const d = (res && res.data) || {}
          this.form = {
            intro: d.intro || '',
            processDesc: d.processDesc || '',
            phone1: d.phone1 || '',
            phone2: d.phone2 || '',
            email1: d.email1 || '',
            email2: d.email2 || '',
            attachmentName: d.attachmentName || '',
            attachmentUrl: d.attachmentUrl || ''
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    beforeUpload(file) {
      const isLt20M = file.size / 1024 / 1024 < 20
      if (!isLt20M) {
        this.$message.error('附件大小不能超过 20MB')
        return false
      }
      return true
    },
    async onUpload({ file }) {
      this.uploading = true
      try {
        const fd = new FormData()
        fd.append('file', file)
        const res = await uploadRequest(fd)
        this.form.attachmentUrl = res.data
        if (!this.form.attachmentName) this.form.attachmentName = file.name
        this.$message.success('上传成功')
      } catch (e) {
        this.$message.error('上传失败: ' + (e.message || '未知错误'))
      } finally {
        this.uploading = false
      }
    },
    clearAttachment() {
      this.form.attachmentUrl = ''
      this.form.attachmentName = ''
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return
        this.submitting = true
        updateCooperationSetting(this.form)
          .then(() => {
            this.$message.success('保存成功')
          })
          .finally(() => {
            this.submitting = false
          })
      })
    },

    // 留言列表
    onSearch() { this.query.page = 1; this.loadList() },
    onReset() {
      this.query = { page: 1, size: 10, keyword: '', status: undefined }
      this.loadList()
    },
    async loadList() {
      this.listLoading = true
      try {
        const params = {
          page: this.query.page,
          size: this.query.size,
          type: 'cooperation',
          status: this.query.status,
          keyword: this.query.keyword
        }
        const res = await feedbackPage(params)
        this.list = (res.data && (res.data.records || res.data.list || [])) || []
        this.total = (res.data && res.data.total) || 0
      } catch (e) {
        this.list = []
        this.total = 0
      } finally {
        this.listLoading = false
      }
    },
    openDetail(row) {
      this.detailDialog.row = row
      this.detailDialog.remark = row.remark || ''
      this.detailDialog.visible = true
    },
    async submitHandle() {
      if (!this.detailDialog.row) return
      this.detailDialog.submitting = true
      try {
        await handleFeedback(this.detailDialog.row.id, this.detailDialog.remark)
        this.$message.success('已标记为已处理')
        this.detailDialog.visible = false
        this.loadList()
      } catch (e) {
        this.$message.error('操作失败: ' + (e.message || ''))
      } finally {
        this.detailDialog.submitting = false
      }
    },
    onHandle(row) {
      this.openDetail(row)
    },
    onDeleteOne(id) {
      this.$confirm('确定要删除该留言吗?', '删除确认', {
        type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消'
      })
        .then(() => deleteFeedback([id]))
        .then(() => { this.$message.success('删除成功'); this.loadList() })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') this.$message.error('删除失败')
        })
    },
    onDelete() {
      const ids = this.selection.map(s => s.id)
      if (ids.length === 0) return
      this.$confirm(`确定要删除选中的 ${ids.length} 条留言吗?`, '删除确认', {
        type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消'
      })
        .then(() => deleteFeedback(ids))
        .then(() => { this.$message.success('删除成功'); this.loadList() })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') this.$message.error('删除失败')
        })
    },

    // 合作申请查询
    onApplySearch() { this.applyQuery.page = 1; this.loadApplyList() },
    onApplyReset() {
      this.applyQuery = { page: 1, size: 10, unitName: '', authCode: '' }
      this.applyList = []
      this.applyTotal = 0
      this.applyQueried = false
    },
    async loadApplyList() {
      if (!this.applyQuery.unitName || !this.applyQuery.authCode) {
        this.applyList = []
        this.applyTotal = 0
        return
      }
      this.applyLoading = true
      this.applyQueried = true
      try {
        const res = await cooperationApplyPage(this.applyQuery)
        this.applyList = (res.data && (res.data.records || res.data.list || [])) || []
        this.applyTotal = (res.data && res.data.total) || 0
      } catch (e) {
        this.applyList = []
        this.applyTotal = 0
      } finally {
        this.applyLoading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.form-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
.attachment-preview {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;

  a { color: #1989fa; text-decoration: none; }
  a:hover { text-decoration: underline; }
}
.danger-text { color: #f56c6c; }
.filter-container { margin-bottom: 12px; }
.filter-item { margin-right: 8px; }
.pagination-container { margin-top: 16px; text-align: right; }
</style>
