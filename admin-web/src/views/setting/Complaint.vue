<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="filter-container">
        <el-input v-model="query.keyword" placeholder="联系人/电话/邮箱/内容" clearable
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

      <el-table v-loading="loading" :data="list" border stripe
        @selection-change="rows => (selection = rows)">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="email" label="邮箱" width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="投诉建议" min-width="300" show-overflow-tooltip>
          <template slot-scope="s">
            <span>{{ s.row.content ? s.row.content.substring(0, 80) : '' }}{{ s.row.content && s.row.content.length > 80 ? '...' : '' }}</span>
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
        <el-table-column label="操作" width="180" align="center" fixed="right">
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
    </el-card>

    <!-- 详情 -->
    <el-dialog title="投诉建议详情" :visible.sync="detailDialog.visible" width="640px">
      <el-descriptions v-if="detailDialog.row" :column="1" border>
        <el-descriptions-item label="联系人">{{ detailDialog.row.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailDialog.row.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detailDialog.row.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交IP">{{ detailDialog.row.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detailDialog.row.createTime }}</el-descriptions-item>
        <el-descriptions-item label="投诉建议内容">
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
import { feedbackPage, handleFeedback, deleteFeedback } from '@/api/feedback'

export default {
  name: 'SettingComplaint',
  data() {
    return {
      loading: false,
      query: { page: 1, size: 10, keyword: '', status: undefined },
      list: [],
      total: 0,
      selection: [],
      detailDialog: { visible: false, row: null, remark: '', submitting: false }
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    onSearch() { this.query.page = 1; this.loadList() },
    onReset() {
      this.query = { page: 1, size: 10, keyword: '', status: undefined }
      this.loadList()
    },
    async loadList() {
      this.loading = true
      try {
        const params = {
          page: this.query.page,
          size: this.query.size,
          type: 'complaint',
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
        this.loading = false
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
    onHandle(row) { this.openDetail(row) },
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
    }
  }
}
</script>

<style lang="scss" scoped>
.danger-text { color: #f56c6c; }
.filter-container { margin-bottom: 12px; }
.filter-item { margin-right: 8px; }
.pagination-container { margin-top: 16px; text-align: right; }
</style>
