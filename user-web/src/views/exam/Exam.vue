<template>
  <div class="exam-take-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <!-- 考试时间未到或已结束提示 -->
        <div v-if="examTimeStatus !== 'normal'" class="time-status-card">
          <van-icon :name="examTimeStatus === 'not-started' ? 'clock-o' : 'info-o'" size="48" color="#1989fa" />
          <div class="time-status-title">{{ examTimeStatus === 'not-started' ? '考试尚未开始' : '考试已结束' }}</div>
          <div class="time-status-desc">
            <template v-if="examTimeStatus === 'not-started'">
              考试开始时间：{{ examInfo.startTime }}<br/>
              请在考试开始后再进入答题。
            </template>
            <template v-else>
              考试结束时间：{{ examInfo.endTime }}<br/>
              该考试已结束，无法继续答题。
            </template>
          </div>
          <van-button type="primary" round @click="$router.push('/exam')">返回考试列表</van-button>
        </div>

        <!-- 考试信息条 -->
        <div v-else class="exam-info-bar">
          <div class="exam-name">{{ examInfo.name || '考试答题' }}</div>
          <div class="exam-meta">
            <span class="meta-item">
              <van-icon name="clock-o" />
              剩余时间：<span class="countdown" :class="{ warning: remainingSeconds < 300 }">{{ formattedTime }}</span>
            </span>
            <span class="meta-item">
              <van-icon name="passed" />
              已答 {{ answeredCount }}/{{ questionList.length }}
            </span>
          </div>
        </div>

        <div class="exam-layout">
          <!-- 左侧题目区域 -->
          <div class="question-section">
            <div class="question-card" v-if="currentQuestion">
              <div class="question-header">
                <span class="q-type-tag" :class="currentQuestion.type">
                  {{ getTypeText(currentQuestion.type) }}
                </span>
                <span class="q-index">
                  第 {{ currentIndex + 1 }} 题 / 共 {{ questionList.length }} 题
                </span>
                <span class="q-score">（{{ currentQuestion.score }}分）</span>
              </div>

              <div class="question-content">{{ currentQuestion.content }}</div>

              <!-- 单选/判断 -->
              <div class="options-list" v-if="['single', 'judge'].includes(currentQuestion.type)">
                <div
                  v-for="opt in currentQuestion.options"
                  :key="opt.key"
                  class="option-item"
                  :class="{ active: isOptionSelected(currentQuestion, opt.key) }"
                  @click="selectSingle(currentQuestion, opt.key)"
                >
                  <span class="opt-key">{{ opt.key }}</span>
                  <span class="opt-text">{{ opt.text }}</span>
                </div>
              </div>

              <!-- 多选 -->
              <div class="options-list" v-else-if="currentQuestion.type === 'multiple'">
                <div
                  v-for="opt in currentQuestion.options"
                  :key="opt.key"
                  class="option-item"
                  :class="{ active: isOptionSelected(currentQuestion, opt.key) }"
                  @click="selectMultiple(currentQuestion, opt.key)"
                >
                  <van-icon
                    :name="isOptionSelected(currentQuestion, opt.key) ? 'checked' : 'circle'"
                    :color="isOptionSelected(currentQuestion, opt.key) ? '#1989fa' : '#ccc'"
                    size="20"
                  />
                  <span class="opt-key">{{ opt.key }}</span>
                  <span class="opt-text">{{ opt.text }}</span>
                </div>
              </div>

              <!-- 简答 -->
              <div class="answer-textarea" v-else-if="currentQuestion.type === 'essay'">
                <van-field
                  v-model="currentQuestion.userAnswer"
                  type="textarea"
                  placeholder="请在此处输入你的答案..."
                  rows="8"
                  autosize
                  maxlength="2000"
                  show-word-limit
                  @input="onAnswerChange(currentQuestion)"
                />
              </div>

              <!-- 填空 -->
              <div class="answer-blank" v-else-if="currentQuestion.type === 'blank'">
                <div v-if="blankSlots(currentQuestion).length > 0" class="blank-slots">
                  <div
                    v-for="(slot, idx) in blankSlots(currentQuestion)"
                    :key="idx"
                    class="blank-slot"
                  >
                    <span class="blank-slot-label">第 {{ idx + 1 }} 空</span>
                    <van-field
                      v-model="currentQuestion.userAnswerArr[idx]"
                      :placeholder="'请输入第 ' + (idx + 1) + ' 空答案'"
                      :maxlength="200"
                      clearable
                      @input="onAnswerChange(currentQuestion)"
                    />
                  </div>
                </div>
                <van-field
                  v-else
                  v-model="currentQuestion.userAnswer"
                  type="textarea"
                  placeholder="请在此处输入答案(若有多个空,请用逗号分隔,例如:北京,上海)"
                  rows="4"
                  autosize
                  maxlength="1000"
                  show-word-limit
                  @input="onAnswerChange(currentQuestion)"
                />
                <div v-if="blankSlots(currentQuestion).length === 0" class="blank-tip">
                  提示:若题目含有多个空,请用 <code>,</code> 分隔各空的答案(与后端阅卷规则一致)
                </div>
              </div>

              <!-- 题目导航 -->
              <div class="question-nav">
                <van-button
                  plain
                  type="primary"
                  :disabled="currentIndex === 0"
                  @click="prevQuestion"
                >
                  上一题
                </van-button>
                <van-button
                  type="primary"
                  v-if="currentIndex < questionList.length - 1"
                  @click="nextQuestion"
                >
                  下一题
                </van-button>
                <van-button
                  type="success"
                  v-else
                  @click="handleSubmit"
                >
                  交卷
                </van-button>
              </div>
            </div>
          </div>

          <!-- 右侧答题卡 -->
          <div class="answer-card-section">
            <div class="answer-card">
              <div class="card-title">答题卡</div>
              <div class="card-grid">
                <div
                  v-for="(q, i) in questionList"
                  :key="q.id"
                  class="card-no"
                  :class="{
                    answered: isAnswered(q),
                    current: i === currentIndex
                  }"
                  @click="jumpTo(i)"
                >
                  {{ i + 1 }}
                </div>
              </div>

              <div class="card-legend">
                <div class="legend-item">
                  <span class="dot answered"></span>
                  <span>已答</span>
                </div>
                <div class="legend-item">
                  <span class="dot"></span>
                  <span>未答</span>
                </div>
                <div class="legend-item">
                  <span class="dot current"></span>
                  <span>当前</span>
                </div>
              </div>

              <div class="card-progress">
                <van-progress
                  :percentage="progressPercent"
                  color="#1989fa"
                  stroke-width="6"
                />
                <span class="progress-text">{{ answeredCount }}/{{ questionList.length }}</span>
              </div>

              <van-button
                type="danger"
                block
                round
                class="submit-btn"
                @click="handleSubmit"
              >
                交卷
              </van-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 交卷确认弹窗 -->
    <van-dialog
      v-model="showSubmitDialog"
      title="确认交卷"
      show-cancel-button
      confirm-button-text="确认交卷"
      confirm-button-color="#ee0a24"
      @confirm="confirmSubmit"
    >
      <div class="submit-tips">
        <p v-if="answeredCount < questionList.length" class="tip-warning">
          你还有 {{ questionList.length - answeredCount }} 题未作答
        </p>
        <p>确认要交卷吗？交卷后将无法修改答案。</p>
      </div>
    </van-dialog>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getExamPaper, submitExam, saveExamAnswer } from '@/api/exam'
