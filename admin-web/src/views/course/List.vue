<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="filter-container">
        <el-select
          v-model="query.professionId"
          placeholder="专业"
          clearable
          filterable
          class="filter-item"
          style="width: 180px"
        >
          <el-option
            v-for="item in professionOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-select
          v-model="query.categoryId"
          placeholder="课程分类"
          clearable
          filterable
          class="filter-item"
          style="width: 160px"
        >
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-input
          v-model="query.name"
          placeholder="课程名称"
          clearable
          class="filter-item"
          style="width: 200px"
          @keyup.enter.native="handleSearch"
        />
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="创建开始日期"
          end-placeholder="创建结束日期"
          value-format="yyyy-MM-dd"
          class="filter-item"
          style="width: 280px"
        />
        <el-select
          v-model="query.status"
          placeholder="课程状态"
          clearable
          class="filter-item"
          style="width: 140px"
        >
          <el-option label="已上架" :value="1" />
          <el-option label="未上架" :value="0" />
        </el-select>
        <el-input
          v-model="query.sectionCount"
          placeholder="小节数量"
          clearable
          class="filter-item"
          style="width: 130px"
          @keyup.enter.native="handleSearch"
        />
        <el-button type="primary" icon="el-icon-search" class="filter-item" @click="handleSearch">
          搜索
        </el-button>
        <el-button icon="el-icon-refresh" class="filter-item" @click="handleReset">重置</el-button>
        <el-button type="danger" icon="el-icon-delete" size="small" class="filter-item" :disabled="selection.length === 0" @click="handleBatchDelete">批量删除</el-button>
        <el-button
          type="success"
          icon="el-icon-plus"
          class="filter-item"
          style="float: right"
          @click="handleAdd"
        >
          新增课程
        </el-button>
      </div>

      <el-table v-loading="loading" :max-height="tableMaxHeight" :fit="false" :data="list" border stripe style="width: 100%" @selection-change="rows => (selection = rows)">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="缩略图" width="100" align="center">
          <template slot-scope="{ row }">
            <img v-if="row.coverUrl" :src="apiUrl(row.coverUrl)" class="table-thumb" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="课程名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="professionName" label="专业" width="140" align="center">
          <template slot-scope="{ row }">{{ row.professionName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="categoryName" label="课程分类" width="120" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.categoryName" size="mini" type="info">{{ row.categoryName }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="标签" width="160">
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
        <el-table-column prop="price" label="价格" width="100" align="center">
          <template slot-scope="{ row }">¥{{ row.price || 0 }}</template>
        </el-table-column>
        <el-table-column prop="sectionCount" label="小节数" width="90" align="center" />
        <el-table-column label="置顶" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.isTop === 1" size="mini" type="danger">置顶</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" icon="el-icon-user" @click="handleOpenStudents(row)">开通学生</el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog title="课程详情" :visible.sync="detailDialog.visible" width="680px">
      <div v-loading="detailDialog.loading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="课程名称">{{ detailDialog.data.name }}</el-descriptions-item>
          <el-descriptions-item label="价格">¥{{ detailDialog.data.price || 0 }}</el-descriptions-item>
          <el-descriptions-item label="标签">{{ detailDialog.data.tag || '-' }}</el-descriptions-item>
          <el-descriptions-item label="小节数">{{ detailDialog.data.sectionCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailDialog.data.createTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detailDialog.data.status === 1 ? '上架' : '下架' }}</el-descriptions-item>
          <el-descriptions-item label="简介" :span="2">{{ detailDialog.data.intro || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 16px 0 8px">课程小节</h4>
        <el-table :data="detailDialog.data.sections || []" border size="mini">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="title" label="小节标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="videoName" label="关联视频" min-width="160" show-overflow-tooltip />
        </el-table>
      </div>
      <div slot="footer">
        <el-button @click="detailDialog.visible = false">关 闭</el-button>
      </div>
    </el-dialog>

    <open-students
      v-if="openStudentsDialog.visible"
      :visible.sync="openStudentsDialog.visible"
      :course-id="openStudentsDialog.courseId"
      :course-name="openStudentsDialog.courseName"
    />
  </div>
</template>

<script>
import { coursePage, courseDetail, deleteCourse, batchDeleteCourses } from '@/api/course'
import { professions, videoCategories } from '@/api/setting'
import OpenStudents from './OpenStudents.vue'
import { apiUrl } from '@/utils/apiBase'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'CourseList',
  mixins: [tableMaxHeight],
  components: { OpenStudents },
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      selection: [],
      professionOptions: [],
      categoryOptions: [],
      query: {
        page: 1,
        size: 10,
        professionId: undefined,
        categoryId: undefined,
        name: '',
        status: undefined,
        sectionCount: '',
        dateRange: []
      },
      detailDialog: {
        visible: false,
        loading: false,
        data: {}
      },
      openStudentsDialog: {
        visible: false,
        courseId: null,
        courseName: ''
      }
    }
  },
  created() {
    this.fetchProfessions()
    this.fetchCategories()
    this.fetchList()
  },
  methods: {
    apiUrl,
    fetchProfessions() {
      professions()
        .then((res) => {
          this.professionOptions = res.data || []
        })
        .catch(() => {
          this.professionOptions = []
        })
    },
    fetchCategories() {
      videoCategories()
        .then((res) => {
          this.categoryOptions = res.data || []
        })
        .catch(() => {
          this.categoryOptions = []
        })
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
        professionId: this.query.professionId,
        categoryId: this.query.categoryId,
        name: this.query.name,
        status: this.query.status,
        sectionCount: this.query.sectionCount
      }
      if (this.query.dateRange && this.query.dateRange.length === 2) {
        params.startDate = this.query.dateRange[0]
        params.endDate = this.query.dateRange[1]
      }
      coursePage(params)
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
        professionId: undefined,
        name: '',
        status: undefined,
        sectionCount: '',
        dateRange: []
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
    handleAdd() {
      this.$router.push('/course/edit').catch(() => {})
    },
    handleEdit(row) {
      this.$router.push(`/course/edit/${row.id}`).catch(() => {})
    },
    handleDetail(row) {
      this.detailDialog.visible = true
      this.detailDialog.loading = true
      this.detailDialog.data = {}
      courseDetail(row.id)
        .then((res) => {
          // 后端 detail 返回结构:{ course: {...}, sections: [{ id, name, videos: [{...}] }] }
          // 需要把 sections 数组也合并进弹窗数据,这样"课程小节"表格才能渲染
          const payload = res.data || {}
          const course = payload.course || row
          const sections = payload.sections || []
          this.detailDialog.data = {
            ...course,
            sections: sections.map((s) => ({
              id: s.id,
              title: s.name || '',
              // 后端 videos 里嵌套了完整的 video 实体(videos[i].video.name)
              videoName: (s.videos && s.videos.length > 0 && s.videos[0].video ? s.videos[0].video.name : '') || ''
            }))
          }
        })
        .catch(() => {
          this.detailDialog.data = { ...row, sections: [] }
        })
        .finally(() => {
          this.detailDialog.loading = false
        })
    },
    handleOpenStudents(row) {
      this.openStudentsDialog.courseId = row.id
      this.openStudentsDialog.courseName = row.name
      this.openStudentsDialog.visible = true
    },
    handleDelete(row) {
      this.$confirm(`确定要删除课程 "${row.name}" 吗?`, '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
        .then(() => {
          deleteCourse(row.id).then(() => {
            this.$message.success('删除成功')
            if (this.list.length === 1 && this.query.page > 1) this.query.page--
            this.fetchList()
          })
        })
        .catch(() => {})
    },
    handleBatchDelete() {
      if (this.selection.length === 0) return
      this.$confirm(`确认删除选中的 ${this.selection.length} 条数据?`, '提示', {
        type: 'warning'
      }).then(() => {
        const ids = this.selection.map(item => item.id)
        batchDeleteCourses(ids).then(() => {
          this.$message.success('删除成功')
          this.selection = []
          this.fetchList()
        }).catch(() => {})
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.tag-item {
  margin: 2px 4px 2px 0;
}

.danger-text {
  color: #f56c6c;
}

.table-thumb {
  width: 80px;
  height: 50px;
  object-fit: cover;
  border-radius: 4px;
  vertical-align: middle;
}
</style>
