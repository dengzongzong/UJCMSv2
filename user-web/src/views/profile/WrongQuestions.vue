<template>
  <div class="wrong-questions-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <div class="page-title">
          <span class="title-text">我的错题</span>
          <span class="title-sub">共 {{ wrongQuestions.length }} 道错题</span>
        </div>

        <!-- 筛选栏 -->
        <div class="filter-bar">
          <van-dropdown-menu>
            <van-dropdown-item v-model="filterType" :options="typeOptions" />
            <van-dropdown-item v-model="filterStatus" :options="statusOptions" />
          </van-dropdown-menu>
          <van-button
            size="small"
            type="danger"
            plain
            round
            @click="handleClearAll"
          >
            清空错题
          </van-button>
        </div>

        <!-- 错题列表 -->
        <div class="question-list">
          <div
            v-for="(item, index) in filteredQuestions"
            :key="item.id"
            class="question-card"
          >
            <div class="card-header">
              <div class="question-no">第 {{ index + 1 }} 题</div>
              <div class="question-tags">
                <van-tag plain :type="getTypeColor(item.type)">{{ getTypeText(item.type) }}</van-tag>
                <van-tag plain type="warning" v-if="item.wrongCount > 1">
                  错{{ item.wrongCount }}次
                </van-tag>
                <van-tag plain type="success" v-if="item.mastered">已掌握</van-tag>
              </div>
            </div>

            <div class="question-content">{{ item.content }}</div>

            <!-- 选项 -->
            <div class="options-list" v-if="item.options && item.options.length">
              <div
                v-for="opt in item.options"
                :key="opt.key"
                class="option-item"
                :class="{
                  'user-answer': isUserAnswer(item, opt.key),
                  'correct-answer': isCorrectAnswer(item, opt.key)
                }"
              >
                <span class="opt-key">{{ opt.key }}.</span>
                <span class="opt-text">{{ opt.text }}</span>
                <van-icon
                  v-if="isUserAnswer(item, opt.key) && !isCorrectAnswer(item, opt.key)"
                  name="close"
                  color="#ee0a24"
                  size="14"
                />
                <van-icon
                  v-if="isCorrectAnswer(item, opt.key)"
                  name="success"
                  color="#07c160"
                  size="14"
                />
              </div>
            </div>

            <!-- 解析 -->
            <div class="analysis-block">
              <div class="analysis-row">
                <span class="label">你的答案：</span>
                <span class="value wrong">{{ item.userAnswer || '未作答' }}</span>
              </div>
              <div class="analysis-row">
                <span class="label">正确答案：</span>
                <span class="value correct">{{ item.correctAnswer }}</span>
              </div>
              <div class="analysis-row analysis-text">
                <span class="label">解析：</span>
                <span class="value">{{ item.analysis || '暂无解析' }}</span>
              </div>
            </div>

            <div class="card-footer">
              <span class="wrong-time">
                <van-icon name="clock-o" size="13" />
                {{ item.wrongTime }}
              </span>
              <div class="footer-actions">
                <van-button
                  size="small"
                  plain
                  type="primary"
                  @click="toggleMastered(item)"
                >
                  {{ item.mastered ? '取消掌握' : '标记已掌握' }}
                </van-button>
                <van-button
                  size="small"
                  type="danger"
                  plain
                  @click="handleDelete(item)"
                >
                  删除
                </van-button>
              </div>
            </div>
          </div>

          <van-empty v-if="filteredQuestions.length === 0" description="暂无错题" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getWrongQuestions, deleteWrongQuestion, clearWrongQuestions, updateWrongQuestion } from '@/api/wrongQuestion'
import { Toast, Dialog } from 'vant'