import { Toast, Dialog } from 'vant'

export default {
  name: 'ExamTake',
  components: { Header },
  data() {
    return {
      examId: this.$route.params.id,
      recordId: null,
      examInfo: {},
      questionList: [],
      currentIndex: 0,
      remainingSeconds: 0,
      timer: null,
      saveTimer: null,
      answerCache: {},
      showSubmitDialog: false,
      submitted: false,
      examTimeStatus: 'normal'
    }
  },
  computed: {
    currentQuestion() {
      return this.questionList[this.currentIndex] || null
    },
    answeredCount() {
      return this.questionList.filter(q => this.isAnswered(q)).length
    },
    progressPercent() {
      if (this.questionList.length === 0) return 0
      return Math.round((this.answeredCount / this.questionList.length) * 100)
    },
    canSubmit() {
      return this.examTimeStatus === 'normal'
    },
    formattedTime() {
      const s = this.remainingSeconds
      if (s <= 0) return '00:00:00'
      const h = Math.floor(s / 3600)
      const m = Math.floor((s % 3600) / 60)
      const sec = s % 60
      return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
    }
  },
  created() {
    // 从路由 query 读取 recordId（断点续考）
    this.recordId = (this.$route.query && this.$route.query.recordId) || null
    // 本地保存做防抖，避免输入时频繁写 localStorage
    this.debouncedSaveToLocal = this.debounce(this.saveToLocal, 400)
    // 各题后端保存的延迟定时器（按 questionId 维度）
    this.backendSaveTimers = {}
    // 页面关闭/刷新前同步保存
    window.addEventListener('beforeunload', this.handleBeforeUnload)
    this.fetchExamPaper()
  },
  beforeDestroy() {
    this.stopTimer()
    this.stopAutoSave()
    this.clearBackendTimers()
    window.removeEventListener('beforeunload', this.handleBeforeUnload)
    this.saveToLocal()
  },
  beforeRouteLeave(to, from, next) {
    // 交卷跳转结果页 / 已提交：不拦截
    if (this.submitted || (to && to.path && to.path.indexOf('/exam/result') === 0)) {
      next()
      return
    }
    // 未加载出题目或无 recordId：直接放行
    if (!this.questionList.length || !this.recordId) {
      next()
      return
    }
    // 其它离开场景：提示用户进度已保存可续考
    Dialog.confirm({
      title: '退出考试',
      message: '退出后答题进度会自动保存，下次可继续答题（断点续考）。是否退出？',
      confirmButtonText: '退出',
      cancelButtonText: '继续答题'
    }).then(() => {
      this.saveToLocal()
      this.stopAutoSave()
      this.clearBackendTimers()
      // 尽力保存到后端（不阻塞跳转）
      this.saveAllToBackend()
      Toast('已保存答题进度，下次可继续')
      next()
    }).catch(() => {
      next(false)
    })
  },
  methods: {
    async fetchExamPaper() {
      try {
        const res = await getExamPaper(this.examId, this.recordId)
        const data = res.data || res
        // 后端返回的 recordId（首次开始考试或续考均会返回）
        if (data && data.recordId) {
          this.recordId = data.recordId
        }
        // 保存 recordId 到 localStorage，用于断点续考识别
        if (this.recordId) {
          localStorage.setItem('exam_record_' + this.examId, this.recordId)
        }
        this.examInfo = {
          id: data.examId || this.examId,
          name: data.examName || '考试',
          duration: data.duration
        }
        // 先从 localStorage 恢复已答内容（断点续考）
        const savedMap = this.loadFromLocal()
        this.questionList = (data.questions || []).map(q => {
          const normalizedType = this.normalizeQuestionType(q.type)
          // 优先级: localStorage 已存 > 后端返回的 userAnswer > 空白默认
          let canonical = savedMap[q.id]
          if (canonical === undefined || canonical === null || canonical === '') {
            const backendAns = q.userAnswer
            canonical = (backendAns !== undefined && backendAns !== null && backendAns !== '') ? backendAns : ''
          }
          const restored = this.restoreAnswerFromCanonical(q, normalizedType, canonical)
          this.answerCache[q.id] = this.getStudentAnswer({
            ...q, type: normalizedType, userAnswer: restored.userAnswer, userAnswerArr: restored.userAnswerArr
          })
          return {
            ...q,
            // 后端 type 为 Integer: 1-单选 2-多选 3-填空 4-判断 5-简答
            // 前端模板与逻辑按字符串使用,统一转成 'single/multiple/judge/blank/essay'
            type: normalizedType,
            // 选项 key 取后端 label(A/B/C/D);content 才是题目展示内容
            options: (q.options || []).map(o => ({ key: o.label, text: o.content })),
            userAnswer: restored.userAnswer,
            // 填空题多空场景:每个空一个答案框
            userAnswerArr: restored.userAnswerArr
          }
        })
        // 倒计时: 续考时从 localStorage 恢复上次剩余时间(扣除已退出时间),首次考试用总时长
        var totalSeconds = (data.duration || 60) * 60
        var savedRemaining = null
        if (this.recordId) {
          var saved = localStorage.getItem('exam_remaining_' + this.examId + '_' + this.recordId)
          if (saved) {
            var parsed = parseInt(saved, 10)
            if (!isNaN(parsed) && parsed > 0 && parsed <= totalSeconds) {
              savedRemaining = parsed
            }
          }
        }
        this.remainingSeconds = savedRemaining || totalSeconds
        if (!this.questionList.length) {
          Toast('该试卷暂无题目')
        } else {
          this.startTimer()
          this.startAutoSave()
        }
      } catch (error) {
        // 接口失败时不做假题兜底，避免污染后端数据
        console.error('获取考试试卷失败:', error)
        this.examInfo = { id: this.examId, name: '考试', duration: 0 }
        this.questionList = []
        this.remainingSeconds = 0
        const errorMsg = error && error.message ? error.message : '加载试卷失败，请返回重试'
        if (errorMsg.includes('考试次数') || errorMsg.includes('次数')) {
          this.$dialog.alert({
            title: '考试次数已用完',
            message: errorMsg,
            confirmButtonText: '我知道了',
            confirmButtonColor: '#1989fa'
          }).then(() => {
            this.$router.push('/exam/records').catch(() => {})
          }).catch(() => {
            this.$router.push('/exam').catch(() => {})
          })
        } else if (errorMsg.includes('尚未开始')) {
          this.examTimeStatus = 'not-started'
        } else if (errorMsg.includes('已结束')) {
          this.examTimeStatus = 'ended'
        } else {
          Toast(errorMsg)
        }
      }
    },
    /**
     * 简单防抖工具
     */
    debounce(fn, delay) {
      let timer = null
      const self = this
      return function () {
        const args = arguments
        if (timer) clearTimeout(timer)
        timer = setTimeout(function () {
          fn.apply(self, args)
        }, delay)
      }
    },
    /**
     * 取某题的标准答案字符串(与提交/后端保存格式一致)
     * multiple -> 逗号分隔; blank -> 多空逗号分隔; 其它 -> 原始字符串
     */
    getStudentAnswer(q) {
      if (!q) return ''
      if (q.type === 'multiple') {
        return Array.isArray(q.userAnswer) ? q.userAnswer.join(',') : ''
      }
      if (q.type === 'blank') {
        return this.serializeBlankAnswer(q)
      }
      return q.userAnswer || ''
    },
    /**
     * 由标准答案字符串还原 { userAnswer, userAnswerArr }
     */
    restoreAnswerFromCanonical(q, type, canonical) {
      const str = canonical || ''
      if (type === 'multiple') {
        const arr = str ? String(str).split(',') : []
        return { userAnswer: arr, userAnswerArr: [] }
      }
      if (type === 'blank') {
        return { userAnswer: str, userAnswerArr: this.initBlankArr({ ...q, type: 'blank' }, str) }
      }
      return { userAnswer: str, userAnswerArr: [] }
    },
    /**
     * 构建全部题目答案 map { questionId: studentAnswer }
     */
    buildAnswerMap() {
      const map = {}
      this.questionList.forEach(q => {
        map[q.id] = this.getStudentAnswer(q)
      })
      return map
    },
    /**
     * 保存全部答案到 localStorage（同步，断点续考核心）
     */
    saveToLocal() {
      if (!this.recordId || !this.questionList.length) return
      try {
        localStorage.setItem(
          'exam_answers_' + this.examId + '_' + this.recordId,
          JSON.stringify(this.buildAnswerMap())
        )
        // 同时保存剩余倒计时秒数,续考时恢复
        if (this.remainingSeconds > 0) {
          localStorage.setItem(
            'exam_remaining_' + this.examId + '_' + this.recordId,
            String(this.remainingSeconds)
          )
        }
      } catch (e) {
        console.warn('保存答案到本地失败:', e)
      }
    },
    /**
     * 从 localStorage 读取答案 map
     */
    loadFromLocal() {
      if (!this.recordId) return {}
      try {
        const saved = localStorage.getItem('exam_answers_' + this.examId + '_' + this.recordId)
        return saved ? JSON.parse(saved) : {}
      } catch (e) {
        return {}
      }
    },
    /**
     * 答案变化时调用：本地持久化 + 静默保存到后端（防抖）
     */
    onAnswerChange(question) {
      if (!question) return
      this.answerCache[question.id] = this.getStudentAnswer(question)
      if (this.debouncedSaveToLocal) this.debouncedSaveToLocal()
      this.debouncedSaveBackend(question)
    },
    /**
     * 静默保存单题答案到后端（防抖，避免输入时频繁请求）
     */
    debouncedSaveBackend(question) {
      if (!question || !this.recordId) return
      if (!this.backendSaveTimers) this.backendSaveTimers = {}
      if (this.backendSaveTimers[question.id]) {
        clearTimeout(this.backendSaveTimers[question.id])
      }
      const qid = question.id
      this.backendSaveTimers[qid] = setTimeout(() => {
        const cur = this.questionList.find(x => x.id === qid)
        this.saveToBackend(qid, this.getStudentAnswer(cur || question))
      }, 1500)
    },
    /**
     * 保存单题答案到后端（静默，失败不影响答题）
     */
    async saveToBackend(questionId, studentAnswer) {
      if (!this.recordId || !questionId) return
      try {
        await saveExamAnswer(this.recordId, questionId, studentAnswer)
      } catch (e) {
        console.warn('保存答案到后端失败:', questionId, e)
      }
    },
    /**
     * 保存当前全部答案到后端（静默）
     */
    async saveAllToBackend() {
      if (!this.recordId || !this.questionList.length) return
      const tasks = this.questionList.map(q => {
        return saveExamAnswer(this.recordId, q.id, this.getStudentAnswer(q)).catch(e => {
          console.warn('保存答案到后端失败:', q.id, e)
        })
      })
      await Promise.all(tasks)
    },
    /**
     * 切题前保存当前题答案到后端（立即保存，取消延迟）
     */
    saveCurrentToBackend() {
      const q = this.currentQuestion
      if (!q || !this.recordId) return
      if (this.backendSaveTimers && this.backendSaveTimers[q.id]) {
        clearTimeout(this.backendSaveTimers[q.id])
        delete this.backendSaveTimers[q.id]
      }
      this.saveToBackend(q.id, this.getStudentAnswer(q))
    },
    /**
     * 启动定时自动保存（每 30 秒保存本地 + 后端）
     */
    startAutoSave() {
      this.stopAutoSave()
      this.saveTimer = setInterval(() => {
        this.saveToLocal()
        this.saveAllToBackend()
      }, 30000)
    },
    stopAutoSave() {
      if (this.saveTimer) {
        clearInterval(this.saveTimer)
        this.saveTimer = null
      }
    },
    clearBackendTimers() {
      if (!this.backendSaveTimers) return
      Object.keys(this.backendSaveTimers).forEach(k => {
        if (this.backendSaveTimers[k]) clearTimeout(this.backendSaveTimers[k])
      })
      this.backendSaveTimers = {}
    },
    /**
     * 页面关闭/刷新前同步保存到本地（后端尽力保存）
     */
    handleBeforeUnload(e) {
      this.saveToLocal()
      this.saveAllToBackend()
      e.preventDefault()
      e.returnValue = ''
      return ''
    },
    normalizeQuestionType(type) {
      // 后端: 1-单选 2-多选 3-填空 4-判断 5-简答
      const map = { 1: 'single', 2: 'multiple', 3: 'blank', 4: 'judge', 5: 'essay' }
      return map[type] || (typeof type === 'string' ? type : 'single')
    },
    normalizeUserAnswer(type) {
      const t = this.normalizeQuestionType(type)
      if (t === 'multiple') return []
      return ''
    },
    /**
     * 初始化填空题的多空答案数组
     * 思路:用 ___ 字符(连续下划线/全角空格)占位符识别空数
     * 与后端阅卷逻辑对齐:多空用逗号分隔(ExamServiceImpl.gradeFillInQuestion)
     */
    initBlankArr(question, initialAnswer) {
      const slots = this.blankSlots(question)
      if (slots.length === 0) return []
      const ans = Array.isArray(initialAnswer) ? initialAnswer : (initialAnswer ? String(initialAnswer).split(',') : [])
      const arr = []
      for (let i = 0; i < slots.length; i++) {
        arr[i] = ans[i] || ''
      }
      return arr
    },
    /**
     * 从题干中识别空数(____、____ 或 ( ) 形式的占位符)
     * 返回空数数组
     */
    blankSlots(question) {
      if (!question) return []
      // 优先使用后端字段 blanks(若返回),否则尝试正则识别
      if (Array.isArray(question.blanks) && question.blanks.length > 0) {
        return question.blanks.map(b => ({ id: b.id, index: b.index }))
      }
      const content = question.content || ''
      // 匹配 4 个或更多连续下划线 / ( ) /  (    ) 等
      const matches = content.match(/_{3,}|\(\s*\)|（\s*）/g) || []
      if (matches.length > 0) {
        return matches.map((m, i) => ({ id: `auto-${i}`, index: i }))
      }
      // 如果题目里没有显式占位符,且已知是填空题,默认给一个空
      if (question.type === 'blank') {
        return [{ id: 'default-0', index: 0 }]
      }
      return []
    },
    /**
     * 把当前填空题的 userAnswerArr 同步到 userAnswer(交卷时使用)
     */
    syncBlankToAnswer(question) {
      if (question && question.type === 'blank') {
        question.userAnswer = this.serializeBlankAnswer(question)
      }
    },
    getTypeText(type) {
      const map = { single: '单选题', multiple: '多选题', judge: '判断题', blank: '填空题', essay: '简答题' }
      return map[type] || '题目'
    },
    isOptionSelected(question, key) {
      if (question.type === 'multiple') {
        return Array.isArray(question.userAnswer) && question.userAnswer.includes(key)
      }
      return question.userAnswer === key
    },
    selectSingle(question, key) {
      question.userAnswer = key
      this.onAnswerChange(question)
    },
    selectMultiple(question, key) {
      if (!Array.isArray(question.userAnswer)) {
        question.userAnswer = []
      }
      const idx = question.userAnswer.indexOf(key)
      if (idx > -1) {
        question.userAnswer.splice(idx, 1)
      } else {
        question.userAnswer.push(key)
      }
      this.onAnswerChange(question)
    },
    isAnswered(question) {
      if (question.type === 'multiple') {
        return Array.isArray(question.userAnswer) && question.userAnswer.length > 0
      }
      if (question.type === 'blank') {
        // 多空场景:任何一个空有答案就算已答
        if (Array.isArray(question.userAnswerArr) && question.userAnswerArr.length > 0) {
          return question.userAnswerArr.some(a => !!(a && String(a).trim()))
        }
        return !!question.userAnswer && question.userAnswer !== ''
      }
      return !!question.userAnswer && question.userAnswer !== ''
    },
    /**
     * 提交前序列化填空题答案(多空 → 字符串,用逗号分隔,与后端 gradeFillInQuestion 对齐)
     */
    serializeBlankAnswer(question) {
      if (Array.isArray(question.userAnswerArr) && question.userAnswerArr.length > 0) {
        return question.userAnswerArr.map(a => (a || '').trim()).join(',')
      }
      return (question.userAnswer || '').trim()
    },
    prevQuestion() {
      // 切题前同步填空题答案并保存当前题
      if (this.currentQuestion && this.currentQuestion.type === 'blank') {
        this.syncBlankToAnswer(this.currentQuestion)
      }
      this.saveCurrentToBackend()
      if (this.currentIndex > 0) {
        this.currentIndex--
        this.scrollToTop()
      }
    },
    nextQuestion() {
      if (this.currentQuestion && this.currentQuestion.type === 'blank') {
        this.syncBlankToAnswer(this.currentQuestion)
      }
      this.saveCurrentToBackend()
      if (this.currentIndex < this.questionList.length - 1) {
        this.currentIndex++
        this.scrollToTop()
      }
    },
    jumpTo(index) {
      if (this.currentQuestion && this.currentQuestion.type === 'blank') {
        this.syncBlankToAnswer(this.currentQuestion)
      }
      this.saveCurrentToBackend()
      this.currentIndex = index
      this.scrollToTop()
    },
    scrollToTop() {
      const section = document.querySelector('.question-section')
      if (section) section.scrollTop = 0
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    startTimer() {
      this.stopTimer()
      this.timer = setInterval(() => {
        if (this.remainingSeconds > 0) {
          this.remainingSeconds--
        } else {
          this.stopTimer()
          Toast('考试时间已到，自动交卷')
          this.autoSubmit()
        }
      }, 1000)
    },
    stopTimer() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },
    handleSubmit() {
      this.showSubmitDialog = true
    },
    async confirmSubmit() {
      if (this.submitted) return
      this.submitted = true
      this.stopTimer()
      this.stopAutoSave()
      this.clearBackendTimers()
      if (!this.recordId) {
        Toast.fail('考试记录丢失，无法交卷，请刷新页面重试')
        this.submitted = false
        return
      }
      // 提交前同步所有填空题答案(把多空数组合并为字符串)
      this.questionList.forEach(q => {
        if (q.type === 'blank') this.syncBlankToAnswer(q)
      })
      // 提交前最后保存一次本地进度
      this.saveToLocal()
      // 后端 AnswerDTO: { questionId, studentAnswer }
      // 多选用逗号分隔;填空多空也用逗号分隔(与后端 gradeFillInQuestion 对齐)
      const answers = this.questionList.map(q => ({
        questionId: q.id,
        studentAnswer: q.type === 'multiple'
          ? (Array.isArray(q.userAnswer) ? q.userAnswer.join(',') : '')
          : (q.type === 'blank' ? this.serializeBlankAnswer(q) : (q.userAnswer || ''))
      }))
      try {
        const totalSeconds = (this.examInfo.duration || 0) * 60
        const usedDuration = totalSeconds - this.remainingSeconds
        const res = await submitExam({
          recordId: this.recordId,
          answers,
          duration: Math.max(0, usedDuration)
        })
        const data = res.data || res
        const recordId = (data && data.recordId) || this.recordId
        // 交卷成功后清除断点续考数据(答案+倒计时+recordId),避免已交卷的考试还能续考
        localStorage.removeItem('exam_answers_' + this.examId + '_' + this.recordId)
        localStorage.removeItem('exam_remaining_' + this.examId + '_' + this.recordId)
        localStorage.removeItem('exam_record_' + this.examId)
        Toast.success('交卷成功')
        this.$router.replace({
          path: `/exam/result/${recordId}`,
          query: { examId: this.examId }
        }).catch(() => {})
      } catch (error) {
        console.error('提交考试失败:', error)
        this.submitted = false
        Toast.fail(error && error.message ? error.message : '交卷失败，请重试')
      }
    },
    autoSubmit() {
      this.confirmSubmit()
    }
  }
}
</script>

