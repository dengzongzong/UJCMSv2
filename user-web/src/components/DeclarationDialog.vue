<template>
  <!-- 网站声明弹窗 -->
  <van-popup
    :value="value"
    @input="$emit('input', $event)"
    position="center"
    round
    close-on-click-overlay
    :style="{ width: '90%', maxWidth: '720px', maxHeight: '80vh', overflow: 'hidden' }"
  >
    <div class="declaration-modal">
      <div class="modal-header">
        <span class="modal-title">{{ declaration.title || '网站声明' }}</span>
        <van-icon name="cross" class="modal-close" @click="onClose" />
      </div>
      <div class="modal-body" :class="{ 'is-loading': loading }">
        <van-loading v-if="loading" size="20" class="modal-loading">加载中...</van-loading>
        <div class="declaration-content rich-content" v-html="processRichContent(declaration.content || '暂无声明内容')"></div>
        <div v-if="declaration.updateTime" class="declaration-time">
          最后更新:{{ declaration.updateTime }}
        </div>
      </div>
    </div>
  </van-popup>
</template>

<script>
import { getDeclaration } from '@/api/feedback'
import { processRichContent } from '@/utils/apiBase'

export default {
  name: 'DeclarationDialog',
  props: {
    value: { type: Boolean, default: false }
  },
  data() {
    return {
      declaration: {},
      loading: false
    }
  },
  watch: {
    value(v) {
      if (v) this.load()
    }
  },
  methods: {
    processRichContent,
    onClose() { this.$emit('input', false) },
    async load() {
      this.loading = true
      try {
        const res = await getDeclaration()
        this.declaration = (res && res.data) || {}
      } catch (e) {
        this.declaration = { title: '网站声明', content: '获取失败,请稍后重试' }
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.declaration-modal {
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 80vh;
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
}
.modal-close {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
  cursor: pointer;
}
.modal-body {
  padding: 20px 24px 24px;
  overflow-y: auto;
  font-size: 14px;
  color: #333;
  line-height: 1.85;
}
.declaration-content {
  word-break: break-word;
}
.declaration-content.rich-content {
  white-space: normal;
  font-size: 14px;
  color: #333;
  line-height: 1.85;
}
.declaration-content.rich-content ::v-deep p { margin: 8px 0; }
.declaration-content.rich-content ::v-deep img { max-width: 100%; }
.declaration-time {
  margin-top: 16px;
  color: #999;
  font-size: 12px;
  text-align: right;
}
</style>
