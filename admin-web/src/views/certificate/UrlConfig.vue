<template>
  <div class="app-container">
    <el-card shadow="never" v-loading="loading">
      <div slot="header">
        <span>证书二维码 URL 配置</span>
      </div>

      <div class="intro">
        配置证书二维码 1/2/3 的跳转链接生成规则。规则中可使用<strong>常量</strong>与<strong>证书用户属性占位符</strong>
        (用 <code>{属性名}</code> 表示)拼接,渲染证书生成二维码时,占位符会被证书实际属性值
        (URL 编码)替换,最终生成符合 URL 查询参数格式的链接。二维码绘制到证书上的位置,由
        <strong>证书模板</strong>界面编辑的字段配置(fieldKey=qr1/qr2/qr3)决定。
      </div>

      <el-form ref="form" :model="form" label-width="120px" style="max-width: 820px">
        <el-form-item label="二维码1 规则">
          <el-input
            v-model="form.qr1Template"
            type="textarea"
            :rows="2"
            placeholder="留空则回退使用证书本身的 qr_url1。示例:https://example.com/query?idCard={idCard}&certNo={certNo}"
          />
        </el-form-item>
        <el-form-item label="二维码2 规则">
          <el-input
            v-model="form.qr2Template"
            type="textarea"
            :rows="2"
            placeholder="留空则回退使用证书本身的 qr_url2"
          />
        </el-form-item>
        <el-form-item label="二维码3 规则">
          <el-input
            v-model="form.qr3Template"
            type="textarea"
            :rows="2"
            placeholder="留空则回退使用证书本身的 qr_url3"
          />
        </el-form-item>

        <el-form-item label="可用占位符">
          <div class="placeholder-list">
            <el-tag
              v-for="p in placeholders"
              :key="p.key"
              size="small"
              class="placeholder-tag"
              @click="insertPlaceholder(p.key)"
            >
              { {{ p.key }} }
              <span class="placeholder-desc">{{ p.desc }}</span>
            </el-tag>
          </div>
          <div class="placeholder-tip">点击占位符可复制到剪贴板,再粘贴到上方规则中。</div>
        </el-form-item>

        <el-form-item label="生成预览">
          <div class="preview-box">
            <div class="preview-row">
              <span class="preview-label">二维码1：</span>
              <span class="preview-value">{{ previewUrl(form.qr1Template) }}</span>
            </div>
            <div class="preview-row">
              <span class="preview-label">二维码2：</span>
              <span class="preview-value">{{ previewUrl(form.qr2Template) }}</span>
            </div>
            <div class="preview-row">
              <span class="preview-label">二维码3：</span>
              <span class="preview-value">{{ previewUrl(form.qr3Template) }}</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitForm">保 存</el-button>
          <el-button @click="fetchDetail">重 置</el-button>
        </el-form-item>
      </el-form>

      <div class="rule-tip">
        <p><strong>说明：</strong></p>
        <p>1. 占位符格式为 <code>{属性名}</code>,不区分大小写,如 <code>{idCard}</code>、<code>{certNo}</code>。</p>
        <p>2. 渲染时占位符会被证书实际属性值(URL 编码)替换,生成符合 URL 查询参数格式的链接,扫码即可跳转。</p>
        <p>3. 规则为空时,回退使用证书导入时填写的 qr_url1/2/3。</p>
        <p>4. 二维码的位置、大小由"证书模板"界面中 fieldKey=qr1/qr2/qr3 的字段配置决定。</p>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getUrlConfig, updateUrlConfig } from '@/api/certificate'

export default {
  name: 'CertificateUrlConfig',
  data() {
    return {
      loading: false,
      submitting: false,
      form: {
        qr1Template: '',
        qr2Template: '',
        qr3Template: ''
      },
      placeholders: [
        { key: 'certNo', desc: '证书编号' },
        { key: 'studentNo', desc: '学员编号' },
        { key: 'name', desc: '姓名' },
        { key: 'idCard', desc: '身份证号' },
        { key: 'gender', desc: '性别' },
        { key: 'profession', desc: '职业名称' },
        { key: 'skillLevel', desc: '技能等级' },
        { key: 'issueDate', desc: '颁发日期' },
        { key: 'agency', desc: '报单机构' },
        { key: 'phone', desc: '手机号' },
        { key: 'theoryScore', desc: '理论成绩' },
        { key: 'practicalScore', desc: '实操成绩' },
        { key: 'comprehensiveEvaluation', desc: '综合测评' }
      ],
      sample: {
        certNo: 'ZGZH20260703M12345',
        studentNo: 'RCCP20260703B67890',
        name: '张三',
        idCard: '130684199001011234',
        gender: '男',
        profession: '电工',
        skillLevel: '三级/高级',
        issueDate: '2026-07-03',
        agency: '某某培训机构',
        phone: '13800138000',
        theoryScore: '88',
        practicalScore: '合格',
        comprehensiveEvaluation: '88'
      }
    }
  },
  created() {
    this.fetchDetail()
  },
  methods: {
    fetchDetail() {
      this.loading = true
      getUrlConfig()
        .then((res) => {
          const data = res.data || {}
          this.form = {
            qr1Template: data.qr1Template || '',
            qr2Template: data.qr2Template || '',
            qr3Template: data.qr3Template || ''
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    submitForm() {
      this.submitting = true
      updateUrlConfig(this.form)
        .then(() => {
          this.$message.success('保存成功')
        })
        .finally(() => {
          this.submitting = false
        })
    },
    insertPlaceholder(key) {
      const text = '{' + key + '}'
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success('已复制: ' + text)
        }).catch(() => {
          this.$message.info('占位符: ' + text)
        })
      } else {
        this.$message.info('占位符: ' + text)
      }
    },
    previewUrl(template) {
      if (!template) return '(空,回退使用证书 qr_url)'
      let result = template
      Object.keys(this.sample).forEach((k) => {
        const val = this.sample[k]
        const encoded = encodeURIComponent(val)
        result = result.replace(new RegExp('\\{' + k + '\\}', 'gi'), encoded)
      })
      return result
    }
  }
}
</script>

<style lang="scss" scoped>
.intro {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 20px;
  color: #595959;
  font-size: 13px;
  line-height: 1.8;

  code {
    background: #f5f5f5;
    padding: 1px 6px;
    border-radius: 3px;
    color: #c41d7f;
  }
}

.placeholder-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.placeholder-tag {
  cursor: pointer;

  .placeholder-desc {
    color: #909399;
    margin-left: 4px;
    font-size: 12px;
  }
}

.placeholder-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.preview-box {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 16px 20px;

  .preview-row {
    margin-bottom: 10px;
    word-break: break-all;

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
    font-size: 14px;
    font-weight: 500;
  }
}

.rule-tip {
  margin-top: 8px;
  padding: 16px 20px;
  background: #ecf5ff;
  border-radius: 6px;
  color: #606266;
  font-size: 13px;
  line-height: 1.9;

  code {
    background: #f5f5f5;
    padding: 1px 6px;
    border-radius: 3px;
    color: #c41d7f;
  }
}
</style>