<style lang="scss" scoped>
.exam-take-page {
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

.exam-info-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 16px 24px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  .exam-name {
    font-size: 18px;
    font-weight: bold;
    color: #333;
  }

  .exam-meta {
    display: flex;
    gap: 24px;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      color: #666;

      .countdown {
        color: #1989fa;
        font-weight: bold;
        font-family: monospace;

        &.warning {
          color: #ee0a24;
        }
      }
    }
  }
}

.exam-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.question-section {
  flex: 0 0 70%;
  min-width: 0;
}

.answer-card-section {
  flex: 0 0 28%;
}

.question-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  .question-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #f0f0f0;

    .q-type-tag {
      padding: 3px 10px;
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

      &.blank {
        background: #ee0a24;
      }

      &.essay {
        background: #9b59b6;
      }
    }

    .q-index {
      font-size: 15px;
      color: #333;
      font-weight: 500;
    }

    .q-score {
      font-size: 13px;
      color: #999;
    }
  }

  .question-content {
    font-size: 16px;
    color: #333;
    line-height: 1.8;
    margin-bottom: 24px;
  }

  .options-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 28px;

    .option-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 14px 18px;
      background: #f7f8fa;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
      border: 2px solid transparent;

      &:hover {
        background: #f0f8ff;
      }

      &.active {
        background: #f0f8ff;
        border-color: #1989fa;
      }

      .opt-key {
        width: 26px;
        height: 26px;
        line-height: 26px;
        text-align: center;
        background: #fff;
        border: 1px solid #ddd;
        border-radius: 50%;
        font-size: 13px;
        color: #666;
        flex-shrink: 0;
      }

      &.active .opt-key {
        background: #1989fa;
        border-color: #1989fa;
        color: #fff;
      }

      .opt-text {
        flex: 1;
        font-size: 15px;
        color: #333;
      }
    }
  }

  .answer-textarea {
    margin-bottom: 28px;

    .van-field {
      background: #f7f8fa;
      border-radius: 8px;
    }
  }

  .answer-blank {
    margin-bottom: 28px;

    .blank-slots {
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .blank-slot {
      display: flex;
      align-items: center;
      gap: 12px;

      .blank-slot-label {
        flex-shrink: 0;
        width: 60px;
        font-size: 13px;
        color: #666;
        font-weight: 500;
      }

      .van-field {
        flex: 1;
        background: #f7f8fa;
        border-radius: 8px;
      }
    }

    .blank-tip {
      margin-top: 10px;
      font-size: 12px;
      color: #999;

      code {
        background: #f0f0f0;
        padding: 2px 6px;
        border-radius: 3px;
        color: #1989fa;
        font-family: monospace;
      }
    }

    .van-cell {
      padding: 6px 12px;
    }
  }

  .question-nav {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding-top: 20px;
    border-top: 1px solid #f0f0f0;

    .van-button {
      min-width: 120px;
    }
  }
}

