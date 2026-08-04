<template>
  <div class="my-courses-page">
    <Header @subject-change="fetchCourses" />

    <div class="page-body">
      <div class="container">
        <div class="page-title">
          <span class="title-text">学习中心</span>
        </div>

        <div class="search-bar">
          <van-search
            v-model="searchKeyword"
            placeholder="搜索课程名称"
            show-action
            clearable
            @search="onSearch"
          >
            <template #action>
              <div class="search-action" @click="onSearch">搜索</div>
            </template>
          </van-search>
        </div>

        <van-notice-bar
          v-if="!loggedIn"
          left-icon="info-o"
          color="#1989fa"
          background="#ecf5ff"
          text="您当前未登录,可以浏览所有课程;点击具体课程时若未开通会要求登录。"
          style="margin-bottom: 16px"
        />

        <!-- 课程列表:按视频分类分组展示 -->
        <van-list
          v-model="loading"
          :finished="finished"
          :immediate-check="false"
          finished-text="没有更多了"
          @load="onLoad"
        >
          <div v-if="groupedCourses.length > 0">
            <div
              v-for="group in groupedCourses"
              :key="group.name"
              class="course-group"
            >
              <div class="group-title">
                <span class="group-name">{{ group.name }}</span>
              </div>
              <div class="course-grid">
                <div
                  v-for="course in group.courses"
                  :key="course.id"
                  class="course-card"
                  :class="{ 'course-locked': !course.purchased }"
                >
                  <!-- 封面图 -->
                  <div class="card-cover" @click="goDetail(course)">
                    <img :src="resolveImg(course.coverUrl || course.coverImage || defaultCover)" :alt="course.name" />
                    <div v-if="!course.purchased" class="locked-tag">
                      <van-icon name="lock" /> 未开通
                    </div>
                    <div v-if="course.purchased" class="cover-mask">
                      <van-icon name="play-circle-o" size="40" color="#fff" />
                    </div>
                  </div>
                  <!-- 卡片内容 -->
                  <div class="card-content">
                    <!-- 课程标题 -->
                    <div class="card-name" @click="goDetail(course)">{{ course.name }}</div>
                    <!-- 已开通时显示学习进度 -->
                    <div v-if="course.purchased" class="card-progress">
                      <van-progress
                        :percentage="course.progress || 0"
                        color="#1989fa"
                        :show-pivot="false"
                        stroke-width="3"
                      />
                      <span class="progress-text">已学习 {{ course.progress || 0 }}%</span>
                    </div>
                    <!-- 学习数据:已有多少人学过 + 学时 -->
                    <div class="card-stats">
                      <van-icon name="friends-o" size="14" color="#909399" />
                      <span>已有 {{ course.studyCount || 0 }} 人学过</span>
                      <span class="stat-divider">·</span>
                      <span>学时：{{ course.studyHours || 0 }}</span>
                    </div>
                    <!-- 底部:价格 + 查看详情按钮 -->
                    <div class="card-footer">
                      <div class="card-price">
                        <span v-if="course.price > 0" class="price-value">¥{{ course.price }}</span>
                        <span v-else class="price-free">免费</span>
                      </div>
                      <van-button
                        size="small"
                        type="danger"
                        round
                        icon="eye-o"
                        @click="goDetail(course)"
                      >
                        查看详情
                      </van-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <van-empty v-else-if="!loading" description="暂无课程" />
        </van-list>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getPublicCourseList, checkCourseAccess } from '@/api/course'
import { getVideoCategories } from '@/api/home'
import { resolveImg } from '@/utils/apiBase'
import { Toast, Dialog } from 'vant'

