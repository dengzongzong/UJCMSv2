<template>
  <!--
    富文本编辑器封装(基于 wangEditor 4)
    用法:
      <RichEditor v-model="form.content" :height="360" placeholder="..." />
    - v-model 双向绑定 HTML 内容
    - 自动配置图片上传: 上传到 /admin/file/upload (同 upload.js 的 uploadFile)
    - 文本上传限制大小(默认 2MB)
  -->
  <div class="rich-editor-wrap">
    <div ref="editorContainer" class="rich-editor" :style="{ minHeight: height + 'px' }"></div>
    <div v-if="!editorReady" class="rich-editor-loading">
      <i class="el-icon-loading"></i> 富文本编辑器加载中...
    </div>
  </div>
</template>

<script>
import { uploadFile as uploadRequest } from '@/api/upload'
import store from '@/store'

export default {
  name: 'RichEditor',
  props: {
    value: { type: String, default: '' },
    height: { type: Number, default: 360 },
    placeholder: { type: String, default: '请输入内容...' },
    backgroundImage: { type: String, default: '' }
  },
  data() {
    return {
      editor: null,
      editorReady: false,
      // 防止外部 value 改变时监听器覆盖正在输入的内容
      isUserInput: false
    }
  },
  watch: {
    value(v) {
      if (this.editor && !this.isUserInput) {
        this.editor.txt.html(v || '')
      }
    },
    backgroundImage(v) {
      this.applyBackgroundImage(v)
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
  methods: {
    initEditor() {
      // wangEditor 4 暴露在 window 上
      if (typeof window.wangEditor === 'undefined') {
        this.$message.error('富文本编辑器未加载,请刷新页面重试')
        return
      }
      this.editor = new window.wangEditor(this.$refs.editorContainer)
      // 自定义菜单(常用富文本能力)
      this.editor.config.menus = [
        'head',
        'bold',
        'fontSize',
        'fontName',
        'italic',
        'underline',
        'strikeThrough',
        'foreColor',
        'backColor',
        'link',
        'list',
        'justify',
        'quote',
        'emoticon',
        'image',
        'table',
        'video',
        'code',
        'undo',
        'redo'
      ]
      this.editor.config.height = this.height
      this.editor.config.placeholder = this.placeholder
      this.editor.config.showLinkImg = false
      this.editor.config.uploadImgMaxSize = 2 * 1024 * 1024
      this.editor.config.uploadImgMaxLength = 5
      this.editor.config.uploadFileName = 'file'
      this.editor.config.uploadImgServer = '/api/file/upload'
      this.editor.config.uploadImgHeaders = {
        'Admin-Token': store.getters.token,
        Authorization: 'Bearer ' + store.getters.token
      }
      this.editor.config.uploadImgHooks = {
        customInsert: (insertImg, result) => {
          // 后端约定: { code:200, data:"/uploads/xxx.png" }
          const url =
            (result && (result.data || (result.data && result.data.url) || result.url)) || ''
          if (Array.isArray(url)) {
            url.forEach((u) => insertImg(u))
          } else if (url) {
            insertImg(url)
          } else if (typeof result === 'string') {
            insertImg(result)
          } else {
            this.$message.error('图片上传返回结果异常')
          }
        },
        before: () => {},
        error: (xhr) => {
          this.$message.error('图片上传失败: ' + (xhr && xhr.message))
        }
      }
      // 内容变化时回传 v-model
      this.editor.config.onchange = (newHtml) => {
        this.isUserInput = true
        this.$emit('input', newHtml)
        // 一段时间后重置标记,避免外部 v-model 覆盖
        clearTimeout(this._timer)
        this._timer = setTimeout(() => {
          this.isUserInput = false
        }, 200)
      }
      this.editor.create()
      if (this.value) {
        this.editor.txt.html(this.value)
      }
      this.editorReady = true
      // 初始化背景图
      if (this.backgroundImage) {
        this.applyBackgroundImage(this.backgroundImage)
      }
    },
    applyBackgroundImage(url) {
      if (!this.$refs.editorContainer) return
      var textArea = this.$refs.editorContainer.querySelector('.w-e-text')
      if (!textArea) return
      if (url) {
        textArea.style.backgroundImage = 'url("' + url + '")'
        textArea.style.backgroundSize = '100% 100%'
        textArea.style.backgroundRepeat = 'no-repeat'
        textArea.style.minHeight = this.height + 'px'
      } else {
        textArea.style.backgroundImage = ''
        textArea.style.backgroundSize = ''
        textArea.style.backgroundRepeat = ''
      }
    },
    /**
     * 对外暴露: 主动同步最新 HTML
     */
    getHtml() {
      return this.editor ? this.editor.txt.html() : ''
    },
    setHtml(html) {
      if (this.editor) this.editor.txt.html(html || '')
    }
  }
}
</script>

<style lang="scss" scoped>
.rich-editor-wrap {
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
}
.rich-editor {
  min-height: 360px;
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
::v-deep .w-e-toolbar {
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
}
::v-deep .w-e-text-container {
  border-bottom-left-radius: 4px;
  border-bottom-right-radius: 4px;
}
</style>
