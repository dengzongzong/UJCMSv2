<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="filter-container">
        <el-input
          v-model="query.courseName"
          placeholder="课程名称"
          clearable
          class="filter-item"
          style="width: 200px"
          @keyup.enter.native="handleSearch"
        />
        <el-input
          v-model="query.phone"
          placeholder="学生手机号"
          clearable
          class="filter-item"
          style="width: 180px"
          @keyup.enter.native="handleSearch"
        />
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="学习开始日期"
          end-placeholder="学习结束日期"
          value-format="yyyy-MM-dd"
          class="filter-item"
          style="width: 280px"
        />
        <el-select
          v-model="query.courseStatus"
          placeholder="课程状态"
          clearable
          class="filter-item"
          style="width: 140px"
        >
          <el-option label="已上架" :value="1" />
          <el-option label="未上架" :value="0" />
        </el-select>
        <el-input-number
          v-model="query.exactCount"
          :min="1"
          :max="10000"
          placeholder="显示最新N条"
          controls-position="right"
          class="filter-item"
          style="width: 160px"
        />
        <el-button type="primary" icon="el-icon-search" class="filter-item" @click="handleSearch">
          搜索
        </el-button>
        <el-button icon="el-icon-refresh" class="filter-item" @click="handleReset">重置</el-button>
        <el-button
          type="warning"
          icon="el-icon-download"
          class="filter-item"
          style="float: right"
          :loading="exporting"
          @click="handleExport"
        >
          导出Excel
        </el-button>
      </div>

      <el-table v-loading="loading" :max-height="tableMaxHeight" :fit="false" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="courseName" label="课程名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="课程标签" width="160">
          <template slot-scope="{ row }">
            <el-tag
              v-for="tag in splitTags(row.tag)"
              :key="tag"
              size="mini"
              class="tag-item"
              type="success"
            >
              {{ tag }}
            </el-tag>
            <span v-if="!splitTags(row.tag).length">-</span>
          </template>
        </el-table-column>
        <el-table-column label="课程价格" width="100" align="center">
          <template slot-scope="{ row }">¥{{ row.price || 0 }}</template>
        </el-table-column>
        <el-table-column prop="sectionCount" label="小节数量" width="90" align="center" />
        <el-table-column label="课程状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.courseStatus === 1 ? 'success' : 'info'" size="mini">
              {{ row.courseStatus === 1 ? '已上架' : '未上架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="学生手机号" width="140" align="center" />
        <el-table-column prop="startTime" label="学习开始时间" width="170" align="center" />
        <el-table-column label="学习进度" width="200" align="center">
          <template slot-scope="{ row }">
            <el-progress :percentage="Number(row.progress || 0)" :status="row.progress >= 100 ? 'success' : ''" />
          </template>
        </el-table-column>
        <el-table-column prop="lastStudyTime" label="最近学习时间" width="170" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusType(row.status)" size="mini">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
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

    <el-dialog title="学习记录详情" :visible.sync="detailDialog.visible" width="640px">
      <div v-loading="detailDialog.loading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="课程名称">{{ detailDialog.data.courseName }}</el-descriptions-item>
          <el-descriptions-item label="学生手机号">{{ detailDialog.data.phone }}</el-descriptions-item>
          <el-descriptions-item label="学习开始时间">{{ detailDialog.data.startTime }}</el-descriptions-item>
          <el-descriptions-item label="最近学习时间">{{ detailDialog.data.lastStudyTime }}</el-descriptions-item>
          <el-descriptions-item label="学习进度">{{ detailDialog.data.progress || 0 }}%</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(detailDialog.data.status) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div slot="footer">
        <el-button @click="detailDialog.visible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { courseRecordPage, courseRecordDetail, exportCourseRecord } from '@/api/courseRecord'
import { downloadBlob } from '@/utils'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'CourseStudyRecord',
  mixins: [tableMaxHeight],
  data() {
    return {
      loading: false,
      exporting: false,
      list: [],
      total: 0,
      query: {
        page: 1,
        size: 10,
        courseName: '',
        phone: '',
        courseStatus: undefined,
        dateRange: [],
        exactCount: undefined
      },
      detailDialog: {
        visible: false,
        loading: false,
        data: {}
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    statusText(status) {
      const map = { 0: '未开始', 1: '学习中', 2: '已完成' }
      return map[status] || '学习中'
    },
    statusType(status) {
      const map = { 0: 'info', 1: '', 2: 'success' }
      return map[status] || ''
    },
    splitTags(tag) {
      if (!tag) return []
      return String(tag).split(/[,，;；\s]+/).filter(Boolean)
    },
    fetchList() {
      this.loading = true
      const params = {
        page: this.query.page,
        size: this.query.size,
        courseName: this.query.courseName,
        phone: this.query.phone,
        courseStatus: this.query.courseStatus
      }
      if (this.query.dateRange && this.query.dateRange.length === 2) {
        params.startDate = this.query.dateRange[0]
        params.endDate = this.query.dateRange[1]
      }
      if (this.query.exactCount && this.query.exactCount > 0) {
        params.exactCount = this.query.exactCount
      }
      courseRecordPage(params)
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
        courseName: '',
        phone: '',
        courseStatus: undefined,
        dateRange: [],
        exactCount: undefined
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
    handleDetail(row) {
      this.detailDialog.visible = true
      this.detailDialog.loading = true
      this.detailDialog.data = {}
      courseRecordDetail(row.id)
        .then((res) => {
          this.detailDialog.data = res.data || row
        })
        .catch(() => {
          this.detailDialog.data = row
        })
        .finally(() => {
          this.detailDialog.loading = false
        })
    },
    handleExport() {
      this.exporting = true
      const params = {
        courseName: this.query.courseName,
        phone: this.query.phone,
        courseStatus: this.query.courseStatus
      }
      if (this.query.dateRange && this.query.dateRange.length === 2) {
        params.startDate = this.query.dateRange[0]
        params.endDate = this.query.dateRange[1]
      }
      exportCourseRecord(params)
        .then((response) => {
          downloadBlob(response, '课程学习记录.xlsx')
          this.$message.success('导出成功')
        })
        .catch(() => {
          this.$message.error('导出失败')
        })
        .finally(() => {
          this.exporting = false
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.tag-item {
  margin: 2px 4px 2px 0;
}
</style>