export default {
  name: 'CourseCenter',
  components: { Header },
  data() {
    return {
      courseList: [],
      videoCategories: [],
      loading: false,
      finished: false,
      page: 1,
      pageSize: 50,
      // 搜索关键词
      searchKeyword: '',
      searchTimer: null,
      defaultCover: 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="120" height="80"><rect fill="#e8e8e8" width="120" height="80"/><text x="50%" y="50%" text-anchor="middle" dy=".3em" fill="#999" font-size="12">课程封面</text></svg>')
    }
  },
  computed: {
    loggedIn() {
      return !!this.$store.getters.token
    },
    /**
     * 将课程列表按 categoryName 分组展示:
     * - categoryName 为空/null 的课程归入 "其他课程" 组(仅当有未分类课程时显示)
     * - 分组顺序优先按 videoCategories 的 sort 顺序
     */
    groupedCourses() {
      const OTHER = '其他课程'
      const buckets = {}
      let hasOther = false
      this.courseList.forEach((course) => {
        let name = course.categoryName
        if (!name || !String(name).trim()) {
          name = OTHER
          hasOther = true
        }
        if (!buckets[name]) {
          buckets[name] = []
        }
        buckets[name].push(course)
      })
      const ordered = []
      // 先按后台配置的分类顺序输出(命中的)
      this.videoCategories.forEach((vc) => {
        if (buckets[vc.name] && buckets[vc.name].length > 0) {
          ordered.push({ name: vc.name, courses: buckets[vc.name] })
          delete buckets[vc.name]
        }
      })
      // 追加任何未在配置中的分类名(有分类名的课程直接展示,不再归入"其他课程")
      Object.keys(buckets).forEach((name) => {
        ordered.push({ name, courses: buckets[name] })
      })
      return ordered
    }
  },
  watch: {
    // 输入关键词时防抖 300ms 触发搜索
    searchKeyword() {
      if (this.searchTimer) clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => {
        this.fetchCourses()
      }, 300)
    }
  },
  created() {
    this.fetchVideoCategories()
    this.fetchCourses()
  },
  methods: {
    resolveImg,
    // 检查 token 是否存在且未过期(JWT payload 中 exp 字段)
    isTokenValid() {
      const token = this.$store.getters.token
      if (!token) return false
      try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        // exp 是秒级时间戳
        if (payload.exp && payload.exp < Date.now() / 1000) {
          return false
        }
        return true
      } catch (e) {
        return false
      }
    },
    formatDuration(seconds) {
      if (!seconds || seconds <= 0) return '0分钟'
      var minutes = Math.floor(seconds / 60)
      if (minutes < 60) return minutes + '分钟'
      var hours = Math.floor(minutes / 60)
      var mins = minutes % 60
      return mins > 0 ? hours + '小时' + mins + '分钟' : hours + '小时'
    },
    onSearch() {
      // 点击搜索按钮或回车时立即触发,取消待执行的防抖
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
        this.searchTimer = null
      }
      this.fetchCourses()
    },
    async fetchVideoCategories() {
      try {
        const res = await getVideoCategories()
        const data = res.data || res
        this.videoCategories = Array.isArray(data) ? data : []
      } catch (e) {
        this.videoCategories = []
      }
    },
    /**
     * 重置列表并加载第一页(搜索/初始加载/切换专业时调用)
     */
    async fetchCourses() {
      // 每次重新加载递增序号, 用于丢弃过期请求(避免并发搜索导致课程重复)
      this._fetchSeq = (this._fetchSeq || 0) + 1
      const seq = this._fetchSeq
      this.page = 1
      this.courseList = []
      this.finished = false
      this.loading = true
      await this.loadData(seq)
    },
    /**
     * van-list 滚动到底部时加载下一页
     */
    async onLoad() {
      this.page++
      await this.loadData(this._fetchSeq)
    },
    /**
     * 实际加载分页数据并追加到 courseList
     */
    async loadData(seq) {
      try {
        // 未登录时不带专业条件(查所有);登录后按 Header 选的专业过滤;支持关键词搜索
        let professionId
        if (this.loggedIn) {
          const subject = this.$store.getters.currentSubject
          if (subject) {
            professionId = subject.professionId || subject.id
          }
        }
        const res = await getPublicCourseList(professionId, this.searchKeyword, this.page, this.pageSize)
        // 期间发生了新的搜索/切换, 丢弃过期响应, 避免课程重复或错乱
        if (seq !== undefined && seq !== this._fetchSeq) return
        const data = res.data || res
        const records = Array.isArray(data) ? data : (data.list || data.records || [])
        this.courseList.push(...records)
        if (data.total !== undefined) {
          this.finished = this.courseList.length >= data.total
        } else {
          this.finished = records.length < this.pageSize
        }
      } catch (error) {
        if (seq !== undefined && seq !== this._fetchSeq) return
        this.courseList = []
        this.finished = true
      } finally {
        if (seq === undefined || seq === this._fetchSeq) {
          this.loading = false
        }
      }
    },
    async goDetail(course) {
      // 客户端预校验: 检查 token 是否存在
      if (!this.loggedIn) {
        Dialog.confirm({
          title: '需要先登录',
          message: '课程中心展示所有课程,但您当前未登录,登录后若已开通即可学习。是否现在登录?',
          confirmButtonText: '去登录',
          cancelButtonText: '取消'
        })
          .then(() => {
            this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
          })
          .catch(() => {})
        return
      }
      // token 存在但可能已过期: 用 silent 模式调用,不让全局拦截器自动跳转
      // 已开通的课程: 服务端再次校验
      try {
        await checkCourseAccess(course.id, { silent: true })
        this.$router.push(`/course/detail/${course.id}`).catch(() => {})
      } catch (e) {
        const code = e && (e.code || (e.data && e.data.code))
        // 401/1001: token 过期或无效,弹出友好提示让用户选择是否登录
        if (code === 401 || code === 1001) {
          Dialog.confirm({
            title: '登录已过期',
            message: '您的登录状态已过期,请重新登录后访问该课程。是否现在登录?',
            confirmButtonText: '去登录',
            cancelButtonText: '取消'
          })
            .then(() => this.$store.dispatch('logout').then(() => this.$router.replace('/login')))
            .catch(() => {})
        } else if (code === 1002) {
          Dialog.alert({
            title: '未开通该课程',
            message: (e && (e.message || e.msg)) || '请联系管理员先开通。',
            confirmButtonText: '我知道了'
          }).catch(() => {})
        } else {
          Toast((e && (e.message || e.msg)) || '访问失败')
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.my-courses-page { min-height: 100vh; background-color: #f5f5f5; }
.page-body { padding-top: var(--header-height, 170px); }
.container { width: 80%; max-width: 1600px; margin: 0 auto; padding: 24px 20px; }

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  .title-text { font-size: 22px; font-weight: 600; color: #303133; }
}
.search-bar {
  margin-bottom: 16px;
  .search-action { color: #1989fa; font-size: 14px; padding: 0 8px; }
}

.course-group {
  margin-bottom: 28px;
  &:last-child { margin-bottom: 0; }
}
.group-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  padding-left: 10px;
  border-left: 4px solid #1989fa;
  .group-name { font-size: 17px; font-weight: 600; color: #303133; }
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.course-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s;
  display: flex;
  flex-direction: column;

  &:hover { transform: translateY(-3px); box-shadow: 0 6px 20px rgba(25, 137, 250, 0.15); }

  .card-cover {
    position: relative;
    width: 100%;
    height: 160px;
    background: #f5f7fa;
    overflow: hidden;
    img { width: 100%; height: 100%; object-fit: cover; }
    .locked-tag {
      position: absolute;
      top: 8px;
      left: 8px;
      background: rgba(245, 108, 108, 0.92);
      color: #fff;
      font-size: 12px;
      padding: 3px 8px;
      border-radius: 3px;
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
    .cover-mask {
      position: absolute;
      inset: 0;
      background: rgba(0, 0, 0, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.2s;
    }
  }
  &:hover .cover-mask { opacity: 1; }

  .card-content {
    padding: 12px 14px 14px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    flex: 1;
  }
  .card-name {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    cursor: pointer;
    &:hover { color: #1989fa; }
  }
  .card-progress {
    .progress-text { font-size: 12px; color: #909399; margin-top: 2px; }
  }
  .card-stats {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
    font-size: 12px;
    color: #909399;
    .stat-divider { margin: 0 2px; color: #c0c4cc; }
  }
  .card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: auto;
    padding-top: 4px;
    .card-price {
      .price-value {
        font-size: 18px;
        font-weight: bold;
        color: #ee0a24;
      }
      .price-free {
        font-size: 15px;
        font-weight: 600;
        color: #07c160;
      }
    }
  }
}
.course-locked {
  opacity: 0.85;
  .card-name { color: #606266; }
}
@media (max-width: 1100px) { .course-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 800px) { .course-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 540px) { .course-grid { grid-template-columns: 1fr; } }

/* 移动端适配 */
@media (max-width: 768px) {
  .container { width: 100%; padding: 12px; }
  .page-title .title-text { font-size: 18px; }
  .group-title .group-name { font-size: 15px; }
  .course-card {
    .card-cover { height: 100px; }
    .card-content { padding: 8px 10px; }
    .card-name { font-size: 13px; }
    .card-footer .van-button { font-size: 12px; min-width: auto; }
  }
}
</style>
