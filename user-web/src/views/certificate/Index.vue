<template>
  <div class="certificate-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <div class="page-title">
          <van-icon name="medal-o" size="20" color="#1989fa" />
          <span class="title-text">证书查询</span>
          <span class="title-sub">查询您名下的证书与可查看的试卷</span>
        </div>

        <van-tabs v-model="activeTab" class="main-tabs">
          <!-- 证书查询 Tab: 不需要登录,按身份证+姓名查 -->
          <van-tab title="证书查询" name="cert">
            <div class="cert-layout">
              <!-- 左栏:查询表单 -->
              <div class="sidebar">
                <div class="card">
                  <div class="card-header">
                    <van-icon name="search" size="16" color="#1989fa" />
                    <span>查询条件</span>
                  </div>
                  <div class="card-body">
                    <van-field v-model="form.idCard" label="身份证号" placeholder="选填,与姓名一起查询" maxlength="20" clearable @clear="onClearField('idCard')" />
                    <van-field v-model="form.name" label="姓名" placeholder="选填,与身份证一起查询" maxlength="50" clearable @clear="onClearField('name')" />
                    <van-field v-model="form.certNo" label="证书编号" placeholder="可单独凭证书编号查询" maxlength="50" clearable @clear="onClearField('certNo')" />
                    <div class="btn-group">
                      <van-button type="primary" icon="search" :loading="searching" block @click="onSearch">查询证书</van-button>
                      <van-button icon="replay" block @click="onReset">重 置</van-button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 右栏:结果列表 -->
              <div class="content-area">
                <div class="card result-card">
                  <div class="card-header result-header">
                  <div class="header-left">
                    <van-icon name="medal-o" size="16" color="#1989fa" />
                    <span>查询结果</span>
                    <span v-if="searched" class="result-count">共 <em>{{ total }}</em> 张</span>
                  </div>
                  <div class="header-right" v-if="searched && records.length > 0">
                    <van-button size="small" type="warning" :disabled="selectedIds.length === 0" :loading="!!selectedDownloading" @click="openDownloadSheet({ type: 'selected' })">下载选中({{ selectedIds.length }})</van-button>
                    <van-button size="small" type="primary" :loading="!!batchDownloading" @click="openDownloadSheet({ type: 'all' })">下载全部</van-button>
                  </div>
                </div>

                  <div class="card-body">
                    <!-- 未查询状态 -->
                    <div v-if="!searched" class="placeholder">
                      <van-icon name="search" size="48" color="#c0c4cc" />
                      <p>请在左侧填写身份证号和姓名后点击"查询证书"</p>
                    </div>

                    <!-- 空结果 -->
                    <div v-else-if="records.length === 0 && !searching" class="placeholder">
                      <van-icon name="warning-o" size="48" color="#c0c4cc" />
                      <p>未找到匹配的证书</p>
                      <p class="sub">请确认身份证号和姓名与证书上的信息完全一致</p>
                    </div>

                    <!-- 结果表格 -->
                    <div v-else class="result-table">

                      <table class="cert-table">
                        <thead>
                          <tr>
                            <th style="width:40px;text-align:center"><input type="checkbox" :checked="selectedIds.length === records.length && records.length > 0" @change="onToggleAll($event)" /></th>
                            <th style="min-width:150px;text-align:center">证书操作</th>
                            <th style="width:80px;text-align:center">姓名</th>
                            <th style="width:60px;text-align:center">性别</th>
                            <th style="min-width:160px;text-align:center">证书编号</th>
                            <th style="min-width:180px;text-align:center">身份证号</th>
                            <th style="min-width:100px;text-align:center">专业</th>
                            <th style="width:80px;text-align:center">技能等级</th>
                            <th style="width:110px;text-align:center">颁发日期</th>
                            <th style="min-width:120px;text-align:center">考试成绩</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="cert in records" :key="cert.id" class="cert-row" :class="{ 'row-selected': selectedIds.includes(cert.id) }">
                            <td style="text-align:center" data-label="选择"><input type="checkbox" :value="cert.id" v-model="selectedIds" /></td>
                            <td data-label="证书操作">
                              <div class="action-cell">
                                <span class="preview-link" @click="onPreviewCert(cert)">预览</span>
                                <van-button size="small" type="primary" :loading="isCertDownloading(cert)" @click="openDownloadSheet({ type: 'single', cert })">下载</van-button>
                              </div>
                            </td>
                            <td style="text-align:center" data-label="姓名">
                              <div class="name-cell">
                                <van-icon name="user-circle-o" color="#1989fa" size="16" />
                                <span>{{ cert.name }}</span>
                              </div>
                            </td>
                            <td style="text-align:center" data-label="性别">{{ formatGender(cert) }}</td>
                            <td style="text-align:center" data-label="证书编号"><span class="cert-no">{{ cert.certNo || '—' }}</span></td>
                            <td style="text-align:center" data-label="身份证号"><span class="id-card">{{ cert.idCard }}</span></td>
                            <td style="text-align:center" data-label="专业"><span class="profession">{{ cert.professionName || cert.profession || '—' }}</span></td>
                            <td style="text-align:center" data-label="技能等级">
                              <span class="level" v-if="cert.skillLevel">{{ cert.skillLevel }}</span>
                              <span v-else>—</span>
                            </td>
                            <td style="text-align:center" data-label="颁发日期">{{ cert.issueDateStr || cert.issueDate || '—' }}</td>
                            <td style="text-align:center" data-label="考试成绩">
                              <div v-if="cert.theoryScore != null && cert.theoryScore !== ''" class="exam-score">
                                <span :class="Number(cert.theoryScore) >= 60 ? 'score-pass' : 'score-fail'">{{ cert.theoryScore }}分</span>
                              </div>
                              <span v-else>—</span>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    <!-- 证书预览弹窗 -->
                    <div v-if="certPreview.visible" class="cert-preview-modal" @click.self="certPreview.visible = false">
                      <div class="cert-preview-content">
                        <div class="cert-preview-header">
                          <span>证书预览 - {{ certPreview.certName }}</span>
                          <van-icon name="cross" size="20" color="#fff" class="close-btn" @click="certPreview.visible = false" />
                        </div>
                        <div class="cert-preview-body" @wheel.prevent="onPreviewWheel" @touchstart.prevent="onTouchStart" @touchmove.prevent="onTouchMove" @touchend="onTouchEnd">
          <img v-if="certPreview.url" :src="certPreview.url" alt="证书预览" @error="onPreviewError" @load="onPreviewLoad"
               :style="previewImgStyle" />
          <div v-if="certPreview.url" class="zoom-controls">
            <span class="zoom-btn" @click="onZoomIn">+</span>
            <span class="zoom-info">{{ Math.round(previewScale * 100) }}%</span>
            <span class="zoom-btn" @click="onZoomOut">−</span>
            <span class="zoom-btn" @click="onZoomReset">↺</span>
          </div>
                          <div v-if="certPreview.error" class="preview-error">
                            <van-icon name="warning-o" size="48" color="#c0c4cc" />
                            <p>证书预览加载失败</p>
                          </div>
                          <div v-if="certPreview.loading" class="preview-loading">
                            <van-loading size="48" />
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </van-tab>

          <!-- 试卷查询 Tab: 需要登录 -->
          <van-tab title="试卷查询" name="paper">
            <div class="paper-layout">
              <!-- 未登录: 登录提示 -->
              <div class="card" v-if="!isLoggedIn">
                <div class="card-body">
                  <van-notice-bar left-icon="info-o" color="#1989fa" background="#e8f3ff">
                    试卷查询需要先登录,登录后将展示您已开通的所有考试试卷。
                  </van-notice-bar>
                  <div class="login-tip">
                    <van-field v-model="paperLogin.account" placeholder="手机号或身份证号" clearable left-icon="user-o" class="login-field" />
                    <van-field v-model="paperLogin.password" placeholder="密码" type="password" clearable left-icon="lock" class="login-field" @keyup.enter.native="onPaperLogin" />
                    <van-button type="primary" :loading="paperLogin.submitting" block @click="onPaperLogin">登 录</van-button>
                  </div>
                </div>
              </div>

              <!-- 已登录: 已考过的试卷(同专业取最高分) -->
              <div v-else class="card">
                <div class="card-header">
                  <van-icon name="records" size="16" color="#1989fa" />
                  <span>我的考试成绩({{ myExamRecords.length }} 个专业)</span>
                  <span class="logout-link" @click="onPaperLogout">切换账号</span>
                </div>
                <div class="card-body">
                  <div class="paper-grid">
                    <van-loading v-if="paperLoading" class="paper-loading" />
                    <template v-else-if="myExamRecords.length > 0">
                      <div v-for="item in myExamRecords" :key="item.recordId" class="paper-card" @click="openPaperFromRecord(item)">
                        <div class="paper-cover">
                          <img v-if="item.coverUrl" :src="resolveImg(item.coverUrl)" :alt="item.examName" @error="onCoverError" />
                          <div v-else class="paper-cover-default">
                            <van-icon name="certificate" size="32" color="#1989fa" />
                          </div>
                        </div>
                        <div class="paper-content">
                          <div class="paper-name">{{ item.examName }}</div>
                          <div class="paper-meta">
                            <span><van-icon name="bookmark-o" /> {{ item.professionName || '通用' }}</span>
                            <span :class="item.score >= 60 ? 'score-pass' : 'score-fail'">
                              <van-icon name="gold-coin-o" /> {{ item.score }}分
                            </span>
                          </div>
                          <div class="paper-action">
                            <van-button type="primary" size="small" icon="eye-o" @click.stop="openPaperFromRecord(item)">查看试卷</van-button>
                          </div>
                        </div>
                      </div>
                    </template>
                    <van-empty v-else description="暂无考试记录" />
                  </div>
                </div>
              </div>
            </div>
          </van-tab>
        </van-tabs>
      </div>
    </div>

    <!-- 试卷查看弹窗 -->
    <van-popup v-model="paperDialog.visible" position="bottom" :style="{ height: '80%' }" round closeable>
      <div class="paper-popup">
        <div class="paper-popup-title">{{ paperDialog.examName }} - 试卷预览</div>
        <van-loading v-if="paperDialog.loading" class="paper-loading" />
        <div v-else class="paper-preview">
          <div v-for="(q, idx) in paperDialog.questions" :key="q.id" class="paper-question">
            <div class="q-title">
              <span class="q-no">{{ idx + 1 }}.</span>
              <span class="q-type">({{ questionTypeLabel(q.type) }})</span>
              <span class="q-score">【{{ q.score || 0 }} 分】</span>
              <span class="q-content">{{ q.content }}</span>
            </div>
            <div v-if="q.options && q.options.length" class="q-options">
              <div v-for="opt in q.options" :key="opt.id" class="q-option">
                <span class="opt-label">{{ opt.label }}.</span>
                <span class="opt-content">{{ opt.content }}</span>
                <span v-if="opt.label === String(paperDialog.answers[q.id])" class="tag-warning">您的答案</span>
                <span v-if="opt.isCorrect" class="tag-success-sm">正确答案</span>
              </div>
            </div>
            <div v-if="paperDialog.answers[q.id] && q.type !== 1 && q.type !== 2 && q.type !== 4" class="q-answer-block">
              您的答案:<span class="q-answer-text">{{ paperDialog.answers[q.id] }}</span>
            </div>
            <div v-if="q.correctAnswer" class="q-correct">
              参考答案:<span class="q-correct-text">{{ q.correctAnswer }}</span>
            </div>
          </div>
          <van-empty v-if="paperDialog.questions.length === 0" description="该试卷暂无题目" />
        </div>
      </div>
    </van-popup>

    <!-- 照片预览 -->
    <div v-if="previewPhotoUrl" class="photo-preview-modal" @click="onClosePreview">
      <div class="preview-content" @click.stop>
        <img :src="previewPhotoUrl" class="preview-image" />
        <van-icon name="cross" size="24" color="#fff" class="close-btn" @click="onClosePreview" />
      </div>
    </div>

    <!-- 下载格式选择 -->
    <van-dialog
      v-model="downloadSheet.show"
      title="选择下载格式"
      :show-cancel-button="false"
      :confirm-button-open-type="false"
      width="320px"
    >
      <div class="download-format-options">
        <div class="format-option" @click="onDownloadSheetSelect({ format: 'image' })">
          <van-icon name="picture-o" size="32" color="#1989fa" />
          <div class="format-name">下载为图片</div>
          <div class="format-desc">PNG格式</div>
        </div>
        <div class="format-option" @click="onDownloadSheetSelect({ format: 'pdf' })">
          <van-icon name="file-text-o" size="32" color="#67c23a" />
          <div class="format-name">下载为PDF</div>
          <div class="format-desc">PDF格式</div>
        </div>
      </div>
    </van-dialog>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { searchMyCertificates, downloadCertificate, downloadAllCertificates, downloadSelectedCertificates, saveBlob, buildCertificateDownloadUrl, getMyCertificates, getMyExamRecords } from '@/api/certificate'
