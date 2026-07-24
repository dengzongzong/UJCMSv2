<template>
  <div class="my-exams-page" :class="{ 'embedded-mode': embedded }">
    <Header v-if="!embedded" />
    <div :class="embedded ? 'embedded-body' : 'page-body'">
      <div :class="embedded ? 'embedded-container' : 'container'">
        <div class="page-title">
          <h2>我的考试</h2>
          <p>您已开通的考试</p>
        </div>
        <van-search v-model="searchKeyword" placeholder="搜索考试名称" shape="round" @search="onSearch" @clear="onSearch" />
        <van-list
          v-model="loading"
          :finished="finished"
          :immediate-check="false"
          finished-text="没有更多了"
          @load="onLoad"
        >
          <div v-if="examList.length > 0" class="exam-list">
            <div v-for="exam in examList" :key="exam.id" class="exam-card" @click="goExam(exam)">
              <div class="exam-cover">
                <img v-if="exam.coverUrl" :src="apiUrl(exam.coverUrl)" alt="" />
                <div v-else class="cover-default"><van-icon name="notes-o" size="32" /></div>
              </div>
              <div class="exam-info">
                <div class="exam-name">{{ exam.name }}</div>
                <div class="exam-meta">
                  <span v-if="exam.category"><van-icon name="label-o" /> {{ exam.category }}</span>
                  <span><van-icon name="clock-o" /> {{ exam.duration }}分钟</span>
                  <span><van-icon name="gold-coin-o" /> {{ exam.totalScore }}分</span>
                </div>
                <div class="exam-status">
                  <span v-if="exam.lastScore != null" :class="exam.lastScore >= 60 ? 'score-pass' : 'score-fail'">
                    上次成绩: {{ exam.lastScore }}分
                  </span>
                  <span v-else class="not-taken">尚未考试</span>
                </div>
              </div>
              <van-icon name="arrow" class="arrow-icon" />
            </div>
          </div>
          <van-empty v-if="examList.length === 0 && !loading" description="暂无已开通考试" />
        </van-list>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getMyExams } from '@/api/exam'
import { apiUrl } from '@/utils/apiBase'

export default {
  name: 'MyExams',
  components: { Header },
  props: {
    // 内嵌模式：在个人中心右侧内容区直接展示，不渲染独立页头与外层 page-body
    embedded: { type: Boolean, default: false }
  },
  data() {
    return {
      examList: [],
      loading: false,
      finished: false,
      page: 1,
      pageSize: 50,
      searchKeyword: '',
      searchTimer: null
    }
  },
  watch: {
    searchKeyword() {
      if (this.searchTimer) clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => {
        this.fetchExams()
      }, 300)
    }
  },
  methods: {
    apiUrl,
    onSearch() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
        this.searchTimer = null
      }
      this.fetchExams()
    },
    /**
     * 重置列表并加载第一页(搜索/初始加载时调用)
     */
    async fetchExams() {
      this.page = 1
      this.examList = []
      this.finished = false
      this.loading = true
      await this.loadData()
    },
    /**
     * van-list 滚动到底部时加载下一页
     */
    async onLoad() {
      this.page++
      await this.loadData()
    },
    /**
     * 实际加载分页数据并追加到 examList
     */
    async loadData() {
      try {
        var params = { page: this.page, pageSize: this.pageSize }
        if (this.searchKeyword) params.keyword = this.searchKeyword
        const res = await getMyExams(params)
        const data = res.data || res
        const records = Array.isArray(data) ? data : (data.records || data.list || [])
        this.examList.push(...records)
        if (data.total !== undefined) {
          this.finished = this.examList.length >= data.total
        } else {
          this.finished = records.length < this.pageSize
        }
      } catch (error) {
        this.finished = true
        this.$toast.fail((error && error.message) || '加载失败,请稍后重试')
      } finally {
        this.loading = false
      }
    },
    goExam(exam) {
      this.$router.push(`/exam/intro/${exam.id}`).catch(() => {})
    }
  },
  created() {
    this.fetchExams()
  }
}
</script>

<style lang="scss" scoped>
.my-exams-page { min-height: 100vh; background: #f5f5f5; }
.page-body { padding-top: var(--header-height, 170px); }
.container { width: 80%; max-width: 1600px; margin: 0 auto; padding: 24px 20px; }
/* 内嵌模式：撑满个人中心右侧内容区，去掉固定页头留白与宽度限制 */
.my-exams-page.embedded-mode { min-height: auto; background: transparent; }
.embedded-body { padding-top: 0; }
.embedded-container { width: 100%; max-width: none; margin: 0; padding: 0; }
.page-title {
  margin-bottom: 16px;
  h2 { font-size: 20px; color: #333; margin: 0 0 4px; }
  p { font-size: 13px; color: #999; margin: 0; }
}
.loading-wrapper { display: flex; justify-content: center; padding: 40px 0; }
.exam-list { display: flex; flex-direction: column; gap: 12px; }
.exam-card {
  display: flex; align-items: center; background: #fff; border-radius: 10px;
  padding: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  .exam-cover {
    width: 80px; height: 60px; border-radius: 6px; overflow: hidden; flex-shrink: 0;
    img { width: 100%; height: 100%; object-fit: cover; }
    .cover-default { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #e8f3ff; color: #1989fa; }
  }
  .exam-info { flex: 1; margin-left: 12px; min-width: 0;
    .exam-name { font-size: 15px; font-weight: 500; color: #333; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .exam-meta { display: flex; gap: 12px; font-size: 12px; color: #999;
      span { display: flex; align-items: center; gap: 3px; }
    }
    .exam-status { margin-top: 6px; font-size: 12px;
      .score-pass { color: #07c160; }
      .score-fail { color: #ee0a24; }
      .not-taken { color: #ff976a; }
    }
  }
  .arrow-icon { color: #c8c9cc; flex-shrink: 0; }
}
</style>
