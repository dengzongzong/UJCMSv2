<template>
  <div class="exam-result-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <!-- 默认显示：简洁的附件界面 -->
        <div v-if="!showDetail" class="attachment-view">
          <!-- 顶部蓝色区域 -->
          <div class="attachment-header" :class="resultLevel">
            <div class="student-name-bar" v-if="resultInfo.studentName">
              <van-icon name="user-circle-o" size="16" />
              <span>{{ resultInfo.studentName }}</span>
            </div>
            <!-- 圆形进度环 -->
            <div class="score-ring-wrapper">
              <svg class="score-ring" width="140" height="140" viewBox="0 0 140 140">
                <circle class="ring-bg" cx="70" cy="70" r="60" fill="none" stroke="rgba(255,255,255,0.2)" stroke-width="8" />
                <circle class="ring-fg" cx="70" cy="70" r="60" fill="none" stroke="#fff" stroke-width="8"
                  stroke-linecap="round"
                  :stroke-dasharray="ringDashArray"
                  stroke-dashoffset="0"
                  transform="rotate(-90 70 70)"
                />
              </svg>
              <div class="score-center">
                <div class="score-value">{{ resultInfo.score || 0 }}</div>
                <div class="score-label">分</div>
              </div>
            </div>
            <!-- 重测按钮 -->
            <div class="retry-btn" v-if="resultInfo.allowRetry === 1" @click="handleRetry">
              <van-icon name="replay" size="14" /> 重测
            </div>
            <van-tag v-else :type="isPassed ? 'success' : 'danger'" size="large" round class="pass-tag">
              {{ isPassed ? '恭喜通过' : '未通过' }}
            </van-tag>
            <div class="tip-text">最终成绩待阅卷后公布，请保存成绩码</div>
          </div>

          <!-- 统计信息 -->
          <div class="stats-grid stats-grid-4">
            <div class="stat-item">
              <div class="stat-label">阅卷前分数</div>
              <div class="stat-value">{{ resultInfo.score || '--' }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">用时</div>
              <div class="stat-value">{{ formattedDuration }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">交卷时间</div>
              <div class="stat-value">{{ resultInfo.submitTime || '--' }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">总分/及格分</div>
              <div class="stat-value">{{ resultInfo.totalScore || '--' }}/{{ resultInfo.passScore || 60 }}</div>
            </div>
          </div>

          <!-- 二维码区域 -->
          <div class="qr-section">
            <div class="qr-tip">请截屏保存二维码方便查成绩</div>
            <div class="qr-code">
              <img :src="qrCodeUrl" alt="成绩二维码" />
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-links">
            <div class="action-item" @click="showDetail = true">
              <span>查看详情</span>
              <van-icon name="arrow" />
            </div>
            <div class="action-item" @click="$router.push('/exam/records')">
              <span>查看历史成绩</span>
              <van-icon name="arrow" />
            </div>
          </div>

          <!-- 返回按钮 -->
          <div class="back-btn-wrapper">
            <van-button type="primary" block round size="large" @click="$router.push('/exam')">
              返回
            </van-button>
          </div>
        </div>

        <!-- 详情展开：完整考试结果 -->
        <div v-else class="detail-view">
          <!-- 返回简洁视图 -->
          <div class="back-to-attachment" @click="showDetail = false">
            <van-icon name="arrow-left" />
            <span>返回成绩页</span>
          </div>

          <!-- 成绩概览 -->
          <div class="result-summary" :class="resultLevel">
            <div class="summary-left">
              <div class="score-circle">
                <div class="score-value">{{ resultInfo.score || 0 }}</div>
                <div class="score-label">分</div>
              </div>
              <div class="pass-status">
                <van-tag :type="isPassed ? 'success' : 'danger'" size="large" round>
                  {{ isPassed ? '恭喜通过' : '未通过' }}
                </van-tag>
              </div>
            </div>
            <div class="summary-right">
              <div class="exam-title">{{ resultInfo.examName || '考试结果' }}</div>
              <div class="student-name-detail" v-if="resultInfo.studentName">
                <van-icon name="user-circle-o" size="14" />
                <span>{{ resultInfo.studentName }}</span>
              </div>
              <div class="summary-meta">
                <span class="meta-item">
                  <van-icon name="clock-o" />
                  考试时长：{{ formattedDuration }}
                </span>
                <span class="meta-item">
                  <van-icon name="chart-trending-o" />
                  正确率：{{ accuracy }}%
                </span>
              </div>
            </div>
          </div>

          <!-- 统计卡片横排 -->
          <div class="stats-row">
            <div class="stat-card">
              <div class="stat-icon correct">
                <van-icon name="success" size="24" />
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ resultInfo.correctCount || 0 }}</div>
                <div class="stat-label">答对题数</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon wrong">
                <van-icon name="cross" size="24" />
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ resultInfo.wrongCount || 0 }}</div>
                <div class="stat-label">答错题数</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon unanswered">
                <van-icon name="question-o" size="24" />
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ resultInfo.pendingCount || 0 }}</div>
                <div class="stat-label">待批改</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon total">
                <van-icon name="notes-o" size="24" />
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ resultInfo.totalCount || 0 }}</div>
                <div class="stat-label">总题数</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon accuracy">
                <van-icon name="chart-trending-o" size="24" />
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ accuracy }}%</div>
                <div class="stat-label">正确率</div>
              </div>
            </div>
          </div>

          <!-- 题目导航 + 解析 -->
          <div class="result-layout">
            <!-- 左侧题目导航 -->
            <div class="nav-section">
              <div class="nav-card">
                <div class="card-title">题目导航</div>
                <div class="nav-grid">
                  <div
                    v-for="(q, i) in questionList"
                    :key="q.id"
                    class="nav-no"
                    :class="getNavClass(q)"
                    @click="scrollToQuestion(i)"
                  >
                    {{ i + 1 }}
                  </div>
                </div>
                <div class="nav-legend">
                  <div class="legend-item">
                    <span class="dot correct"></span>
                    <span>正确</span>
                  </div>
                  <div class="legend-item">
                    <span class="dot wrong"></span>
                    <span>错误</span>
                  </div>
                  <div class="legend-item">
                    <span class="dot unanswered"></span>
                    <span>未答</span>
                  </div>
                </div>
              </div>

              <div class="action-card">
                <van-button
                  type="primary"
                  block
                  round
                  @click="$router.push('/exam')"
                >
                  返回考试列表
                </van-button>
                <van-button
                  plain
                  type="primary"
                  block
                  round
                  style="margin-top: 12px;"
                  @click="$router.push('/exam/records')"
                >
                  查看考试记录
                </van-button>
                <van-button
                  v-if="resultInfo.allowRetry === 1"
                  plain
                  type="warning"
                  block
                  round
                  style="margin-top: 12px;"
                  @click="handleRetry"
                >
                  再考一次
                </van-button>
              </div>
            </div>

            <!-- 右侧题目解析 -->
            <div class="analysis-section">
              <div
                v-for="(q, i) in questionList"
                :key="q.id"
                class="question-card"
                :id="'question-' + i"
              >
                <div class="q-header">
                  <span class="q-no">第 {{ i + 1 }} 题</span>
                  <span class="q-type" :class="q.type">{{ getTypeText(q.type) }}</span>
                  <span class="q-score">{{ q.score }}分</span>
                  <span class="q-result" :class="getQuestionResult(q)">
                    {{ getQuestionResultText(q) }}
                  </span>
                </div>

                <div class="q-content">{{ q.content }}</div>

                <div class="q-options" v-if="q.options && q.options.length">
                  <div
                    v-for="opt in q.options"
                    :key="opt.key"
                    class="opt-item"
                    :class="{
                      'user-answer': isUserAnswer(q, opt.key),
                      'correct-answer': isCorrectAnswer(q, opt.key)
                    }"
                  >
                    <span class="opt-key">{{ opt.key }}.</span>
                    <span class="opt-text">{{ opt.text }}</span>
                    <van-icon
                      v-if="isUserAnswer(q, opt.key) && !isCorrectAnswer(q, opt.key)"
                      name="close"
                      color="#ee0a24"
                      size="14"
                    />
                    <van-icon
                      v-if="isCorrectAnswer(q, opt.key)"
                      name="success"
                      color="#07c160"
                      size="14"
                    />
                  </div>
                </div>

                <div class="q-answer-block">
                  <div class="answer-row">
                    <span class="label">你的答案：</span>
                    <span class="value" :class="isCorrect(q) ? 'correct' : 'wrong'">
                      {{ q.userAnswer || '未作答' }}
                    </span>
                  </div>
                  <div class="answer-row">
                    <span class="label">正确答案：</span>
                    <span class="value correct">{{ q.correctAnswer }}</span>
                  </div>
                </div>

                <div class="q-analysis">
                  <div class="analysis-title">
                    <van-icon name="info-o" color="#1989fa" size="16" />
                    解析
                  </div>
                  <div class="analysis-text">{{ q.analysis || '暂无解析' }}</div>
                </div>
              </div>

              <van-empty v-if="questionList.length === 0" description="暂无题目解析" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getExamResult } from '@/api/exam'
