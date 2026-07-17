<template>
  <div class="app-container">
    <el-card shadow="never" v-loading="loading">
      <div slot="header">
        <span>证书编号配置</span>
      </div>
      <el-form ref="form" :model="form" :rules="rules" label-width="160px" style="max-width: 640px">
        <el-form-item label="证书编号前缀字母" prop="certNoPrefix">
          <el-input v-model="form.certNoPrefix" placeholder="如 ZGZH" maxlength="10" show-word-limit />
        </el-form-item>
        <el-form-item label="证书编号中段字母" prop="certNoMiddle">
          <el-input v-model="form.certNoMiddle" placeholder="如 M" maxlength="10" show-word-limit />
        </el-form-item>
        <el-form-item label="学员编号前缀字母" prop="studentNoPrefix">
          <el-input v-model="form.studentNoPrefix" placeholder="如 RCCP" maxlength="10" show-word-limit />
        </el-form-item>
        <el-form-item label="学员编号中段字母" prop="studentNoMiddle">
          <el-input v-model="form.studentNoMiddle" placeholder="如 B" maxlength="10" show-word-limit />
        </el-form-item>

        <el-form-item label="编号预览">
          <div class="preview-box">
            <div class="preview-row">
              <span class="preview-label">证书编号：</span>
              <span class="preview-value">{{ certNoPreview }}</span>
            </div>
            <div class="preview-row">
              <span class="preview-label">学员编号：</span>
              <span class="preview-value">{{ studentNoPreview }}</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitForm">保 存</el-button>
          <el-button @click="fetchDetail">重 置</el-button>
        </el-form-item>
      </el-form>

      <div class="rule-tip">
        <p><strong>编号生成规则：</strong>前缀字母 + 出证日期(yyyyMMdd) + 中段字母 + 10000~99999随机数（系统内唯一）</p>
        <p class="example">示例：{{ form.certNoPrefix || 'ZGZH' }}20201020{{ form.certNoMiddle || 'M' }}12345</p>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getNumberConfig, updateNumberConfig } from '@/api/certificate'

export default {
  name: 'CertificateNumberConfig',
  data() {
    return {
      loading: false,
      submitting: false,
      form: {
        certNoPrefix: '',
        certNoMiddle: '',
        studentNoPrefix: '',
        studentNoMiddle: ''
      },
      rules: {}
    }
  },
  computed: {
    certNoPreview() {
      const p = this.form.certNoPrefix || ''
      const m = this.form.certNoMiddle || ''
      return p + '20201020' + m + '12345'
    },
    studentNoPreview() {
      const p = this.form.studentNoPrefix || ''
      const m = this.form.studentNoMiddle || ''
      return p + '20201020' + m + '67890'
    }
  },
  created() {
    this.fetchDetail()
  },
  methods: {
    fetchDetail() {
      this.loading = true
      getNumberConfig()
        .then((res) => {
          const data = res.data || {}
          this.form = {
            certNoPrefix: data.certNoPrefix || '',
            certNoMiddle: data.certNoMiddle || '',
            studentNoPrefix: data.studentNoPrefix || '',
            studentNoMiddle: data.studentNoMiddle || ''
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    submitForm() {
      this.submitting = true
      updateNumberConfig(this.form)
        .then(() => {
          this.$message.success('保存成功')
        })
        .finally(() => {
          this.submitting = false
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.preview-box {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 16px 20px;

  .preview-row {
    margin-bottom: 8px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .preview-label {
    color: #909399;
    font-size: 14px;
  }

  .preview-value {
    color: #303133;
    font-size: 15px;
    font-weight: 500;
    letter-spacing: 1px;
  }
}

.rule-tip {
  margin-top: 8px;
  padding: 16px 20px;
  background: #ecf5ff;
  border-radius: 6px;
  color: #606266;
  font-size: 13px;
  line-height: 1.8;

  .example {
    color: #409eff;
    font-weight: 500;
    letter-spacing: 1px;
  }
}
</style>
