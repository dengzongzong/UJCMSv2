<template>
  <div class="app-container">
    <el-card shadow="never" v-loading="loading">
      <div slot="header">
        <span>网站声明设置</span>
        <el-tooltip content="用户在学员端首页底部点击『网站声明』时弹窗内的展示内容" placement="top" style="margin-left: 8px">
          <i class="el-icon-question"></i>
        </el-tooltip>
      </div>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" style="max-width: 880px">
        <el-form-item label="声明标题" prop="title">
          <el-input v-model="form.title" placeholder="例如:网站声明" maxlength="200" />
        </el-form-item>
        <el-form-item label="声明内容" prop="content">
          <RichEditor v-model="form.content" :height="420" placeholder="网站声明正文(支持富文本)" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitForm">保 存</el-button>
          <el-button @click="fetchDetail">重 置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getDeclaration, updateDeclaration } from '@/api/feedback'
import RichEditor from '@/components/RichEditor'

export default {
  name: 'SettingDeclaration',
  components: { RichEditor },
  data() {
    return {
      loading: false,
      submitting: false,
      form: { title: '网站声明', content: '' },
      rules: {
        title: [{ required: true, message: '请输入声明标题', trigger: 'blur' }],
        content: [{ required: true, message: '请输入声明内容', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.fetchDetail()
  },
  methods: {
    fetchDetail() {
      this.loading = true
      getDeclaration()
        .then((res) => {
          const d = (res && res.data) || {}
          this.form = { title: d.title || '网站声明', content: d.content || '' }
        })
        .finally(() => {
          this.loading = false
        })
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return
        this.submitting = true
        updateDeclaration(this.form)
          .then(() => {
            this.$message.success('保存成功')
          })
          .finally(() => {
            this.submitting = false
          })
        })
    }
  }
}
</script>
