<template>
  <!-- 投诉建议弹窗 -->
  <van-popup
    :value="value"
    @input="$emit('input', $event)"
    position="center"
    round
    close-on-click-overlay
    :style="{ width: '90%', maxWidth: '520px' }"
  >
    <div class="complaint-modal">
      <div class="modal-header">
        <span class="modal-title">投诉建议</span>
        <van-icon name="cross" class="modal-close" @click="onClose" />
      </div>
      <div class="modal-body">
        <div class="modal-tip">您的反馈将帮助我们持续改进,感谢您的支持!</div>
        <van-cell-group class="form-group">
          <van-field v-model="form.contactName" placeholder="联系人" required clearable />
          <van-field v-model="form.phone" placeholder="联系电话" required clearable type="tel" />
          <van-field v-model="form.email" placeholder="邮箱(选填)" clearable type="email" />
          <van-field
            v-model="form.content"
            placeholder="请详细描述您的投诉或建议"
            type="textarea"
            rows="5"
            autosize
            maxlength="2000"
            show-word-limit
            required
          />
        </van-cell-group>
        <div class="form-actions">
          <van-button type="warning" block :loading="submitting" @click="onSubmit">提 交</van-button>
        </div>
      </div>
    </div>
  </van-popup>
</template>

<script>
import { submitFeedback } from '@/api/feedback'
import { Toast } from 'vant'

export default {
  name: 'ComplaintDialog',
  props: {
    value: { type: Boolean, default: false }
  },
  data() {
    return {
      form: { contactName: '', phone: '', email: '', content: '' },
      submitting: false
    }
  },
  methods: {
    onClose() { this.$emit('input', false) },
    async onSubmit() {
      const f = this.form
      if (!f.contactName || !f.phone || !f.content) {
        Toast('请填写联系人、电话和投诉建议内容')
        return
      }
      this.submitting = true
      try {
        await submitFeedback({
          type: 'complaint',
          contactName: f.contactName,
          phone: f.phone,
          email: f.email,
          content: f.content
        })
        Toast('提交成功,感谢您的反馈')
        this.form = { contactName: '', phone: '', email: '', content: '' }
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
.complaint-modal {
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
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
}
.modal-close {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
  cursor: pointer;
}
.modal-body { padding: 18px 20px 22px; }
.modal-tip {
  background: #fff7e8;
  color: #b88217;
  padding: 8px 12px;
  border-radius: 3px;
  font-size: 12px;
  margin-bottom: 12px;
}
.form-group { background: #fafbfc; border-radius: 4px; }
.form-actions { margin-top: 14px; }
::v-deep .van-cell { padding: 6px 8px; }
::v-deep .van-field__body { background: #fff; padding: 4px 8px; border-radius: 3px; }
</style>
