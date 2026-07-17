<template>
  <div class="records-page" :class="{ 'embedded-mode': embedded }">
    <Header v-if="!embedded" />

    <div :class="embedded ? 'embedded-body' : 'page-body'">
      <div :class="embedded ? 'embedded-container' : 'container'">
        <div class="page-title">
          <span class="title-text">考试记录</span>
          <span class="title-sub">共 {{ records.length }} 次考试</span>
        </div>

        <!-- 统计概览 -->
        <div class="overview-row">
          <div class="overview-card">
            <div class="ov-value">{{ records.length }}</div>
            <div class="ov-label">考试次数</div>
          </div>
          <div class="overview-card">
            <div class="ov-value">{{ avgScore }}</div>
            <div class="ov-label">平均分</div>
          </div>
          <div class="overview-card">
            <div class="ov-value">{{ maxScore }}</div>
            <div class="ov-label">最高分</div>
          </div>
          <div class="overview-card">
            <div class="ov-value">{{ passRate }}%</div>
            <div class="ov-label">通过率</div>
          </div>
        </div>

        <!-- 记录表格 -->
        <div class="records-table">
          <div class="table-header">
            <div class="th col-name">考试名称</div>
            <div class="th col-score">得分</div>
            <div class="th col-result">结果</div>
            <div class="th col-time">用时</div>
            <div class="th col-date">考试时间</div>
            <div class="th col-action">操作</div>
          </div>

          <div
            v-for="record in records"
            :key="record.id"
            class="table-row"
          >
            <div class="td col-name">
              <van-icon name="notes-o" color="#1989fa" size="16" />
              <span>{{ record.examName }}</span>
            </div>
            <div class="td col-score">
              <span class="score" :class="getScoreClass(record.score)">{{ record.score }}</span>
            </div>
            <div class="td col-result">
              <van-tag :type="isPassed(record) ? 'success' : 'danger'" round size="medium">
                {{ isPassed(record) ? '通过' : '未通过' }}
              </van-tag>
            </div>
            <div class="td col-time">{{ formatDuration(record.duration) }}</div>
            <div class="td col-date">{{ formatDate(record.submitTime) }}</div>
            <div class="td col-action">
              <van-button
                size="mini"
                type="primary"
                plain
                round
                @click="viewResult(record)"
              >
                查看解析
              </van-button>
              <van-button
                size="mini"
                type="warning"
                plain
                round
                @click="retryExam(record)"
              >
                再考一次
              </van-button>
            </div>
          </div>

          <van-empty v-if="records.length === 0" description="暂无考试记录" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getExamRecords } from '@/api/exam'

export default {
  name: 'ExamRecords',
  components: { Header },
  props: {
    // 内嵌模式：在个人中心右侧内容区直接展示，不渲染独立页头与外层 page-body
    embedded: { type: Boolean, default: false }
  },
  data() {
    return {
      records: [],
      loading: false
    }
  },
  computed: {
    avgScore() {
      if (this.records.length === 0) return 0
      const total = this.records.reduce((sum, r) => sum + (r.score || 0), 0)
      return Math.round(total / this.records.length)
    },
    maxScore() {
      if (this.records.length === 0) return 0
      return Math.max(...this.records.map(r => r.score || 0))
    },
    passRate() {
      if (this.records.length === 0) return 0
      const passed = this.records.filter(r => this.isPassed(r)).length
      return Math.round((passed / this.records.length) * 100)
    }
  },
  created() {
    this.fetchRecords()
  },
  methods: {
    async fetchRecords() {
      this.loading = true
      try {
        const res = await getExamRecords()
        const data = res.data || res
        this.records = Array.isArray(data) ? data : (data.list || data.records || [])
      } catch (error) {
        console.error('获取考试记录失败:', error)
        this.records = []
      } finally {
        this.loading = false
      }
    },
    getScoreClass(score) {
      if (score >= 85) return 'excellent'
      if (score >= 60) return 'pass'
      return 'fail'
    },
    // 后端 ExamRecordVO 没有 passed 字段，按 60 分判定
    isPassed(record) {
      return Number(record.score || 0) >= 60
    },
    formatDate(value) {
      if (!value) return '--'
      const d = new Date(value)
      if (isNaN(d.getTime())) return '--'
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    formatDuration(duration) {
      if (!duration || duration <= 0) return '--'
      const minutes = Math.floor(duration / 60)
      const seconds = duration % 60
      return `${minutes}分${seconds}秒`
    },
    viewResult(record) {
      this.$router.push({
        path: `/exam/result/${record.id}`,
        query: { examId: record.examId }
      }).catch(() => {})
    },
    retryExam(record) {
      this.$router.push(`/exam/intro/${record.examId}`).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.records-page {
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

/* 内嵌模式：撑满个人中心右侧内容区，去掉固定页头留白与宽度限制 */
.records-page.embedded-mode { min-height: auto; background: transparent; }
.embedded-body { padding-top: 0; }
.embedded-container { width: 100%; max-width: none; margin: 0; padding: 0; }

.page-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;

  .title-text {
    font-size: 24px;
    font-weight: bold;
    color: #333;
    position: relative;
    padding-left: 14px;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 4px;
      height: 22px;
      background: #1989fa;
      border-radius: 2px;
    }
  }

  .title-sub {
    font-size: 14px;
    color: #999;
  }
}

.overview-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.overview-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  .ov-value {
    font-size: 32px;
    font-weight: bold;
    color: #1989fa;
    line-height: 1.2;
  }

  .ov-label {
    font-size: 14px;
    color: #999;
    margin-top: 6px;
  }
}

.records-table {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  .table-header {
    display: flex;
    align-items: center;
    background: #f7f8fa;
    padding: 14px 24px;
    border-bottom: 1px solid #f0f0f0;

    .th {
      font-size: 14px;
      color: #666;
      font-weight: 500;
    }
  }

  .table-row {
    display: flex;
    align-items: center;
    padding: 18px 24px;
    border-bottom: 1px solid #f5f5f5;
    transition: background 0.2s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: #fafbfc;
    }

    .td {
      font-size: 14px;
      color: #333;
    }
  }

  .col-name {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;

    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .col-score {
    flex: 0 0 80px;

    .score {
      font-size: 18px;
      font-weight: bold;

      &.excellent {
        color: #07c160;
      }

      &.pass {
        color: #1989fa;
      }

      &.fail {
        color: #ee0a24;
      }
    }
  }

  .col-result {
    flex: 0 0 90px;
  }

  .col-time {
    flex: 0 0 110px;
    color: #666;
  }

  .col-date {
    flex: 0 0 160px;
    color: #999;
  }

  .col-action {
    flex: 0 0 200px;
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
}

@media (max-width: 992px) {
  .overview-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .records-table {
    .col-time,
    .col-date {
      display: none;
    }
  }
}

@media (max-width: 600px) {
  .records-table {
    .col-result {
      display: none;
    }

    .col-action {
      flex: 0 0 140px;
    }
  }
}
</style>
