<template>
  <div class="app-container">
    <el-page-header @back="goBack" content="学生详情" class="page-header" />

    <el-card shadow="never" class="info-card" v-loading="loading">
      <div slot="header">
        <span>基本信息</span>
      </div>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="手机号">{{ student.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ student.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="专业">
          <template v-if="student.professionNames && student.professionNames.length">
            <el-tag v-for="(name, idx) in student.professionNames" :key="idx" size="mini" style="margin-right: 4px; margin-bottom: 2px;">{{ name }}</el-tag>
          </template>
          <span v-else-if="student.professionName">{{ student.professionName }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="身份证号">
          <span v-if="student.idCard" class="id-card">{{ student.idCard }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="student.status === 1 ? 'success' : 'danger'" size="mini">
            {{ student.status === 1 ? '正常' : '冻结' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ student.registerTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上次登录时间">{{ student.lastLoginTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学生ID">{{ student.id || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="info-card">
      <div slot="header" class="clearfix">
        <span>已开通课程 ({{ courses.length }})</span>
      </div>
      <el-table :data="courses" border size="small" v-loading="courseLoading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="课程名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sectionCount" label="小节数" width="90" align="center" />
        <el-table-column prop="progress" label="学习进度" width="160" align="center">
          <template slot-scope="{ row }">
            <el-progress :percentage="Number(row.progress || 0)" />
          </template>
        </el-table-column>
        <el-table-column prop="openTime" label="开通时间" width="170" align="center" />
      </el-table>
      <el-empty v-if="!courseLoading && courses.length === 0" description="暂无已开通课程" />
    </el-card>

    <el-card shadow="never" class="info-card">
      <div slot="header" class="clearfix">
        <span>已开通考试 ({{ exams.length }})</span>
      </div>
      <el-table :data="exams" border size="small" v-loading="examLoading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="考试名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="totalScore" label="总分" width="90" align="center" />
        <el-table-column prop="duration" label="时长(分)" width="100" align="center" />
        <el-table-column prop="score" label="考试分数" width="100" align="center">
          <template slot-scope="{ row }">
            <span v-if="row.score !== null && row.score !== undefined">{{ row.score }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="openTime" label="开通时间" width="170" align="center" />
      </el-table>
      <el-empty v-if="!examLoading && exams.length === 0" description="暂无已开通考试" />
    </el-card>
  </div>
</template>

<script>
import { studentDetail, getStudentCourses, getStudentExams } from '@/api/student'

export default {
  name: 'StudentDetail',
  data() {
    return {
      loading: false,
      courseLoading: false,
      examLoading: false,
      student: {},
      courses: [],
      exams: []
    }
  },
  created() {
    const id = this.$route.params.id
    if (id) {
      this.fetchDetail(id)
      this.fetchCourses(id)
      this.fetchExams(id)
    }
  },
  methods: {
    fetchDetail(id) {
      this.loading = true
      studentDetail(id)
        .then((res) => {
          this.student = res.data.student || {}
        })
        .finally(() => {
          this.loading = false
        })
    },
    fetchCourses(id) {
      this.courseLoading = true
      getStudentCourses(id)
        .then((res) => {
          const data = res.data || {}
          this.courses = data.opened || data.openedList || data.list || []
        })
        .catch(() => {
          this.courses = []
        })
        .finally(() => {
          this.courseLoading = false
        })
    },
    fetchExams(id) {
      this.examLoading = true
      getStudentExams(id)
        .then((res) => {
          const data = res.data || {}
          this.exams = data.opened || data.openedList || data.list || []
        })
        .catch(() => {
          this.exams = []
        })
        .finally(() => {
          this.examLoading = false
        })
    },
    goBack() {
      this.$router.push('/student/list').catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.page-header {
  margin-bottom: 16px;
}

.info-card {
  margin-bottom: 16px;
}
</style>
