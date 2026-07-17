<template>
  <div class="exam-list-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <div class="page-title">
          <span class="title-text">考试中心</span>
        </div>

        <div class="search-bar">
          <van-search
            v-model="searchKeyword"
            placeholder="搜索考试名称"
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
          text="您当前未登录,可以浏览考试列表;点击具体考试时若未开通会要求登录。"
          style="margin-bottom: 16px"
        />

        <div
          v-for="(group, categoryName) in groupedExams"
          :key="categoryName"
          class="exam-group"
        >
          <div class="group-title">
            <span class="title-bar"></span>
            {{ categoryName }}
          </div>
          <div class="exam-grid">
            <div
              v-for="exam in group"
              :key="exam.id"
              class="exam-card"
              :class="{ 'exam-locked': !exam.purchased }"
              @click="goExam(exam)"
            >
              <div class="exam-cover">
                <img :src="resolveImg(exam.coverUrl || exam.coverImage || defaultCover)" :alt="exam.name" />
                <div v-if="!exam.purchased" class="locked-tag">
                  <van-icon name="lock" /> 未开通
                </div>
                <div v-if="exam.purchased && exam.lastScore !== null && exam.lastScore !== undefined" class="last-score">
                  上次:{{ exam.lastScore }} 分
                </div>
                <div v-if="exam.purchased" class="exam-mask">
                  <van-icon name="play-circle-o" size="40" color="#fff" />
                </div>
              </div>
              <div class="exam-info">
                <div class="exam-name">{{ exam.name }}</div>
                <div class="exam-meta">
                  <span class="meta-tag">
                    <van-icon name="question-o" size="14" />
                    {{ exam.questionCount }}题
                  </span>
                  <span class="meta-tag">
                    <van-icon name="gem-o" size="14" />
                    {{ exam.totalScore }}分
                  </span>
                  <span class="meta-tag">
                    <van-icon name="clock-o" size="14" />
                    {{ exam.duration }}分钟
                  </span>
                  <span class="meta-tag">
                    <van-icon name="friends-o" size="14" />
                    已有 {{ exam.examCount || 0 }} 人考过
                  </span>
                </div>
                <div class="exam-footer">
                  <span class="last-time" v-if="exam.lastTime || exam.lastExamTime">
                    上次考试:{{ exam.lastTime || exam.lastExamTime }}
                  </span>
                  <van-button
                    class="exam-action-btn"
                    size="small"
                    :type="exam.purchased ? 'primary' : 'warning'"
                    round
                  >
                    <van-icon :name="exam.purchased ? 'play' : 'lock'" />
                    {{ exam.purchased ? (exam.lastScore !== null && exam.lastScore !== undefined ? '再次考试' : '去考试') : '未开通' }}
                  </van-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <van-empty v-if="examList.length === 0 && !loading" description="暂无考试" />
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getPublicExamList, checkExamAccess } from '@/api/exam'
import { resolveImg } from '@/utils/apiBase'
import { Toast, Dialog } from 'vant'

