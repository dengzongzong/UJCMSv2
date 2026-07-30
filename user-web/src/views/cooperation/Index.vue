<template>
  <div class="cooperation-page">
    <Header />

    <div class="query-container">
      <div class="query-card">
        <div class="query-title">
          <i class="icon-search"></i>
          <span>合作单位查询</span>
        </div>
        <div class="query-form">
          <div class="form-row">
            <label class="form-label"><span class="required">*</span> 单位名称</label>
            <input v-model="query.unitName" type="text" placeholder="请输入单位名称" class="form-input" @keyup.enter="handleQuery" />
          </div>
          <div class="form-row">
            <label class="form-label"><span class="required">*</span> 授权管理编号</label>
            <input v-model="query.authCode" type="text" placeholder="请输入授权管理编号" class="form-input" @keyup.enter="handleQuery" />
          </div>
          <div class="form-row form-actions">
            <button class="btn-query" :disabled="loading" @click="handleQuery">{{ loading ? '查询中...' : '查 询' }}</button>
            <button class="btn-reset" @click="handleReset">重 置</button>
          </div>
        </div>
      </div>

      <div v-if="queried && !loading" class="result-section">
        <div v-if="resultList.length === 0" class="empty-tip">
          <p>未查询到相关合作单位信息</p>
          <p class="empty-sub">请检查单位名称和授权管理编号是否正确</p>
        </div>
        <div v-else class="result-list">
          <div v-for="item in resultList" :key="item.id" class="result-card" @click="showDetail(item)">
            <div class="result-card-header">
              <span class="result-unit-name">{{ item.unitName }}</span>
              <span v-if="item.status !== 1" :class="['result-status', 'status-' + item.status]">{{ statusText(item.status) }}</span>
            </div>
            <div class="result-card-body">
              <div class="result-info-left">
                <div class="result-item">
                  <span class="result-label">授权编号：</span>
                  <span class="result-value">{{ item.authCode || '—' }}</span>
                </div>
                <div class="result-item">
                  <span class="result-label">主营业务：</span>
                  <span class="result-value">{{ item.mainBusiness || '—' }}</span>
                </div>
                <div class="result-item">
                  <span class="result-label">联系人：</span>
                  <span class="result-value">{{ item.contactName || '—' }}</span>
                </div>
                <div class="result-item">
                  <span class="result-label">联系电话：</span>
                  <span class="result-value">{{ item.contactPhone || '—' }}</span>
                </div>
                <div class="result-item">
                  <span class="result-label">授权日期：</span>
                  <span class="result-value">{{ authDateText(item) }}</span>
                </div>
              </div>
              <!-- 证书图片+富文本覆盖展示 -->
              <div v-if="certContent.imageUrl" class="result-cert-right" @click.stop>
                <div class="cert-image-overlay" @dblclick="openCertPreview">
                  <img :src="resolveCertImg(certContent.imageUrl)" alt="授权证书" class="cert-bg-img" />
                  <div class="cert-rich-text" v-html="certContent.richText"></div>
                </div>
                <div class="cert-preview-hint">双击预览证书</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="detailVisible" class="detail-overlay" @click="detailVisible = false">
      <div class="detail-modal" @click.stop>
        <div class="detail-header">
          <span>合作单位详情</span>
          <span class="detail-close" @click="detailVisible = false">×</span>
        </div>
        <div class="detail-body">
          <div class="detail-row"><span class="detail-label">单位名称</span><span class="detail-value">{{ detail.unitName }}</span></div>
          <div class="detail-row"><span class="detail-label">授权管理编号</span><span class="detail-value">{{ detail.authCode }}</span></div>
          <div class="detail-row"><span class="detail-label">主营业务</span><span class="detail-value">{{ detail.mainBusiness || '—' }}</span></div>
          <div class="detail-row"><span class="detail-label">合作意向</span><span class="detail-value">{{ detail.cooperationIntent || '—' }}</span></div>
          <div class="detail-row"><span class="detail-label">联系人</span><span class="detail-value">{{ detail.contactName || '—' }}</span></div>
          <div class="detail-row"><span class="detail-label">联系电话</span><span class="detail-value">{{ detail.contactPhone || '—' }}</span></div>
          <div class="detail-row" v-if="detail.status !== 1"><span class="detail-label">审核状态</span><span class="detail-value">{{ statusText(detail.status) }}</span></div>
          <div class="detail-row"><span class="detail-label">授权日期</span><span class="detail-value">{{ authDateText(detail) }}</span></div>
        </div>
      </div>
    </div>

    <!-- 证书图片预览弹窗 -->
    <div v-if="certPreviewVisible" class="cert-preview-overlay" @click="closeCertPreview">
      <div class="cert-preview-container" @click.stop>
        <div class="cert-preview-header">
          <span>授权证书预览</span>
          <span class="cert-preview-close" @click="closeCertPreview">×</span>
        </div>
        <div class="cert-preview-body" @wheel.prevent="onCertWheel" @touchstart.prevent="onCertTouchStart" @touchmove.prevent="onCertTouchMove" @touchend="onCertTouchEnd">
          <div class="cert-preview-content" :style="{ transform: 'scale(' + certPreviewScale + ')' }">
            <img :src="resolveCertImg(certContent.imageUrl)" alt="授权证书" class="cert-preview-img" />
            <div class="cert-preview-text" v-html="certContent.richText"></div>
          </div>
          <div class="cert-preview-controls">
            <span class="cert-zoom-btn" @click="certZoomIn">+</span>
            <span class="cert-zoom-info">{{ Math.round(certPreviewScale * 100) }}%</span>
            <span class="cert-zoom-btn" @click="certZoomOut">−</span>
            <span class="cert-zoom-btn" @click="certZoomReset">↺</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
