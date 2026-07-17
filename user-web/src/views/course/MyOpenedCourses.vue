<template>
  <div class="my-courses-page">
    <Header />
    <div class="page-body">
      <div class="container">
        <div class="page-title">
          <h2>我的课程</h2>
          <p>您已开通的课程</p>
        </div>
        <van-search v-model="searchKeyword" placeholder="搜索课程名称" shape="round" @search="onSearch" @clear="onSearch" />
        <div v-if="loading" class="loading-wrapper">
          <van-loading size="24px">加载中...</van-loading>
        </div>
        <div v-else-if="filteredCourses.length > 0" class="course-list">
          <div v-for="course in filteredCourses" :key="course.id" class="course-card" @click="goDetail(course.id)">
            <div class="course-cover">
              <img v-if="course.coverUrl" :src="resolveImg(course.coverUrl)" alt="" />
              <div v-else class="cover-default"><van-icon name="photo-o" size="32" /></div>
            </div>
            <div class="course-info">
              <div class="course-name">{{ course.name }}</div>
              <div class="course-meta">
                <span v-if="course.professionName"><van-icon name="bookmark-o" /> {{ course.professionName }}</span>
                <span v-if="course.sectionCount"><van-icon name="description" /> {{ course.sectionCount }}节</span>
                <span v-if="course.totalDuration && course.totalDuration > 0"><van-icon name="clock-o" /> {{ formatDuration(course.totalDuration) }}</span>
              </div>
              <div class="course-progress" v-if="course.progress != null">
                <van-progress :percentage="course.progress" color="#1989fa" />
              </div>
            </div>
            <van-icon name="arrow" class="arrow-icon" />
          </div>
        </div>
        <van-empty v-else description="暂无已开通课程" />
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getMyCourses } from '@/api/course'
import { resolveImg } from '@/utils/apiBase'

export default {
  name: 'MyOpenedCourses',
  components: { Header },
  data() {
    return {
      courseList: [],
      loading: false,
      searchKeyword: ''
    }
  },
  computed: {
    filteredCourses() {
      if (!this.searchKeyword) return this.courseList
      const kw = this.searchKeyword.toLowerCase()
      return this.courseList.filter(c => c.name && c.name.toLowerCase().includes(kw))
    }
  },
  methods: {
    resolveImg,
    formatDuration(seconds) {
      if (!seconds || seconds <= 0) return '0分钟'
      var minutes = Math.floor(seconds / 60)
      if (minutes < 60) return minutes + '分钟'
      var hours = Math.floor(minutes / 60)
      var mins = minutes % 60
      return mins > 0 ? hours + '小时' + mins + '分钟' : hours + '小时'
    },
    async fetchCourses() {
      this.loading = true
      try {
        const res = await getMyCourses()
        const data = res.data || res
        this.courseList = Array.isArray(data) ? data : (data.list || data.records || [])
      } catch (error) {
        this.courseList = []
      } finally {
        this.loading = false
      }
    },
    onSearch() { /* computed 自动过滤 */ },
    goDetail(id) {
      this.$router.push(`/course/detail/${id}`).catch(() => {})
    }
  },
  created() {
    this.fetchCourses()
  }
}
</script>

<style lang="scss" scoped>
.my-courses-page { min-height: 100vh; background: #f5f5f5; }
.page-body { padding-top: var(--header-height, 170px); }
.container { width: 80%; max-width: 1600px; margin: 0 auto; padding: 24px 20px; }
.page-title {
  margin-bottom: 16px;
  h2 { font-size: 20px; color: #333; margin: 0 0 4px; }
  p { font-size: 13px; color: #999; margin: 0; }
}
.loading-wrapper { display: flex; justify-content: center; padding: 40px 0; }
.course-list { display: flex; flex-direction: column; gap: 12px; }
.course-card {
  display: flex; align-items: center; background: #fff; border-radius: 10px;
  padding: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  .course-cover {
    width: 80px; height: 60px; border-radius: 6px; overflow: hidden; flex-shrink: 0;
    img { width: 100%; height: 100%; object-fit: cover; }
    .cover-default { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f0f0f0; color: #ccc; }
  }
  .course-info { flex: 1; margin-left: 12px; min-width: 0;
    .course-name { font-size: 15px; font-weight: 500; color: #333; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .course-meta { display: flex; gap: 12px; font-size: 12px; color: #999;
      span { display: flex; align-items: center; gap: 3px; }
    }
    .course-progress { margin-top: 8px; }
  }
  .arrow-icon { color: #c8c9cc; flex-shrink: 0; }
}
</style>