import { Toast } from 'vant'

export default {
  name: 'ExamResult',
  components: { Header },
  data() {
    return {
      resultId: this.$route.params.id,
      examId: this.$route.query.examId || '',
      loaded: false,
      showDetail: false,
      resultInfo: {},
      questionList: []
    }
  },
  computed: {
    accuracy() {
      // 后端 accuracy 可能是百分比(0-100)或小数(0-1)，统一归一化为百分比整数
      // 历史数据(ExamManageServiceImpl 旧逻辑)曾存为小数 0.8667，直接 Math.round 会得到 1% 的错误结果
      if (this.resultInfo.accuracy !== undefined && this.resultInfo.accuracy !== null) {
        let acc = Number(this.resultInfo.accuracy)
        if (isNaN(acc)) acc = 0
        // 0 < acc < 1 视为比率(小数形式)，乘 100 转百分比
        if (acc > 0 && acc < 1) {
          acc = acc * 100
        }
        return Math.round(acc)
      }
      const total = this.resultInfo.totalCount || 0
      if (total === 0) return 0
      return Math.round(((this.resultInfo.correctCount || 0) / total) * 100)
    },
    isPassed() {
      // 后端没有 passed 字段，按分数 60 判定
      return Number(this.resultInfo.score || 0) >= 60
    },
    resultLevel() {
      const score = this.resultInfo.score || 0
      if (score >= 85) return 'excellent'
      if (score >= 60) return 'pass'
      return 'fail'
    },
    formattedDuration() {
      const duration = this.resultInfo.duration || 0
      const minutes = Math.floor(duration / 60)
      const seconds = duration % 60
      return `${minutes}分${seconds}秒`
    },
    qrCodeUrl() {
      return `/api/public/exam/qrcode?recordId=${this.resultId}`
    },
    ringDashArray() {
      // 进度环: 根据分数占比计算(满分100对应整圈)
      const score = Number(this.resultInfo.score || 0)
      const totalScore = Number(this.resultInfo.totalScore || 100)
      const percent = Math.min(score / totalScore, 1)
      const circumference = 2 * Math.PI * 60 // r=60
      return `${(percent * circumference).toFixed(2)} ${circumference.toFixed(2)}`
    }
  },
  created() {
    // 支持通过 ?detail=1 直接进入"完整试卷+答案+解析"详情视图
    // （证书查询页点"查看试卷"时携带该参数，直接展示试卷详情而非简洁成绩页）
    if (this.$route.query && (this.$route.query.detail === '1' || this.$route.query.detail === 1)) {
      this.showDetail = true
    }
    this.fetchResult()
  },
  methods: {
    async fetchResult() {
      try {
        const res = await getExamResult(this.resultId)
        const data = res.data || res
        this.resultInfo = {
          recordId: data.recordId,
          examId: data.examId,
          examName: data.examName || data.name || '',
          studentName: data.studentName,
          score: data.score,
          correctCount: data.correctCount,
          wrongCount: data.wrongCount,
          totalCount: data.totalCount,
          pendingCount: data.pendingCount,
          accuracy: data.accuracy,
          duration: data.duration,
          allowRetry: data.allowRetry,
          submitTime: data.submitTime,
          totalScore: data.totalScore,
          passScore: data.passScore || 60
        }
        // 后端是 AnswerResultVO[]，转成模板需要的 questionList
        this.questionList = (data.answers || []).map(a => ({
          id: a.questionId,
          sort: a.sort,
          type: this.normalizeQuestionTypeFromAnswer(a),
          content: a.content,
          options: (a.options || []).map(o => ({ key: o.label, text: o.content })),
          userAnswer: a.studentAnswer || '',
          correctAnswer: a.correctAnswer || '',
          analysis: a.analysis || '',
          // 后端 isCorrect: 1 正确, 2 错误, 0/3 待批改
          isCorrect: a.isCorrect === 1
        }))
      } catch (error) {
        // 失败时不做假数据兜底，避免误导用户
        console.error('获取考试结果失败:', error)
        this.resultInfo = {}
        this.questionList = []
        Toast('加载考试结果失败，请稍后重试')
      } finally {
        this.loaded = true
      }
    },
    normalizeQuestionTypeFromAnswer(answer) {
      // AnswerResultVO 没有 type 字段，从 sort 或 content 推不出；默认 single
      return 'single'
    },
    getTypeText(type) {
      const map = { single: '单选题', multiple: '多选题', judge: '判断题', essay: '简答题' }
      return map[type] || '题目'
    },
    isUserAnswer(q, key) {
      if (!q.userAnswer) return false
      return q.userAnswer.split(',').includes(key)
    },
    isCorrectAnswer(q, key) {
      if (!q.correctAnswer) return false
      return q.correctAnswer.split(',').includes(key)
    },
    isCorrect(q) {
      if (q.isCorrect !== undefined) return q.isCorrect
      return q.userAnswer === q.correctAnswer
    },
    getNavClass(q) {
      if (!q.userAnswer) return 'unanswered'
      return this.isCorrect(q) ? 'correct' : 'wrong'
    },
    getQuestionResult(q) {
      if (!q.userAnswer) return 'unanswered'
      return this.isCorrect(q) ? 'correct' : 'wrong'
    },
    getQuestionResultText(q) {
      if (!q.userAnswer) return '未作答'
      return this.isCorrect(q) ? '回答正确' : '回答错误'
    },
    scrollToQuestion(index) {
      const el = document.getElementById('question-' + index)
      if (el) {
        // 在独立滚动容器内滚动,不再使用 window.scrollTo
        const container = el.closest('.analysis-section')
        if (container) {
          const top = el.offsetTop - 20
          container.scrollTo({ top, behavior: 'smooth' })
        } else {
          const top = el.getBoundingClientRect().top + window.pageYOffset - 80
          window.scrollTo({ top, behavior: 'smooth' })
        }
      }
    },
    handleRetry() {
      const examId = this.examId || this.resultInfo.examId
      if (!examId) {
        Toast('暂未找到原考试，请返回考试列表')
        return
      }
      this.$router.push(`/exam/intro/${examId}`).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.exam-result-page {
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

/* 简洁附件视图 */
.attachment-view {
  max-width: 600px;
  margin: 0 auto;
}

.attachment-header {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border-radius: 12px;
  padding: 24px 20px;
  text-align: center;
  color: #fff;
  margin-bottom: 16px;

  &.excellent {
    background: linear-gradient(135deg, #059669, #10b981);
  }

  &.pass {
    background: linear-gradient(135deg, #2563eb, #3b82f6);
  }

  &.fail {
    background: linear-gradient(135deg, #2563eb, #3b82f6);
  }

  .score-display {
    margin-bottom: 12px;
    display: flex;
    align-items: baseline;
    justify-content: center;
    gap: 6px;

    .score-value {
      font-size: 48px;
      font-weight: bold;
      line-height: 1;
    }

    .score-label {
      font-size: 16px;
      opacity: 0.8;
    }
  }

  .pass-tag {
    margin-bottom: 12px;
  }

  .tip-text {
    font-size: 13px;
    opacity: 0.9;
  }
}

.score-ring-wrapper {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto 16px;
}
.score-ring {
  .ring-fg {
    transition: stroke-dasharray 0.8s ease;
  }
}
.score-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #fff;
  .score-value {
    font-size: 36px;
    font-weight: bold;
    line-height: 1;
  }
  .score-label {
    font-size: 14px;
    opacity: 0.8;
    margin-top: 4px;
  }
}
.retry-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-size: 13px;
  padding: 4px 16px;
  border-radius: 20px;
  margin-bottom: 12px;
  cursor: pointer;
}

.student-name-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 12px;
  opacity: 0.9;
}

.student-name-detail {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 20px;

  .stat-item {
    background: #fff;
    border-radius: 12px;
    padding: 16px;
    text-align: center;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    .stat-label {
      font-size: 13px;
      color: #999;
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 18px;
      font-weight: bold;
      color: #333;
    }
  }
}

.stats-grid-4 {
  grid-template-columns: repeat(4, 1fr) !important;
  .stat-item {
    padding: 12px 8px;
    .stat-label { font-size: 12px; }
    .stat-value { font-size: 15px; }
  }
}

.qr-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  .qr-tip {
    font-size: 13px;
    color: #dc2626;
    font-weight: 500;
    margin-bottom: 16px;
  }

  .qr-code {
    display: flex;
    justify-content: center;
    align-items: center;

    img {
      width: 150px;
      height: 150px;
      border-radius: 8px;
      border: 1px solid #f0f0f0;
    }
  }
}

.action-links {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  .action-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 24px;
    border-bottom: 1px solid #f5f5f5;
    cursor: pointer;
    transition: background 0.2s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: #f9fafb;
    }

    span {
      font-size: 15px;
      color: #333;
    }
  }
}

.back-btn-wrapper {
  margin-bottom: 12px;
}

/* 返回简洁视图按钮 */
.back-to-attachment {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #1989fa;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 0;
  margin-bottom: 16px;
  transition: color 0.2s;

  &:hover {
    color: #409eff;
  }
}

.result-summary {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  border-left: 6px solid #1989fa;

  &.excellent {
    border-left-color: #07c160;
    background: linear-gradient(135deg, #f0fff0, #fff);
  }

  &.pass {
    border-left-color: #1989fa;
    background: linear-gradient(135deg, #f0f8ff, #fff);
  }

  &.fail {
    border-left-color: #ee0a24;
    background: linear-gradient(135deg, #fff5f5, #fff);
  }

  .summary-left {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-right: 32px;
    border-right: 1px solid #f0f0f0;

    .score-circle {
      width: 120px;
      height: 120px;
      border-radius: 50%;
      background: linear-gradient(135deg, #1989fa, #4ba7f7);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #fff;
      margin-bottom: 12px;

      .score-value {
        font-size: 40px;
        font-weight: bold;
        line-height: 1;
      }

      .score-label {
        font-size: 14px;
        margin-top: 4px;
      }
    }
  }

  .summary-right {
    flex: 1;
    padding-left: 32px;

    .exam-title {
      font-size: 22px;
      font-weight: bold;
      color: #333;
      margin-bottom: 16px;
    }

    .summary-meta {
      display: flex;
      gap: 24px;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 14px;
        color: #666;
      }
    }
  }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    flex-shrink: 0;

    &.correct {
      background: #07c160;
    }

    &.wrong {
      background: #ee0a24;
    }

    &.unanswered {
      background: #ff976a;
    }

    &.total {
      background: #1989fa;
    }

    &.accuracy {
      background: #9b59b6;
    }
  }

  .stat-info {
    .stat-value {
      font-size: 26px;
      font-weight: bold;
      color: #333;
      line-height: 1.2;
    }

    .stat-label {
      font-size: 13px;
      color: #999;
      margin-top: 4px;
    }
  }
}

.result-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.nav-section {
  flex: 0 0 260px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 80px;
}

.analysis-section {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  /* 独立滚动区域:试题右侧有自己的滚动条,不再使用页面整体滚动 */
  max-height: calc(100vh - 100px);
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 8px;
  /* 自定义滚动条样式 */
  &::-webkit-scrollbar {
    width: 8px;
  }
  &::-webkit-scrollbar-track {
    background: #f0f0f0;
    border-radius: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: #c0c4cc;
    border-radius: 4px;
    &:hover {
      background: #909399;
    }
  }
}

.nav-card,
.action-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.nav-card {
  .card-title {
    font-size: 16px;
    font-weight: bold;
    color: #333;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
  }

  .nav-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 8px;
    margin-bottom: 16px;

    .nav-no {
      height: 32px;
      line-height: 32px;
      text-align: center;
      border-radius: 6px;
      font-size: 13px;
      cursor: pointer;
      transition: transform 0.2s;

      &:hover {
        transform: scale(1.1);
      }

      &.correct {
        background: #07c160;
        color: #fff;
      }

      &.wrong {
        background: #ee0a24;
        color: #fff;
      }

      &.unanswered {
        background: #f7f8fa;
        color: #999;
      }
    }
  }

  .nav-legend {
    display: flex;
    justify-content: space-around;
    padding-top: 12px;
    border-top: 1px solid #f5f5f5;

    .legend-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: #999;

      .dot {
        width: 12px;
        height: 12px;
        border-radius: 3px;

        &.correct {
          background: #07c160;
        }

        &.wrong {
          background: #ee0a24;
        }

        &.unanswered {
          background: #f7f8fa;
          border: 1px solid #ddd;
        }
      }
    }
  }
}

.question-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  scroll-margin-top: 80px;

  .q-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
    padding-bottom: 14px;
    border-bottom: 1px solid #f0f0f0;

    .q-no {
      font-size: 15px;
      font-weight: bold;
      color: #1989fa;
    }

    .q-type {
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 12px;
      color: #fff;
      background: #1989fa;

      &.multiple {
        background: #ff976a;
      }

      &.judge {
        background: #07c160;
      }

      &.essay {
        background: #9b59b6;
      }
    }

    .q-score {
      font-size: 13px;
      color: #999;
    }

    .q-result {
      margin-left: auto;
      font-size: 13px;
      font-weight: 500;
      padding: 2px 10px;
      border-radius: 4px;

      &.correct {
        color: #07c160;
        background: #f0fff0;
      }

      &.wrong {
        color: #ee0a24;
        background: #fef0f0;
      }

      &.unanswered {
        color: #ff976a;
        background: #fff7e8;
      }
    }
  }

  .q-content {
    font-size: 15px;
    color: #333;
    line-height: 1.7;
    margin-bottom: 16px;
  }

  .q-options {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 16px;

    .opt-item {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 10px 14px;
      background: #f7f8fa;
      border-radius: 8px;
      font-size: 14px;
      color: #666;

      .opt-key {
        font-weight: 500;
        color: #333;
      }

      .opt-text {
        flex: 1;
      }

      &.user-answer {
        background: #fef0f0;
        color: #ee0a24;

        .opt-key {
          color: #ee0a24;
        }
      }

      &.correct-answer {
        background: #f0fff0;
        color: #07c160;

        .opt-key {
          color: #07c160;
        }
      }
    }
  }

  .q-answer-block {
    background: #f7f8fa;
    border-radius: 8px;
    padding: 14px 16px;
    margin-bottom: 16px;

    .answer-row {
      display: flex;
      margin-bottom: 8px;
      font-size: 14px;

      &:last-child {
        margin-bottom: 0;
      }

      .label {
        color: #999;
        flex-shrink: 0;
      }

      .value {
        color: #333;

        &.correct {
          color: #07c160;
          font-weight: 500;
        }

        &.wrong {
          color: #ee0a24;
          font-weight: 500;
        }
      }
    }
  }

  .q-analysis {
    .analysis-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 15px;
      font-weight: 500;
      color: #1989fa;
      margin-bottom: 10px;
    }

    .analysis-text {
      font-size: 14px;
      color: #666;
      line-height: 1.8;
      padding: 12px 16px;
      background: #f0f8ff;
      border-radius: 8px;
      border-left: 3px solid #1989fa;
    }
  }
}

@media (max-width: 992px) {
  .result-summary {
    flex-direction: column;
    text-align: center;

    .summary-left {
      padding-right: 0;
      padding-bottom: 20px;
      border-right: none;
      border-bottom: 1px solid #f0f0f0;
      margin-bottom: 20px;
    }

    .summary-right {
      padding-left: 0;
    }
  }

  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .result-layout {
    flex-direction: column;
  }

  .nav-section {
    flex: 1 1 100%;
    width: 100%;
    position: static;
  }
}

@media (max-width: 768px) {
  .container {
    width: 100%;
    padding: 12px;
  }

  .stats-grid-4 {
    grid-template-columns: repeat(2, 1fr) !important;
  }

  .question-card {
    padding: 14px 12px;
  }

  .result-summary {
    .summary-left {
      .score-circle {
        width: 80px;
        height: 80px;

        .score-value {
          font-size: 26px;
        }
      }
    }

    .summary-right {
      .exam-title {
        font-size: 16px;
      }
    }
  }

  .stats-row {
    gap: 8px;
  }

  .analysis-section {
    max-height: none;
    overflow-y: visible;
  }

  .nav-section {
    position: static;
  }
}
</style>