import Header from '@/components/Header.vue'
import { resolveImg } from '@/utils/apiBase'

export default {
  name: 'CooperationUnit',
  components: { Header },
  data() {
    return {
      query: { unitName: '', authCode: '' },
      resultList: [],
      loading: false,
      queried: false,
      detailVisible: false,
      detail: {},
      certContent: { imageUrl: '', richText: '' },
      certLoaded: false,
      certPreviewVisible: false,
      certPreviewScale: 1,
      certTouchStart: null
    }
  },
  methods: {
    async fetchCertContent() {
      if (this.certLoaded) return
      try {
        const res = await request({ url: '/public/cooperation-cert-content', method: 'get' })
        const data = res.data || res
        if (data) {
          this.certContent.imageUrl = data.imageUrl || ''
          this.certContent.richText = data.richText || ''
        }
        this.certLoaded = true
      } catch (e) {
        // 无数据时静默处理
      }
    },
    resolveCertImg(url) {
      return resolveImg(url)
    },
    async handleQuery() {
      if (!this.query.unitName.trim()) {
        this.$toast('请输入单位名称')
        return
      }
      if (!this.query.authCode.trim()) {
        this.$toast('请输入授权管理编号')
        return
      }
      this.loading = true
      try {
        const res = await request({
          url: '/public/cooperation-apply/query',
          method: 'get',
          params: { unitName: this.query.unitName.trim(), authCode: this.query.authCode.trim() }
        })
        const data = res.data || res
        this.resultList = Array.isArray(data) ? data : (data.list || data.records || [])
        this.queried = true
        // 查到数据后加载证书内容
        if (this.resultList.length > 0) {
          this.fetchCertContent()
        }
      } catch (e) {
        this.$toast('查询失败，请稍后重试')
        this.resultList = []
        this.queried = true
      } finally {
        this.loading = false
      }
    },
    handleReset() {
      this.query.unitName = ''
      this.query.authCode = ''
      this.resultList = []
      this.queried = false
    },
    showDetail(item) {
      this.detail = item
      this.detailVisible = true
    },
    statusText(status) {
      const map = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
      return map[status] || '未知'
    },
    authDateText(item) {
      const start = item.authStartDate
      const end = item.authExpireDate
      if (!start && !end) return '—'
      const startStr = this.formatDateCN(start)
      const endStr = this.formatDateCN(end)
      // 判断是否过期
      let statusStr = ''
      if (end) {
        const today = new Date()
        today.setHours(0, 0, 0, 0)
        const endDate = new Date(end)
        endDate.setHours(0, 0, 0, 0)
        statusStr = endDate >= today ? '（有效）' : '（失效）'
      }
      return `${startStr}至${endStr}${statusStr}`
    },
    formatDateCN(dateStr) {
      if (!dateStr) return '—'
      const d = new Date(dateStr)
      if (isNaN(d.getTime())) return dateStr
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}年${m}月${day}日`
    },
    formatDate(dateStr) {
      return this.formatDateCN(dateStr)
    },
    // ===== 证书预览相关 =====
    openCertPreview() {
      this.certPreviewVisible = true
      this.certPreviewScale = 1
    },
    closeCertPreview() {
      this.certPreviewVisible = false
      this.certPreviewScale = 1
    },
    certZoomIn() {
      this.certPreviewScale = Math.min(this.certPreviewScale + 0.2, 5)
    },
    certZoomOut() {
      this.certPreviewScale = Math.max(this.certPreviewScale - 0.2, 0.2)
    },
    certZoomReset() {
      this.certPreviewScale = 1
    },
    onCertWheel(e) {
      if (e.deltaY < 0) this.certZoomIn()
      else this.certZoomOut()
    },
    onCertTouchStart(e) {
      if (e.touches.length === 2) {
        this.certTouchStart = Math.hypot(
          e.touches[0].clientX - e.touches[1].clientX,
          e.touches[0].clientY - e.touches[1].clientY
        )
      }
    },
    onCertTouchMove(e) {
      if (e.touches.length === 2 && this.certTouchStart) {
        var dist = Math.hypot(
          e.touches[0].clientX - e.touches[1].clientX,
          e.touches[0].clientY - e.touches[1].clientY
        )
        var scale = dist / this.certTouchStart
        this.certPreviewScale = Math.max(0.2, Math.min(5, scale))
      }
    },
    onCertTouchEnd() {
      this.certTouchStart = null
    }
  }
}
</script>

<style lang="scss" scoped>
.cooperation-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-top: var(--header-height, 178px);
}

.page-banner {
  background: linear-gradient(135deg, #c41e3a 0%, #a01530 100%);
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;

  .page-banner-inner {
    text-align: center;
    color: #fff;

    h1 {
      font-size: 28px;
      margin: 0 0 8px;
      letter-spacing: 4px;
    }
    p {
      font-size: 14px;
      opacity: 0.85;
      margin: 0;
    }
  }
}

.query-container {
  max-width: 900px;
  margin: 40px auto 60px;
  padding: 0 20px;
  position: relative;
  z-index: 2;
}

.query-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;

  .query-title {
    background: #f8f8f8;
    padding: 14px 24px;
    border-bottom: 1px solid #eee;
    font-size: 16px;
    font-weight: 600;
    color: #333;
    display: flex;
    align-items: center;
    gap: 8px;

    .icon-search {
      display: inline-block;
      width: 16px;
      height: 16px;
      border: 2px solid #c41e3a;
      border-radius: 50%;
      position: relative;

      &::after {
        content: '';
        position: absolute;
        right: -6px;
        bottom: -6px;
        width: 8px;
        height: 2px;
        background: #c41e3a;
        transform: rotate(45deg);
      }
    }
  }
}

.query-form {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;

  .form-row {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: 100%;

    &.form-actions {
      flex-direction: row;
      gap: 12px;
      margin-top: 6px;
    }
  }

  .form-label {
    font-size: 14px;
    color: #333;
    font-weight: 500;

    .required {
      color: #c41e3a;
      margin-right: 2px;
    }
  }

  .form-input {
    height: 40px;
    padding: 0 12px;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 14px;
    color: #333;
    outline: none;
    transition: border-color 0.2s;
    width: 100%;
    box-sizing: border-box;

    &:focus {
      border-color: #c41e3a;
    }
  }
}

.btn-query {
  height: 38px;
  padding: 0 32px;
  border: none;
  border-radius: 4px;
  background: #c41e3a;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover { background: #a01530; }
  &:disabled { background: #ccc; cursor: not-allowed; }
}

.btn-reset {
  height: 38px;
  padding: 0 32px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #c41e3a;
    color: #c41e3a;
  }
}

.result-section {
  margin-top: 24px;
}

.empty-tip {
  text-align: center;
  padding: 40px 0;
  color: #999;

  p { margin: 4px 0; }
  .empty-sub { font-size: 13px; }
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  }
}

.result-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;

  .result-unit-name {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }
}

.result-status {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;

  &.status-0 { background: #fff7e6; color: #fa8c16; }
  &.status-1 { background: #f6ffed; color: #52c41a; }
  &.status-2 { background: #fff1f0; color: #f5222d; }
}

.result-card-body {
  padding: 16px 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px 40px;
}

.result-info-left {
  flex: 1;
  min-width: 300px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.result-cert-right {
  flex-shrink: 0;
  width: 280px;
}

.cert-image-overlay {
  position: relative;
  width: 100%;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #eee;
}

.cert-bg-img {
  width: 100%;
  display: block;
}

.cert-rich-text {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 12px;
  overflow: hidden;
  pointer-events: none;
  font-size: 13px;
  line-height: 1.6;
}

.cert-preview-hint {
  text-align: center;
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}

/* 证书预览弹窗 */
.cert-preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cert-preview-container {
  background: #fff;
  border-radius: 8px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.cert-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #eee;
  font-size: 16px;
  font-weight: 600;

  .cert-preview-close {
    cursor: pointer;
    font-size: 24px;
    color: #999;
    line-height: 1;
    &:hover { color: #333; }
  }
}

.cert-preview-body {
  flex: 1;
  overflow: auto;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.cert-preview-content {
  position: relative;
  transform-origin: center center;
  transition: transform 0.15s ease;
  max-width: 100%;
}

.cert-preview-img {
  max-width: 100%;
  display: block;
}

.cert-preview-text {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20px;
  overflow: hidden;
  pointer-events: none;
  font-size: 14px;
  line-height: 1.6;
}

.cert-preview-controls {
  position: sticky;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.9);
  border-top: 1px solid #eee;
}

.cert-zoom-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #c41e3a;
  color: #fff;
  cursor: pointer;
  font-size: 16px;
  user-select: none;
  &:active { background: #a01530; }
}

.cert-zoom-info {
  font-size: 13px;
  color: #666;
  min-width: 48px;
  text-align: center;
}

.result-item {
  font-size: 14px;

  .result-label {
    color: #999;
  }
  .result-value {
    color: #333;
  }
}

/* 详情弹窗 */
.detail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-modal {
  background: #fff;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
  font-size: 18px;
  font-weight: 600;

  .detail-close {
    cursor: pointer;
    font-size: 24px;
    color: #999;
    line-height: 1;

    &:hover { color: #333; }
  }
}

.detail-body {
  padding: 20px 24px;
}

.detail-row {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;

  .detail-label {
    width: 120px;
    color: #999;
    font-size: 14px;
    flex-shrink: 0;
  }

  .detail-value {
    color: #333;
    font-size: 14px;
    flex: 1;
  }
}
</style>