export default {
  name: 'ExamCenter',
  components: { Header },
  data() {
    return {
      examList: [],
      loading: false,
      // 搜索关键词
      searchKeyword: '',
      searchTimer: null,
      defaultCover: 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"><rect fill="#1989fa" width="100" height="100"/><text x="50%" y="50%" text-anchor="middle" dy=".3em" fill="#fff" font-size="14">考试</text></svg>')
    }
  },
  computed: {
    loggedIn() {
      return !!this.$store.getters.token
    },
    // 按 exam.category 对考试列表分组(category 为空时归到"通用考试"组)
    groupedExams() {
      const groups = {}
      this.examList.forEach(exam => {
        const key = exam.category || '通用考试'
        if (!groups[key]) groups[key] = []
        groups[key].push(exam)
      })
      return groups
    }
  },
  watch: {
    // 输入关键词时防抖 300ms 触发搜索
    searchKeyword() {
      if (this.searchTimer) clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => {
        this.fetchExamList()
      }, 300)
    }
  },
  created() {
    this.fetchExamList()
  },
  methods: {
    resolveImg,
    onSearch() {
      // 点击搜索按钮或回车时立即触发,取消待执行的防抖
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
        this.searchTimer = null
      }
      this.fetchExamList()
    },
    async fetchExamList() {
      this.loading = true
      try {
        // 考试中心按分类分组展示所有考试,支持关键词搜索
        const res = await getPublicExamList(undefined, undefined, this.searchKeyword)
        const data = res.data || res
        this.examList = Array.isArray(data) ? data : (data.list || data.records || [])
      } catch (error) {
        this.examList = []
      } finally {
        this.loading = false
      }
    },
    /**
     * 点击考试卡片: 走权限闸门
     * - 未登录(后端 1001): 引导去登录
     * - 已登录但未开通(后端 1002): 弹出提示
     * - 已开通: 进入介绍页
     */
    async goExam(exam) {
      // 先做客户端预校验,减少一次请求;但最终以服务端为准
      if (!this.loggedIn) {
        Dialog.confirm({
          title: '需要先登录',
          message: '考试中心展示所有考试,但您当前未登录,登录后若已开通即可参加考试。是否现在登录?',
          confirmButtonText: '去登录',
          cancelButtonText: '取消'
        })
          .then(() => {
            this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
          })
          .catch(() => {})
        return
      }
      if (!exam.purchased) {
        Dialog.alert({
          title: '未开通该考试',
          message: '请联系管理员先开通该考试,开通后即可参加。',
          confirmButtonText: '我知道了'
        }).catch(() => {})
        return
      }
      // 已开通:再次做服务端校验(防止前端状态过期)
      try {
        await checkExamAccess(exam.id)
        this.$router.push(`/exam/intro/${exam.id}`).catch(() => {})
      } catch (e) {
        // 异常时,根据 code 处理
        const code = e && (e.code || (e.data && e.data.code))
        if (code === 1001) {
          Dialog.confirm({
            title: '登录已过期',
            message: '请重新登录后访问该考试。',
            confirmButtonText: '去登录',
            cancelButtonText: '取消'
          })
            .then(() => this.$store.dispatch('logout').then(() => this.$router.replace('/login')))
            .catch(() => {})
        } else if (code === 1002) {
          Dialog.alert({
            title: '未开通该考试',
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
.exam-list-page { min-height: 100vh; background-color: #f5f5f5; }
.page-body { padding-top: var(--header-height, 170px); }
.container { width: 80%; max-width: 1600px; margin: 0 auto; padding: 24px 20px; }
.page-title {
  display: flex; align-items: center; gap: 12px; margin-bottom: 20px;
  .title-text { font-size: 22px; font-weight: 600; color: #303133; }
  .title-sub { font-size: 13px; color: #909399; }
}
.search-bar {
  margin-bottom: 16px;
  .search-action { color: #1989fa; font-size: 14px; padding: 0 8px; }
}
.exam-group { margin-bottom: 32px; }
.group-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 18px; font-weight: 600; color: #303133;
  margin-bottom: 16px; padding-left: 4px;
  .title-bar {
    width: 4px; height: 20px; background: #1989fa; border-radius: 2px;
  }
  .group-count { font-size: 13px; color: #909399; font-weight: normal; }
}
.exam-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}
.exam-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s;
  display: flex;
  flex-direction: column;

  &:hover { transform: translateY(-3px); box-shadow: 0 6px 20px rgba(25, 137, 250, 0.15); }

  .exam-cover {
    position: relative;
    width: 100%;
    height: 140px;
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
    .last-score {
      position: absolute;
      top: 8px;
      right: 8px;
      background: rgba(25, 137, 250, 0.9);
      color: #fff;
      font-size: 12px;
      padding: 3px 8px;
      border-radius: 3px;
    }
    .exam-mask {
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
  &:hover .exam-mask { opacity: 1; }

  .exam-info { padding: 12px 14px 14px; display: flex; flex-direction: column; gap: 8px; flex: 1; }
  .exam-name {
    font-size: 14px; font-weight: 600; color: #303133; line-height: 1.4;
    overflow: hidden; text-overflow: ellipsis; display: -webkit-box;
    -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  }
  .exam-meta {
    display: flex; flex-wrap: wrap; gap: 6px 10px; font-size: 12px; color: #909399;
    .meta-tag { display: inline-flex; align-items: center; gap: 2px; }
  }
  .exam-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: auto;
    .last-time { font-size: 12px; color: #c0c4cc; }
  }
  /* “再次考试/去考试/未开通”按钮加宽，避免文字被截断 */
  .exam-action-btn {
    min-width: 108px;
    padding-left: 18px;
    padding-right: 18px;
    white-space: nowrap;
  }
}
.exam-locked {
  opacity: 0.85;
  .exam-name { color: #606266; }
}
@media (max-width: 1400px) { .exam-grid { grid-template-columns: repeat(5, 1fr); } }
@media (max-width: 1100px) { .exam-grid { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 800px) { .exam-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 540px) { .exam-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 380px) { .exam-grid { grid-template-columns: 1fr; } }
</style>