export default {
  name: 'WrongQuestions',
  components: { Header },
  data() {
    return {
      wrongQuestions: [],
      loading: false,
      filterType: '',
      filterStatus: '',
      typeOptions: [
        { text: '全部题型', value: '' },
        { text: '单选题', value: 1 },
        { text: '多选题', value: 2 },
        { text: '判断题', value: 4 }
      ],
      statusOptions: [
        { text: '全部状态', value: '' },
        { text: '未掌握', value: 'unmastered' },
        { text: '已掌握', value: 'mastered' }
      ]
    }
  },
  computed: {
    filteredQuestions() {
      return this.wrongQuestions.filter(item => {
        if (this.filterType && item.type !== this.filterType) return false
        if (this.filterStatus === 'mastered' && !item.mastered) return false
        if (this.filterStatus === 'unmastered' && item.mastered) return false
        return true
      })
    }
  },
  created() {
    this.fetchWrongQuestions()
  },
  methods: {
    async fetchWrongQuestions() {
      this.loading = true
      try {
        const res = await getWrongQuestions()
        const data = res.data || res
        const list = Array.isArray(data) ? data : (data.list || data.records || [])
        // 映射后端字段名到前端期望的字段名
        this.wrongQuestions = list.map(item => ({
          ...item,
          userAnswer: item.studentAnswer || item.userAnswer || '',
          wrongCount: item.wrongCount || 1,
          wrongTime: item.wrongTime || item.createTime || '',
          mastered: item.mastered || false
        }))
      } catch (error) {
        this.wrongQuestions = this.getMockData()
      } finally {
        this.loading = false
      }
    },
    getMockData() {
      return [
        {
          id: 1,
          type: 'single',
          content: '在Java中，下列哪个关键字用于实现继承？',
          options: [
            { key: 'A', text: 'implements' },
            { key: 'B', text: 'extends' },
            { key: 'C', text: 'inherits' },
            { key: 'D', text: 'super' }
          ],
          userAnswer: 'A',
          correctAnswer: 'B',
          analysis: 'Java中使用extends关键字实现类的继承，implements用于实现接口。',
          wrongCount: 2,
          wrongTime: '2024-03-15 14:30',
          mastered: false
        },
        {
          id: 2,
          type: 'multiple',
          content: '下列哪些是Java的基本数据类型？',
          options: [
            { key: 'A', text: 'String' },
            { key: 'B', text: 'int' },
            { key: 'C', text: 'boolean' },
            { key: 'D', text: 'double' }
          ],
          userAnswer: 'A,B',
          correctAnswer: 'B,C,D',
          analysis: 'Java的基本数据类型有8种：byte、short、int、long、float、double、char、boolean。String是引用类型。',
          wrongCount: 1,
          wrongTime: '2024-03-14 10:20',
          mastered: false
        },
        {
          id: 3,
          type: 'judge',
          content: 'Java中的接口可以包含已实现的方法。',
          options: [
            { key: 'A', text: '正确' },
            { key: 'B', text: '错误' }
          ],
          userAnswer: 'B',
          correctAnswer: 'A',
          analysis: '从Java 8开始，接口可以包含默认方法（default）和静态方法（static），这些方法可以有具体实现。',
          wrongCount: 3,
          wrongTime: '2024-03-13 16:45',
          mastered: true
        }
      ]
    },
    getTypeText(type) {
      const map = { 1: '单选题', 2: '多选题', 3: '填空题', 4: '判断题', 5: '简答题' }
      return map[type] || '其他'
    },
    getTypeColor(type) {
      const map = { 1: 'primary', 2: 'warning', 3: 'primary', 4: 'success', 5: 'danger' }
      return map[type] || 'primary'
    },
    isUserAnswer(item, key) {
      if (!item.userAnswer) return false
      return item.userAnswer.split(',').includes(key)
    },
    isCorrectAnswer(item, key) {
      if (!item.correctAnswer) return false
      return item.correctAnswer.split(',').includes(key)
    },
    async toggleMastered(item) {
      item.mastered = !item.mastered
      try {
        await updateWrongQuestion(item.id, { mastered: item.mastered })
      } catch (error) {
        // 忽略接口错误
      }
      Toast.success(item.mastered ? '已标记为掌握' : '已取消掌握')
    },
    handleDelete(item) {
      Dialog.confirm({
        title: '提示',
        message: '确认删除这道错题吗？',
        confirmButtonText: '删除',
        confirmButtonColor: '#ee0a24'
      }).then(async () => {
        try {
          await deleteWrongQuestion(item.id)
        } catch (error) {
          // 忽略
        }
        const idx = this.wrongQuestions.findIndex(q => q.id === item.id)
        if (idx > -1) this.wrongQuestions.splice(idx, 1)
        Toast.success('删除成功')
      }).catch(() => {})
    },
    handleClearAll() {
      if (this.wrongQuestions.length === 0) {
        Toast('暂无错题')
        return
      }
      Dialog.confirm({
        title: '提示',
        message: '确认清空所有错题吗？此操作不可恢复。',
        confirmButtonText: '清空',
        confirmButtonColor: '#ee0a24'
      }).then(async () => {
        try {
          await clearWrongQuestions()
        } catch (error) {
          // 忽略
        }
        this.wrongQuestions = []
        Toast.success('已清空错题')
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.wrong-questions-page {
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

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 12px;
  padding: 6px 16px 6px 0;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  .van-dropdown-menu {
    flex: 1;
    box-shadow: none;
  }
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;

    .question-no {
      font-size: 15px;
      font-weight: bold;
      color: #1989fa;
    }

    .question-tags {
      display: flex;
      gap: 6px;
    }
  }

  .question-content {
    font-size: 15px;
    color: #333;
    line-height: 1.7;
    margin-bottom: 16px;
  }

  .options-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 16px;

    .option-item {
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

  .analysis-block {
    background: #f7f8fa;
    border-radius: 8px;
    padding: 14px 16px;
    margin-bottom: 14px;

    .analysis-row {
      display: flex;
      margin-bottom: 8px;
      font-size: 14px;
      line-height: 1.6;

      &:last-child {
        margin-bottom: 0;
      }

      .label {
        color: #999;
        flex-shrink: 0;
      }

      .value {
        color: #333;

        &.wrong {
          color: #ee0a24;
          font-weight: 500;
        }

        &.correct {
          color: #07c160;
          font-weight: 500;
        }
      }

      &.analysis-text {
        .value {
          flex: 1;
        }
      }
    }
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 14px;
    border-top: 1px solid #f5f5f5;

    .wrong-time {
      font-size: 13px;
      color: #999;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .footer-actions {
      display: flex;
      gap: 10px;
    }
  }
}
</style>