.answer-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 80px;

  .card-title {
    font-size: 16px;
    font-weight: bold;
    color: #333;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
  }

  .card-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 10px;
    margin-bottom: 16px;

    .card-no {
      height: 36px;
      line-height: 36px;
      text-align: center;
      background: #f7f8fa;
      border-radius: 6px;
      font-size: 14px;
      color: #666;
      cursor: pointer;
      border: 2px solid transparent;
      transition: all 0.2s;

      &:hover {
        background: #f0f8ff;
      }

      &.answered {
        background: #1989fa;
        color: #fff;
      }

      &.current {
        border-color: #1989fa;
        font-weight: bold;
      }
    }
  }

  .card-legend {
    display: flex;
    justify-content: space-around;
    margin-bottom: 16px;
    padding: 12px 0;
    border-top: 1px solid #f5f5f5;
    border-bottom: 1px solid #f5f5f5;

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
        background: #f7f8fa;
        border: 2px solid transparent;

        &.answered {
          background: #1989fa;
        }

        &.current {
          background: #f7f8fa;
          border-color: #1989fa;
        }
      }
    }
  }

  .card-progress {
    margin-bottom: 16px;

    .progress-text {
      display: block;
      text-align: center;
      margin-top: 6px;
      font-size: 13px;
      color: #666;
    }
  }

  .submit-btn {
    margin-top: 4px;
  }
}

