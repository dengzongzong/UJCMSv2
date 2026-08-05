<template>
  <div class="exam-intro-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <div class="intro-content" v-if="loaded && examInfo.id">
          <!-- 试卷名称 -->
          <div class="exam-header">
            <h1 class="exam-title">{{ examInfo.name }}</h1>
            <div class="exam-tags">
              <van-tag type="primary" size="medium">{{ examInfo.questionCount }}题</van-tag>
              <van-tag type="warning" size="medium">{{ examInfo.totalScore }}分</van-tag>
              <van-tag type="success" size="medium">{{ examInfo.duration }}分钟</van-tag>
            </div>
          </div>

          <div class="intro-layout">
            <div class="intro-main">
              <!-- 考试介绍 -->
              <div class="info-card">
                <div class="card-title">考试介绍</div>
                <div class="intro-text">{{ examInfo.description || examInfo.intro || '' }}</div>
              </div>

              <!-- 考试须知 -->
              <div class="info-card notice">
                <div class="card-title">
                  <van-icon name="warning-o" color="#ee0a24" />
                  考试须知
                </div>
                <ul class="notice-list">
                  <li>1. 考试时间为 {{ examInfo.duration }} 分钟，请合理安排答题时间。</li>
                  <li>2. 考试过程中退出会自动保存答题进度，下次可继续答题（断点续考）。</li>
                  <li>3. 倒计时结束将自动提交试卷，请注意答题进度。</li>
                  <li>4. 答题卡可查看已答和未答题目，绿色表示已答。</li>
                  <li>5. 考试结束后可查看成绩和答案解析。</li>
                </ul>
              </div>
            </div>

            <div class="intro-side">
              <!-- 倒计时 -->
              <div class="countdown-card">
                <van-icon name="clock-o" size="24" color="#fff" />
                <div class="countdown-info">
                  <div class="countdown-label">距离考试开始还有</div>
                  <div class="countdown-time" v-if="countdownText">{{ countdownText }}</div>
                  <div class="countdown-time ready" v-else>可以开始考试</div>
                </div>
              </div>

              <!-- 考试时间 -->
              <div class="info-card">
                <div class="card-title">考试时间</div>
                <div class="info-row">
                  <span class="info-label">开始时间</span>
                  <span class="info-value">{{ examInfo.startTime || '随时可考' }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">结束时间</span>
                  <span class="info-value">{{ examInfo.endTime || '长期有效' }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">考试时长</span>
                  <span class="info-value">{{ examInfo.duration }}分钟</span>
                </div>
              </div>

              <!-- 去考试按钮 -->
          <div class="action-bar">
            <van-button
              round
              block
              type="primary"
              size="large"
              :loading="starting"
              :disabled="!canStartExam"
              @click="handleStartExam"
            >
              {{ examStatusText }}
            </van-button>
          </div>
            </div>
          </div>
        </div>

        <van-empty v-else-if="loaded && !examInfo.id" description="试卷信息加载失败" />
        <van-loading v-else class="page-loading" size="24px" vertical>加载中...</van-loading>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getExamIntro, getFaceVerifyInfo } from '@/api/exam'
import { Dialog } from 'vant'

export default {
  name: 'ExamIntro',
  components: { Header },
  data() {
    return {
      examId: this.$route.params.id,
      examInfo: {},
      loaded: false,
      starting: false,
      countdownTimer: null,
      countdownText: '',
      faceVerifyEnabled: false
    }
  },
  computed: {
    canStartExam() {
      const now = Date.now()
      const startTime = this.examInfo.startTime ? new Date(this.examInfo.startTime).getTime() : null
      const endTime = this.examInfo.endTime ? new Date(this.examInfo.endTime).getTime() : null
      if (startTime && now < startTime) return false
      if (endTime && now > endTime) return false
      return true
    },
    examStatusText() {
      const now = Date.now()
      const startTime = this.examInfo.startTime ? new Date(this.examInfo.startTime).getTime() : null
      const endTime = this.examInfo.endTime ? new Date(this.examInfo.endTime).getTime() : null
      if (startTime && now < startTime) return '考试未开始'
      if (endTime && now > endTime) return '考试已结束'
      return '去考试'
    }
  },
  created() {
    this.fetchExamIntro()
    this.fetchFaceVerifyConfig()
  },
  beforeDestroy() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer)
    }
  },
  methods: {
    async fetchFaceVerifyConfig() {
      try {
        const res = await getFaceVerifyInfo(this.examId)
        const data = res.data || {}
        this.faceVerifyEnabled = !!data.enabled
        // 人脸比对已改为后端处理,前端无需预加载任何模型文件
      } catch (e) {
        this.faceVerifyEnabled = false
      }
    },
    async fetchExamIntro() {
      try {
        const res = await getExamIntro(this.examId)
        const data = res.data || res
        this.examInfo = data
        this.startCountdown()
      } catch (error) {
        // 失败时不做写死兜底，让 UI 走"加载失败"空状态
        const code = error && (error.code || (error.data && error.data.code))
        if (code === 1001) {
          Dialog.confirm({
            title: '请先登录',
            message: '考试介绍需要登录后才能查看,是否现在登录?',
            confirmButtonText: '去登录',
            cancelButtonText: '取消'
          })
            .then(() => this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } }))
            .catch(() => {})
        } else if (code === 1002) {
          Dialog.alert({
            title: '未开通该考试',
            message: (error && (error.message || error.msg)) || '请联系管理员先开通该考试。',
            confirmButtonText: '我知道了'
          }).catch(() => {})
        }
        this.examInfo = {}
        console.error('获取试卷介绍失败:', error)
      } finally {
        this.loaded = true
      }
    },
    startCountdown() {
      if (!this.examInfo.startTime) return
      // 后端 startTime 是 LocalDateTime (ISO 字符串)，Date 能直接解析
      const startTime = new Date(this.examInfo.startTime).getTime()
      if (isNaN(startTime)) return
      const updateCountdown = () => {
        const now = Date.now()
        const diff = startTime - now
        if (diff <= 0) {
          this.countdownText = ''
          clearInterval(this.countdownTimer)
          return
        }
        const days = Math.floor(diff / (1000 * 60 * 60 * 24))
        const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
        const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
        const secs = Math.floor((diff % (1000 * 60)) / 1000)
        if (days > 0) {
          this.countdownText = `${days}天 ${hours}时 ${mins}分`
        } else {
          this.countdownText = `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
        }
      }
      updateCountdown()
      this.countdownTimer = setInterval(updateCountdown, 1000)
    },
    handleStartExam() {
      const message = this.faceVerifyEnabled
        ? '考试前需要进行人脸识别验证，请确保光线充足。确认开始吗？'
        : '开始考试后，中途退出会自动保存进度，下次可继续答题（断点续考）。确认开始吗？'
      const confirmText = this.faceVerifyEnabled ? '开始验证' : '开始考试'

      Dialog.confirm({
        title: '确认开始考试',
        message: message,
        confirmButtonText: confirmText,
        confirmButtonColor: '#1989fa'
      }).then(() => {
        if (this.faceVerifyEnabled) {
          this.goToFaceVerify()
        } else {
          this.doStartExam()
        }
      }).catch(() => {})
    },
    goToFaceVerify() {
      var savedRecordId = localStorage.getItem('exam_record_' + this.examId)
      if (savedRecordId) {
        this.$router.push('/exam/face-verify/' + this.examId + '?recordId=' + savedRecordId).catch(() => {})
      } else {
        this.$router.push('/exam/face-verify/' + this.examId).catch(() => {})
      }
    },
    doStartExam() {
      // 不再调 startExam，直接跳转。Exam.vue 会统一调 getExamPaper（带 recordId）
      // 尝试从 localStorage 恢复上次未完成的 recordId，实现断点续考
      var savedRecordId = localStorage.getItem('exam_record_' + this.examId)
      if (savedRecordId) {
        this.$router.push('/exam/take/' + this.examId + '?recordId=' + savedRecordId).catch(() => {})
      } else {
        this.$router.push('/exam/take/' + this.examId).catch(() => {})
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.exam-intro-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-body {
  padding-top: var(--header-height, 170px);
}

.container {
  width: 80%;
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px 20px;
}

.page-loading {
  padding-top: 100px;
  text-align: center;
}

.exam-header {
  background: #fff;
  border-radius: 12px;
  padding: 28px 24px;
  margin-bottom: 20px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  .exam-title {
    font-size: 24px;
    font-weight: bold;
    color: #333;
    margin-bottom: 14px;
  }

  .exam-tags {
    display: flex;
    justify-content: center;
    gap: 8px;
  }
}

.intro-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.intro-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.intro-side {
  flex: 0 0 320px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 80px;
}

.countdown-card {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #1989fa, #4ba7f7);
  border-radius: 12px;
  padding: 20px;
  color: #fff;
  box-shadow: 0 4px 16px rgba(25, 137, 250, 0.2);

  .countdown-info {
    margin-left: 12px;

    .countdown-label {
      font-size: 13px;
      opacity: 0.9;
      margin-bottom: 4px;
    }

    .countdown-time {
      font-size: 24px;
      font-weight: bold;

      &.ready {
        font-size: 18px;
      }
    }
  }
}

.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  .card-title {
    font-size: 16px;
    font-weight: bold;
    color: #333;
    margin-bottom: 14px;
    padding-left: 10px;
    border-left: 4px solid #1989fa;
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .info-row {
    display: flex;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 1px solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .info-label {
      font-size: 14px;
      color: #999;
    }

    .info-value {
      font-size: 14px;
      color: #333;
    }
  }

  .intro-text {
    font-size: 14px;
    color: #666;
    line-height: 1.9;
  }

  &.notice {
    .notice-list {
      li {
        font-size: 14px;
        color: #666;
        line-height: 2;
        list-style: none;
      }
    }
  }
}

.action-bar {
  padding: 8px 0;

  .van-button {
    height: 46px;
    font-size: 16px;
    font-weight: 500;
  }
}

@media (max-width: 992px) {
  .intro-layout {
    flex-direction: column;
  }

  .intro-side {
    flex: 1 1 100%;
    width: 100%;
    position: static;
  }
}
</style>
