<template>
  <div class="rich-editor">
    <div ref="editorRef" class="editor-wrapper" :style="{ minHeight: height }"></div>
    <div v-if="!editorReady" class="rich-editor-loading">
      <i class="el-icon-loading"></i> 富文本编辑器加载中...
    </div>
  </div>
</template>

<script>
import { apiUrl } from '@/utils/apiBase'

/**
 * 富文本编辑器封装(基于 wangEditor 4,通过 CDN 在 index.html 中引入 window.wangEditor)
 * 不再依赖 npm 包 wangeditor,避免构建时 Module not found 错误。
 */
export default {
  name: 'RichEditor',
  props: {
    value: {
      type: String,
      default: ''
    },
    height: {
      type: String,
      default: '400px'
    }
  },
  data() {
    return {
      editor: null,
      editorReady: false
    }
  },
  mounted() {
    this.initEditor()
  },
  beforeDestroy() {
    if (this.editor) {
      this.editor.destroy()
      this.editor = null
    }
  },
  watch: {
    value(val) {
      if (this.editor && val !== this.editor.txt.html()) {
        this.editor.txt.html(val)
      }
    }
  },
  methods: {
    initEditor() {
      // wangEditor 4 通过 CDN 加载到 window.wangEditor
      if (typeof window.wangEditor === 'undefined') {
        this.$message.error('富文本编辑器未加载,请检查网络或刷新页面重试')
        return
      }
      // eslint-disable-next-line no-undef
      this.editor = new window.wangEditor(this.$refs.editorRef)
      this.editor.config.height = this.height
      this.editor.config.showFullScreen = false
      this.editor.config.showLinkImg = false
      this.editor.config.showUploadImg = true
      this.editor.config.uploadImgServer = '/api/file/upload'
      this.editor.config.uploadFileName = 'file'
      const token = localStorage.getItem('admin_token') || ''
      this.editor.config.uploadImgHeaders = token ? { Authorization: 'Bearer ' + token } : {}
      this.editor.config.uploadImgHooks = {
        success: (xhr, editor, result) => {
          const data = typeof result === 'string' ? JSON.parse(result) : result
          if (data.data) {
            const imgUrl = apiUrl(data.data)
            const img = editor.$textElem.find('img:last')
            if (img.length) {
              img.attr('src', imgUrl)
            }
          }
        },
        customInsert: (insertImg, result, editor) => {
          const data = typeof result === 'string' ? JSON.parse(result) : result
          if (data.data) {
            const imgUrl = apiUrl(data.data)
            insertImg(imgUrl)
          }
        }
      }
      this.editor.config.onchange = (html) => {
        this.$emit('input', html)
      }
      this.editor.create()
      if (this.value) {
        this.editor.txt.html(this.value)
      }
      this.editorReady = true
    },
    getHtml() {
      return this.editor ? this.editor.txt.html() : ''
    },
    clear() {
      if (this.editor) {
        this.editor.txt.clear()
        this.$emit('input', '')
      }
    }
  }
}
</script>

<style scoped>
.rich-editor {
  width: 100%;
}
.editor-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.rich-editor-loading {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 13px;
  z-index: 10;
  pointer-events: none;
}
</style>