import { login } from '@/api/auth'
import { getMyExams } from '@/api/exam'
import { viewPaper } from '@/api/examPaper'
import { apiUrl, resolveImg } from '@/utils/apiBase'
import { Toast } from 'vant'

export default {
  name: 'CertificatePortal',
  components: { Header },
  data() {
    return {
      activeTab: 'cert',
      form: { idCard: '', name: '', certNo: '' },
      searching: false,
      searched: false,
      records: [],
      total: 0,
      downloadingId: null,
      batchDownloading: null,
      selectedIds: [],
      selectedDownloading: null,
      certPreview: { visible: false, url: '', certName: '', loading: false, error: false },
      previewScale: 1,
      previewTouchStart: null,
      previewPhotoUrl: '',
      // 下载格式选择面板:context 标记当前下载场景(single/selected/all)
      downloadSheet: { show: false, context: null },
      downloadActions: [
        { name: '下载为图片', format: 'image' },
        { name: '下载为PDF', format: 'pdf' }
      ],
      paperLogin: { account: '', password: '', submitting: false },
      myExams: [],
      myCertLoading: false,
      myExamRecords: [],
      paperLoading: false,
      paperDialog: {
        visible: false,
        loading: false,
        examId: null,
        examName: '',
        questions: [],
        answers: {}
      }
    }
  },
  computed: {
    isLoggedIn() {
      return !!this.$store.getters.token
    },
    previewImgStyle() {
      return {
        transform: 'scale(' + this.previewScale + ')',
        transition: 'transform 0.15s ease'
      }
    }
  },
  watch: {
    activeTab(v) {
      if (v === 'paper' && this.isLoggedIn) {
        this.fetchMyExams()
        this.fetchMyBestRecords()
      }
    },
    isLoggedIn(v) {
      if (v && this.activeTab === 'paper') {
        this.fetchMyExams()
        this.fetchMyBestRecords()
      }
    },
    'certPreview.visible'(val) {
      if (!val) { this.previewScale = 1 }
    }
  },
  created() {
    if (this.isLoggedIn && this.activeTab === 'paper') this.fetchMyExams()
    // 支持 URL 参数自动填入查询条件并自动查询:
    //   ?bh=证书编号&name=姓名  或  ?idCard=xxx&name=xxx  或  ?certNo=xxx
    this.applyUrlQuery()
    // 已登录用户且没有URL查询参数时,自动查询自己的证书
    if (this.isLoggedIn && !this._fromUrl) {
      this.fetchMyCertificates()
      this.fetchMyBestRecords()
    }
  },
  methods: {
    apiUrl,
    resolveImg,
    // 格式化性别显示:优先用后端返回的gender字段,其次从身份证号提取
    formatGender(cert) {
      if (cert.gender != null) {
        return cert.gender === 1 || cert.gender === '1' ? '男' : (cert.gender === 2 || cert.gender === '2' ? '女' : '—')
      }
      if (cert.genderStr) {
        return cert.genderStr
      }
      // 从身份证号第17位推断
      if (cert.idCard && cert.idCard.length === 18) {
        var c = cert.idCard.charAt(16)
        if (c >= '0' && c <= '9') {
          return (parseInt(c) % 2 === 1) ? '男' : '女'
        }
      }
      return '—'
    },
    // 格式化分数:空值显示横杠,有值显示数字
    formatScore(val) {
      if (val == null || val === '' || val === undefined) return '—'
      var s = String(val).trim()
      if (s === '' || s === 'null' || s === 'undefined') return '—'
      // 提取数字部分
      var num = parseFloat(s)
      if (!isNaN(num)) return num
      return s
    },
    // 格式化综合测评:空值显示横杠,有值显示文本(如"合格")
    formatEvaluation(val) {
      if (val == null || val === '' || val === undefined) return '—'
      var s = String(val).trim()
      if (s === '' || s === 'null' || s === 'undefined') return '—'
      return s
    },
    // 从浏览器地址栏读取查询参数,自动填入表单并自动查询。
    // 支持两种参数格式:
    //   1. 原生格式: ?idCard=xxx&name=xxx  或  ?certNo=xxx
    //   2. 外部链接格式: ?bh=证书编号&name=姓名
    //      bh → certNo(证书编号), name → name(姓名)
    applyUrlQuery() {
      const query = (this.$route && this.$route.query) || {}
      let touched = false
      // 外部链接参数: bh → certNo
      if (query.bh) { this.form.certNo = String(query.bh).trim(); touched = true }
      // 原生参数
      if (query.certNo) { this.form.certNo = String(query.certNo).trim(); touched = true }
      if (query.idCard) { this.form.idCard = String(query.idCard).trim(); touched = true }
      if (query.name) { this.form.name = String(query.name).trim(); touched = true }
      if (!touched) return
      // 自动切换到证书查询 Tab
      this.activeTab = 'cert'
      // 标记来自URL参数,跳过已登录用户的自动查询(避免覆盖URL查询结果)
      this._fromUrl = true
      // 参数足以发起查询时,自动查询
      const hasCertNo = !!this.form.certNo
      const hasIdCardAndName = !!this.form.idCard && !!this.form.name
      if (hasCertNo || hasIdCardAndName) {
        this.$nextTick(() => this.onSearch())
      }
    },
    async onSearch() {
      // 如果有证书编号或身份证+姓名,优先走公开查询接口(支持URL参数和手动输入)
      var hasCertNo = (this.form.certNo || '').trim()
      var hasIdCard = (this.form.idCard || '').trim()
      var hasName = (this.form.name || '').trim()
      if (hasCertNo || (hasIdCard && hasName)) {
        this.searching = true
        try {
          const res = await searchMyCertificates({
            idCard: hasIdCard || undefined,
            name: hasName || undefined,
            certNo: hasCertNo || undefined
          })
          const data = res.data || res
          this.records = Array.isArray(data) ? data : (data.records || data.list || [])
          this.total = this.records.length
          this.searched = true
          this.selectedIds = []
          if (this.records.length > 0) Toast.success('找到 ' + this.records.length + ' 张证书')
        } catch (error) {
          this.records = []
          this.total = 0
          this.searched = true
        } finally {
          this.searching = false
        }
        return
      }
      // 已登录用户且没有输入查询条件,直接查询自己的证书(通过token关联)
      if (this.isLoggedIn) {
        await this.fetchMyCertificates()
        return
      }
      Toast('请输入身份证号和姓名,或输入证书编号')
    },
    onReset() {
      this.form = { idCard: '', name: '', certNo: '' }
      this.records = []
      this.total = 0
      this.searched = false
      this.selectedIds = []
    },
    // 清除输入框内容(Vant 2 clearable 的 @clear 事件兜底,确保值一定被清空)
    onClearField(field) {
      this.$set(this.form, field, '')
    },
    // 判断某条证书是否正在下载(任一格式)
    isCertDownloading(cert) {
      return !!this.downloadingId && this.downloadingId.indexOf(cert.id + '-') === 0
    },
    // 打开下载格式选择面板,context 记录下载场景
    openDownloadSheet(context) {
      this.downloadSheet.context = context
      this.downloadSheet.show = true
    },
    // 选择某个格式后,根据场景调用对应下载逻辑
    onDownloadSheetSelect(item) {
      const ctx = this.downloadSheet.context
      const format = item && item.format
      this.downloadSheet.show = false
      this.downloadSheet.context = null
      if (!ctx) return
      if (ctx.type === 'single') {
        this.onDownload(ctx.cert, format)
      } else if (ctx.type === 'selected') {
        this.onDownloadSelected(format)
      } else if (ctx.type === 'all') {
        this.onDownloadAll(format)
      }
    },
    // 取消下载格式选择
    onDownloadSheetCancel() {
      this.downloadSheet.show = false
      this.downloadSheet.context = null
    },
    async onDownload(cert, format) {
      const key = cert.id + '-' + format
      this.downloadingId = key
      try {
        const { fileName, blob } = await downloadCertificate(cert, format)
        saveBlob(blob, fileName)
        Toast.success('证书已开始下载')
      } catch (error) {
        Toast.fail((error && error.message) || '下载失败')
      } finally {
        this.downloadingId = null
      }
    },
    // 一个人可有多张证书,支持一次性打包下载全部证书(ZIP)
    async onDownloadAll(format) {
      if (!this.records.length) {
        Toast('暂无证书可下载')
        return
      }
      // 仅一张时直接走单张下载,无需打包
      if (this.records.length === 1) {
        this.onDownload(this.records[0], format)
        return
      }
      this.batchDownloading = format
      try {
        const { fileName, blob } = await downloadAllCertificates(
          (this.form.idCard || '').trim(),
          (this.form.name || '').trim(),
          format,
          (this.form.certNo || '').trim()
        )
        saveBlob(blob, fileName)
        Toast.success('证书已打包下载')
      } catch (error) {
        Toast.fail((error && error.message) || '下载失败')
      } finally {
        this.batchDownloading = null
      }
    },
    onPreviewPhoto(url) {
      this.previewPhotoUrl = apiUrl(url)
    },
    // 证书预览 - 在弹窗中展示渲染后的证书图片
    onPreviewCert(cert) {
      this.certPreview = {
        visible: true,
        url: buildCertificateDownloadUrl(cert, 'image'),
        certName: cert.name + ' - ' + (cert.certNo || cert.id),
        loading: true,
        error: false
      }
    },
    onPreviewLoad() {
      this.certPreview.loading = false
      this.certPreview.error = false
    },
    onPreviewError() {
      this.certPreview.loading = false
      this.certPreview.error = true
    },
    // ===== 证书预览缩放 =====
    onPreviewWheel(e) {
      var delta = e.deltaY > 0 ? -0.15 : 0.15
      this.previewScale = Math.max(0.3, Math.min(5, this.previewScale + delta))
    },
    onZoomIn() { this.previewScale = Math.min(5, this.previewScale + 0.25) },
    onZoomOut() { this.previewScale = Math.max(0.3, this.previewScale - 0.25) },
    onZoomReset() { this.previewScale = 1 },
    onTouchStart(e) {
      if (e.touches.length === 2) {
        this.previewTouchStart = Math.hypot(
          e.touches[0].clientX - e.touches[1].clientX,
          e.touches[0].clientY - e.touches[1].clientY
        )
      }
    },
    onTouchMove(e) {
      if (e.touches.length === 2 && this.previewTouchStart) {
        var dist = Math.hypot(
          e.touches[0].clientX - e.touches[1].clientX,
          e.touches[0].clientY - e.touches[1].clientY
        )
        var ratio = dist / this.previewTouchStart
        this.previewScale = Math.max(0.3, Math.min(5, this.previewScale * ratio))
        this.previewTouchStart = dist
      }
    },
    onTouchEnd() { this.previewTouchStart = null },
    // 封面图加载失败时，隐藏 img 让默认图标占位显示（避免裂图）
    onCoverError(e) {
      if (e && e.target) {
        e.target.style.display = 'none'
      }
    },
    onToggleAll(e) {
      if (e.target.checked) {
        this.selectedIds = this.records.map(r => r.id)
      } else {
        this.selectedIds = []
      }
    },
    // 勾选部分证书打包下载(ZIP) — 用户自主选择下载哪几张
    async onDownloadSelected(format) {
      if (this.selectedIds.length === 0) {
        Toast('请先勾选要下载的证书')
        return
      }
      if (this.selectedIds.length === 1) {
        var cert = this.records.find(r => r.id === this.selectedIds[0])
        if (cert) {
          this.onDownload(cert, format)
          return
        }
      }
      this.selectedDownloading = format
      try {
        var idCard = (this.form.idCard || '').trim()
        var name = (this.form.name || '').trim()
        var { fileName, blob } = await downloadSelectedCertificates(idCard, name, this.selectedIds, format)
        saveBlob(blob, fileName)
        Toast.success('已下载选中的 ' + this.selectedIds.length + ' 张证书')
      } catch (error) {
        Toast.fail((error && error.message) || '下载失败')
      } finally {
        this.selectedDownloading = null
      }
    },
    onClosePreview() {
      this.previewPhotoUrl = ''
    },
    async onPaperLogin() {
      if (!this.paperLogin.account || !this.paperLogin.password) {
        Toast('请输入手机号/身份证号和密码')
        return
      }
      this.paperLogin.submitting = true
      try {
        // 前端自动识别: 11位纯数字 → phone, 其他 → idCard
        const v = this.paperLogin.account.trim()
        const loginType = /^\d{11}$/.test(v) ? 'phone' : 'idCard'
        const res = await login({
          username: v,
          password: this.paperLogin.password,
          role: 'student',
          agreement: true,
          loginType: loginType
        })
        const data = res.data || res
        const token = data.token || data
        const userInfo = data.userInfo || data
        this.$store.dispatch('login', { token, userInfo })
        Toast.success('登录成功')
        this.fetchMyExams()
        this.fetchMyBestRecords()
      } catch (e) {
        Toast((e && (e.message || e.msg)) || '登录失败,请检查账号密码')
      } finally {
        this.paperLogin.submitting = false
      }
    },
    onPaperLogout() {
      this.$store.dispatch('logout')
      this.paperLogin = { account: '', password: '', submitting: false }
      this.myExams = []
      Toast('已退出登录')
    },
    async fetchMyExams() {
      this.paperLoading = true
      try {
        const res = await getMyExams()
        const data = res.data || res
        this.myExams = Array.isArray(data) ? data : (data.records || data.list || [])
        // user/exam/list 返回的 coverUrl 是权威来源，补全到 myExamRecords 封面图
        this.mergeCoverUrl()
      } catch (e) {
        this.myExams = []
        Toast((e && (e.message || e.msg)) || '获取已开通考试失败')
      } finally {
        this.paperLoading = false
      }
    },
    async fetchMyCertificates() {
      this.myCertLoading = true
      this.searching = true
      try {
        const res = await getMyCertificates()
        const data = res.data || res || []
        this.records = Array.isArray(data) ? data : (data.records || data.list || [])
        this.total = this.records.length
        this.searched = true
        this.selectedIds = []
        if (this.records.length > 0) {
          Toast.success('找到 ' + this.records.length + ' 张证书')
        } else {
          Toast('未找到您的证书')
        }
      } catch (error) {
        this.records = []
        this.total = 0
        this.searched = true
        Toast.fail((error && error.message) || '查询失败')
      } finally {
        this.myCertLoading = false
        this.searching = false
      }
    },
    async fetchMyBestRecords() {
      if (!this.isLoggedIn) return
      try {
        const res = await getMyExamRecords()
        const data = res.data || res || []
        this.myExamRecords = Array.isArray(data) ? data : (data.records || data.list || [])
        // my-exam-records 接口可能未返回 coverUrl 或值为空，用 myExams(user/exam/list) 补全
        this.mergeCoverUrl()
      } catch (error) {
        this.myExamRecords = []
      }
    },
    // 用 myExams(user/exam/list，权威含 coverUrl) 的封面图补全 myExamRecords。
    // 两个接口异步并行加载，谁后完成谁触发合并，确保封面图一定能拿到。
    mergeCoverUrl() {
      if (!this.myExams || this.myExams.length === 0 || !this.myExamRecords || this.myExamRecords.length === 0) return
      // 以 examId 为键建立 coverUrl 索引
      const coverMap = {}
      this.myExams.forEach((e) => {
        if (e && e.id != null && e.coverUrl) {
          coverMap[e.id] = e.coverUrl
        }
      })
      // 给 myExamRecords 中 coverUrl 为空的记录补上封面图
      this.myExamRecords.forEach((r) => {
        if ((!r.coverUrl || r.coverUrl === '') && r.examId != null && coverMap[r.examId]) {
          this.$set(r, 'coverUrl', coverMap[r.examId])
        }
      })
    },
    async openPaper(exam) {
      this.paperDialog.visible = true
      this.paperDialog.examId = exam.id
      this.paperDialog.examName = exam.name
      this.paperDialog.questions = []
      this.paperDialog.answers = {}
      this.paperDialog.loading = true
      try {
        const res = await viewPaper(exam.id)
        const data = res.data || res
        this.paperDialog.questions = data.questions || []
        this.paperDialog.answers = data.answers || {}
      } catch (e) {
        Toast((e && (e.message || e.msg)) || '加载试卷失败')
        this.paperDialog.questions = []
      } finally {
        this.paperDialog.loading = false
      }
    },
    openPaperFromRecord(item) {
      // 跳转到考试详情页：携带 detail=1，直接展示完整试卷+答案+解析（而非简洁成绩页）
      this.$router.push('/exam/result/' + item.recordId + '?examId=' + item.examId + '&detail=1')
    },
    questionTypeLabel(type) {
      const map = { 1: '单选', 2: '多选', 3: '填空', 4: '判断', 5: '简答' }
      return map[type] || '题'
    },
    viewExamResult(cert) {
      if (cert.examRecordId) {
        this.$router.push('/exam/result/' + cert.examRecordId + '?examId=' + cert.examId)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.certificate-page { min-height: 100vh; background-color: #f5f5f5; }
.page-body { padding-top: var(--header-height, 178px); }
.container { width: 100%; max-width: 1200px; margin: 0 auto; padding: 24px 20px; }

.page-title {
  display: flex; align-items: center; gap: 8px; margin-bottom: 20px;
  .title-text { font-size: 22px; font-weight: 600; color: #303133; }
  .title-sub { margin-left: 12px; font-size: 13px; color: #909399; font-weight: normal; }
}

.cert-layout { display: flex; gap: 20px; align-items: flex-start; }
.sidebar { flex: 0 0 320px; }
.content-area { flex: 1; min-width: 0; }

.my-cert-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #e8f3ff;
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #1989fa;
  span { flex: 1; }
}
.score-pass { color: #67c23a; }
.score-fail { color: #f56c6c; }
.exam-score span { font-weight: 600; }

/* 证书查询 / 试卷查询 标签按钮：加背景色，区分选中/未选中态 */
.main-tabs {
  margin-bottom: 4px;
}
.main-tabs ::v-deep .van-tabs__wrap { height: 48px; }
.main-tabs ::v-deep .van-tabs__nav {
  background: transparent;
  padding: 0;
  gap: 12px;
}
.main-tabs ::v-deep .van-tab {
  flex: 0 0 auto;
  min-width: 120px;
  height: 40px;
  padding: 0 22px;
  border-radius: 20px;
  background: #ffffff;
  color: #606266;
  font-size: 15px;
  border: 1px solid #dcdfe6;
  transition: all .2s;
  z-index: 1;
}
.main-tabs ::v-deep .van-tab--active {
  background: #1989fa;
  color: #ffffff;
  font-weight: 600;
  border-color: #1989fa;
  box-shadow: 0 2px 8px rgba(25, 137, 250, 0.3);
}
/* 选中态用背景色区分，隐藏 vant 默认下划线 */
.main-tabs ::v-deep .van-tabs__line { display: none; }
.main-tabs ::v-deep .van-tab__text { color: inherit; }

.card {
  background-color: #fff; border-radius: 8px; overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.card-header {
  display: flex; align-items: center; gap: 8px; padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0; font-size: 15px; font-weight: 600;
  color: #303133; background: #fafbfc;
}
.card-body { padding: 20px; }

.btn-group {
  display: flex; flex-direction: column; gap: 8px; padding: 12px 16px;
}
.login-tip { display: flex; flex-direction: column; gap: 8px; margin-top: 12px; max-width: 360px; }
.login-field { background: #fafbfc; border-radius: 6px; }
.logout-link { margin-left: auto; color: #f56c6c; font-size: 12px; cursor: pointer; }

.placeholder {
  text-align: center; padding: 60px 20px; color: #909399;
  .sub { font-size: 12px; color: #c0c4cc; margin-top: 4px; }
}
.result-header {
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  .header-left { display: flex; align-items: center; gap: 8px; }
  .header-right { display: flex; align-items: center; gap: 8px; }
  .result-count { color: #909399; font-weight: normal; font-size: 13px; em { font-style: normal; color: #1989fa; font-weight: 600; } }
}

.result-table { overflow-x: scroll; }

.cert-table {
  width: 100%; border-collapse: collapse; font-size: 13px;
  th, td { border: 1px solid #ebeef5; padding: 10px 8px; text-align: center; white-space: nowrap; }
  th { background: #fafbfc; color: #606266; font-weight: 600; text-align: center; }
  input[type="checkbox"] { cursor: pointer; width: 16px; height: 16px; }
}
.cert-row.row-selected { background: #ecf5ff; }
.photo-cell {
  cursor: pointer;
  .photo-thumb { width: 36px; height: 48px; object-fit: cover; border-radius: 2px; }
}
.name-cell { display: flex; align-items: center; gap: 4px; }
.cert-no { color: #1989fa; font-family: monospace; }
.id-card { font-family: monospace; }
.profession { color: #67c23a; }
.level { background: #f0f9ff; color: #1989fa; padding: 2px 6px; border-radius: 3px; font-size: 12px; }
.action-cell { display: flex; gap: 4px; }
.preview-link {
  color: #67c23a;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 10px;
  background: #f0f9eb;
  border-radius: 4px;
  transition: all 0.2s;
  &:hover { background: #e1f3d8; color: #52c41a; }
}

.tag-success { display: inline-block; background: #f0f9eb; color: #67c23a; font-size: 12px; padding: 2px 6px; border-radius: 3px; }
.tag-info { display: inline-block; background: #f4f4f5; color: #909399; font-size: 12px; padding: 2px 6px; border-radius: 3px; }
.tag-danger { display: inline-block; background: #fef0f0; color: #f56c6c; font-size: 12px; padding: 2px 6px; border-radius: 3px; }
.tag-warning { display: inline-block; background: #fdf6ec; color: #e6a23c; font-size: 11px; padding: 1px 4px; border-radius: 3px; }
.tag-success-sm { display: inline-block; background: #f0f9eb; color: #67c23a; font-size: 11px; padding: 1px 4px; border-radius: 3px; }

.photo-preview-modal {
  position: fixed; inset: 0; background: rgba(0,0,0,0.85); z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  .preview-content { position: relative; .preview-image { max-width: 90vw; max-height: 90vh; border-radius: 4px; } .close-btn { position: absolute; top: -36px; right: 0; cursor: pointer; } }
}

.cert-preview-modal {
  position: fixed; inset: 0; background: rgba(0,0,0,0.85); z-index: 9999;
  display: flex; align-items: center; justify-content: center; padding: 20px;
  .cert-preview-content {
    position: relative; max-width: 95vw; max-height: 95vh;
    display: flex; flex-direction: column;
  }
  .cert-preview-header {
    display: flex; align-items: center; justify-content: space-between;
    color: #fff; font-size: 14px; padding: 8px 12px; margin-bottom: 8px;
    .close-btn { cursor: pointer; }
  }
  .cert-preview-body {
    display: flex; align-items: center; justify-content: center;
    overflow: auto; max-height: 85vh;
    img { max-width: 90vw; max-height: 80vh; object-fit: contain; border-radius: 4px; }
    .zoom-controls {
      position: fixed;
      bottom: 30px;
      left: 50%;
      transform: translateX(-50%);
      display: flex;
      align-items: center;
      gap: 8px;
      background: rgba(0, 0, 0, 0.6);
      padding: 6px 16px;
      border-radius: 20px;
      z-index: 10001;
    }
    .zoom-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.15);
      color: #fff;
      font-size: 20px;
      cursor: pointer;
      user-select: none;
    }
    .zoom-btn:active { background: rgba(255, 255, 255, 0.3); }
    .zoom-info {
      color: #fff;
      font-size: 13px;
      min-width: 40px;
      text-align: center;
      user-select: none;
    }
  }
  .preview-error, .preview-loading {
    display: flex; flex-direction: column; align-items: center; gap: 12px;
    color: #909399; padding: 60px; p { font-size: 14px; }
  }
}

.paper-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.paper-loading { display: flex; justify-content: center; padding: 60px 0; }
.paper-card {
  background: #fff; border: 1px solid #f0f0f0; border-radius: 8px; overflow: hidden;
  cursor: pointer; transition: all 0.3s; display: flex; flex-direction: column;
  &:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(25,137,250,0.15); border-color: #1989fa; }
  .paper-cover {
    width: 100%; height: 120px; background: #f5f7fa; display: flex; align-items: center; justify-content: center; overflow: hidden;
    img { width: 100%; height: 100%; object-fit: cover; }
    .paper-cover-default { width: 60px; height: 60px; border-radius: 50%; background: #e8f3ff; display: flex; align-items: center; justify-content: center; }
  }
  .paper-content { padding: 12px 14px 14px; display: flex; flex-direction: column; gap: 8px; flex: 1; }
  .paper-name { font-size: 14px; font-weight: 600; color: #303133; line-height: 1.4; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
  .paper-meta { display: flex; flex-wrap: wrap; gap: 8px 12px; font-size: 12px; color: #909399; span { display: inline-flex; align-items: center; gap: 2px; } }
  .paper-record { margin-top: 2px; }
  .paper-action { margin-top: auto; text-align: right; }
}
.paper-empty { grid-column: 1 / -1; padding: 40px 0; }

.paper-popup { display: flex; flex-direction: column; height: 100%; }
.paper-popup-title { font-size: 16px; font-weight: 600; text-align: center; padding: 16px; border-bottom: 1px solid #f0f0f0; }
.paper-preview { flex: 1; overflow-y: auto; padding: 16px; }
.paper-question {
  margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px dashed #ebeef5;
  &:last-child { border-bottom: none; }
  .q-title { display: flex; flex-wrap: wrap; align-items: flex-start; gap: 6px; font-size: 14px; line-height: 1.6; color: #303133; margin-bottom: 8px;
    .q-no { font-weight: 600; color: #1989fa; }
    .q-type { color: #909399; font-size: 12px; }
    .q-score { color: #f56c6c; font-size: 12px; }
    .q-content { flex: 1; }
  }
  .q-options { margin-left: 24px; display: flex; flex-direction: column; gap: 4px; }
  .q-option { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #606266; .opt-label { color: #1989fa; font-weight: 600; min-width: 18px; } }
  .q-correct, .q-answer-block { margin-left: 24px; margin-top: 6px; font-size: 12px; color: #909399; }
  .q-correct-text { color: #67c23a; font-weight: 500; }
  .q-answer-text { color: #e6a23c; font-weight: 500; }
}

@media (max-width: 1100px) { .paper-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 720px) {
  .paper-grid { grid-template-columns: 1fr; }
  .cert-layout { flex-direction: column; }
  .sidebar { flex: 0 0 auto; width: 100%; }
}

.download-format-options {
  display: flex;
  gap: 10px;
  justify-content: center;
  padding: 10px 0;
}

.format-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 16px;
  border: 2px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  min-width: 110px;
  transition: all 0.3s;
  &:hover {
    border-color: #1989fa;
    background: #f5f7fa;
  }
}

.format-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-top: 8px;
}

.format-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* 移动端适配:768px 以下,表格转卡片式 */
@media (max-width: 768px) {
  .container { width: 100%; padding: 12px; }
  .cert-layout { flex-direction: column; }
  .sidebar { flex: 0 0 auto; width: 100%; padding: 0; }
  .sidebar .card-body { padding: 12px; }
  .cert-table thead { display: none; }
  .cert-table tbody { display: block; width: 100%; }
  .cert-table tr {
    display: block;
    width: 100%;
    border-bottom: 2px solid #ebeef5;
  }
  .cert-table td {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border: none;
    border-bottom: 1px solid #f0f0f0;
    text-align: right;
    white-space: normal;
    padding: 8px 10px;
  }
  .cert-table td::before {
    content: attr(data-label);
    font-weight: 600;
    color: #909399;
    margin-right: 12px;
    flex-shrink: 0;
    text-align: left;
  }
  .main-tabs ::v-deep .van-tab { min-width: auto; }
}
</style>
