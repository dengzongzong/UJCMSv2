<template>
  <!-- 合作咨询弹窗 - 纯自定义实现 -->
  <div v-if="value" class="coop-overlay" @click.self="onClose">
    <div class="coop-modal">
      <!-- 顶部标题栏 -->
      <div class="modal-header">
        <span class="modal-title">合作咨询</span>
        <van-icon name="cross" class="modal-close" @click="onClose" />
      </div>

      <!-- 三栏布局 -->
      <div class="modal-body">
        <!-- 左栏:单位背景 -->
        <div class="modal-col modal-col-left">
          <div class="col-title">单位背景</div>
          <div class="col-content rich-content" v-html="processRichContent(setting.intro || '欢迎合作,共创人才测评未来。')"></div>
          <div class="col-contact-block">
            <div class="col-subtitle">联系我们</div>
            <div class="col-phone">电话:{{ setting.phone1 || '010-xxxxxxxx' }}</div>
          </div>
        </div>

        <!-- 中栏:流程 / 联系方式 / 意向表 -->
        <div class="modal-col modal-col-mid">
          <div class="col-flow-btn">合作流程</div>
          <div class="col-flow-list" v-if="setting.processDesc">
            <div v-for="(line, idx) in processLines" :key="idx" class="col-flow-line">
              {{ line }}
            </div>
            <div v-if="processLines.length === 0" class="col-empty">暂无流程说明</div>
          </div>

          <div class="col-mid-block">
            <div class="col-subtitle">联系我们</div>
            <div class="col-phone-line" v-if="setting.phone1">电话:{{ setting.phone1 }}</div>
            <div class="col-phone-line" v-if="setting.phone2">电话:{{ setting.phone2 }}</div>
          </div>

          <div class="col-mid-block">
            <div class="col-subtitle">意向表下载</div>
            <a v-if="setting.attachmentUrl"
               :href="resolveFile(setting.attachmentUrl)"
               :download="setting.attachmentName || true"
               class="col-attachment">
              <van-icon name="description" /> {{ setting.attachmentName || '合作意向表' }}
            </a>
            <div v-else class="col-empty">暂未上传附件</div>
            <div class="col-email" v-if="setting.email1">邮箱:{{ setting.email1 }}</div>
            <div class="col-email" v-if="setting.email2">邮箱:{{ setting.email2 }}</div>
          </div>
        </div>

        <!-- 右栏:留言表单 -->
        <div class="modal-col modal-col-right">
          <div class="col-right-title">给我们留言</div>
          <van-cell-group class="form-group">
            <van-field v-model="form.orgName" placeholder="单位名称" clearable />
            <van-field v-model="form.contactName" placeholder="联系人" required clearable />
            <van-field v-model="form.phone" placeholder="电话" required clearable type="tel" />
            <van-field
              v-model="form.content"
              placeholder="合作意向"
              type="textarea"
              rows="4"
              autosize
              maxlength="2000"
              show-word-limit
            />
          </van-cell-group>
          <div class="form-actions">
            <van-button type="info" block :loading="submitting" @click="onSubmit">提 交</van-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getCooperationSetting, submitFeedback } from '@/api/feedback'
import { Toast } from 'vant'
import { resolveImg, apiUrl, processRichContent } from '@/utils/apiBase'

export default {
  name: 'CooperationDialog',
  props: {
    value: { type: Boolean, default: false }
  },
  data() {
    return {
      setting: {},
      form: { orgName: '', contactName: '', phone: '', content: '' },
      submitting: false,
      loaded: false
    }
  },
  computed: {
    hasFlowContent() {
      const text = (this.setting && this.setting.processDesc) || ''
      return text.replace(/<[^>]+>/g, '').trim().length > 0
    },
    processLines() {
      const text = (this.setting && this.setting.processDesc) || ''
      return text.split(/\n|<br\s*\/?>/i).filter(line => line.trim().length > 0)
    }
  },
  watch: {
    value(v) {
      if (v && !this.loaded) {
        this.loadSetting()
      }
    }
  },
  methods: {
    apiUrl,
    resolveFile(u) {
      return apiUrl(u)
    },
    resolveImg,
    processRichContent,
    onClose() {
      this.$emit('input', false)
    },
    async loadSetting() {
      try {
        const res = await getCooperationSetting()
        this.setting = (res && res.data) || {}
        this.loaded = true
      } catch (e) {
        this.setting = {}
      }
    },
    async onSubmit() {
      const f = this.form
      if (!f.contactName || !f.phone || !f.content) {
        Toast('请填写联系人、电话和合作意向')
        return
      }
      this.submitting = true
      try {
        await submitFeedback({
          type: 'cooperation',
          orgName: f.orgName,
          contactName: f.contactName,
          phone: f.phone,
          content: f.content
        })
        Toast('提交成功,我们会尽快与您联系')
        this.form = { orgName: '', contactName: '', phone: '', content: '' }
        this.onClose()
      } catch (e) {
        Toast((e && (e.message || e.msg)) || '提交失败,请稍后重试')
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.coop-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.coop-modal {
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.18);
  width: 92%;
  max-width: 900px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}
.modal-header {
  position: relative;
  height: 44px;
  background: #1989fa;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;

  .modal-close {
    position: absolute;
    right: 12px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 20px;
    cursor: pointer;
  }
}
.modal-body {
  display: grid;
  grid-template-columns: 1fr 1.2fr 1.2fr;
  min-height: 360px;
  background: #fff;
  overflow-y: auto;
}
.modal-col {
  padding: 18px 18px 22px;
  border-right: 1px solid #f0f0f0;
  font-size: 13px;
  color: #333;
  line-height: 1.7;
  &:last-child { border-right: none; }
}
.col-title,
.col-right-title {
  font-size: 15px;
  font-weight: 600;
  color: #1989fa;
  margin-bottom: 10px;
}
.col-content {
  color: #555;
  margin-bottom: 18px;
  font-size: 13px;
}
.col-content.rich-content {
  white-space: normal;
}
.col-subtitle {
  font-weight: 600;
  color: #333;
  margin: 8px 0 6px;
  font-size: 14px;
}
.col-phone,
.col-phone-line,
.col-email {
  color: #555;
  font-size: 13px;
  margin-bottom: 4px;
}
.col-mid-block { margin-top: 12px; }
.col-flow-btn {
  display: inline-block;
  background: #1989fa;
  color: #fff;
  padding: 6px 14px;
  border-radius: 3px;
  font-size: 13px;
  margin-bottom: 8px;
  font-weight: 500;
}
.col-flow-list { margin-bottom: 12px; }
.col-flow-line {
  position: relative;
  padding: 4px 0 4px 16px;
  color: #333;
  font-size: 13px;
  &::before {
    content: '';
    position: absolute;
    left: 4px;
    top: 12px;
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: #1989fa;
  }
}
.col-attachment {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #1989fa;
  text-decoration: none;
  font-size: 13px;
  margin-bottom: 6px;
  word-break: break-all;

  &:hover { text-decoration: underline; }
}
.col-empty {
  color: #999;
  font-size: 12px;
  margin: 4px 0;
}
.form-group { background: #fafbfc; border-radius: 4px; }
.form-actions {
  margin-top: 14px;
  padding: 0 4px;
}
::v-deep .van-cell { padding: 6px 8px; }
::v-deep .van-field__body { background: #fff; padding: 4px 8px; border-radius: 3px; }

@media (max-width: 720px) {
  .modal-body { grid-template-columns: 1fr; }
  .modal-col { border-right: none; border-bottom: 1px solid #f0f0f0; }
  .modal-col:last-child { border-bottom: none; }
}
</style>