.submit-tips {
  padding: 16px 20px;
  text-align: center;

  p {
    font-size: 14px;
    color: #666;
    line-height: 1.8;
    margin-bottom: 6px;
  }

  .tip-warning {
    color: #ff976a;
    font-weight: 500;
  }
}

.time-status-card {
  background: #fff;
  border-radius: 12px;
  padding: 48px 32px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;

  .time-status-title {
    font-size: 22px;
    font-weight: bold;
    color: #333;
    margin: 16px 0 12px;
  }

  .time-status-desc {
    font-size: 14px;
    color: #999;
    line-height: 1.8;
    margin-bottom: 24px;
  }

  .van-button {
    width: 200px;
    height: 44px;
    font-size: 15px;
  }
}

@media (max-width: 992px) {
  .exam-layout {
    flex-direction: column;
  }

  .question-section,
  .answer-card-section {
    flex: 1 1 100%;
    width: 100%;
  }

  .answer-card {
    position: static;
  }
}

@media (max-width: 768px) {
  .container {
    width: 100%;
    padding: 12px;
  }

  .exam-info-bar {
    flex-wrap: wrap;
    gap: 8px;

    .exam-name {
      font-size: 16px;
    }

    .exam-meta {
      gap: 12px;

      .meta-item {
        font-size: 12px;
      }
    }
  }

  .question-card {
    padding: 14px 12px;

    .question-content {
      font-size: 15px;
    }

    .options-list {
      .option-item {
        .opt-text {
          font-size: 14px;
        }
      }
    }

    .question-nav {
      .van-button {
        min-width: auto;
        flex: 1;
        font-size: 13px;
      }
    }
  }

  .answer-card {
    .card-grid {
      grid-template-columns: repeat(6, 1fr);
      gap: 4px;
    }
  }
}
</style>
