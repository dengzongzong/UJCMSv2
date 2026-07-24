<template>
  <div class="app-container">
    <el-card shadow="never" v-loading="loading">
      <div slot="header">
        <span>关于我们设置</span>
      </div>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px" style="max-width: 760px">
        <el-form-item label="客服电话" prop="servicePhone">
          <el-input v-model="form.servicePhone" placeholder="请输入客服电话" maxlength="30" />
        </el-form-item>
        <el-form-item label="客服二维码" prop="serviceQrcode">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :data="{ type: 'image' }"
            :show-file-list="false"
            :on-success="handleQrcodeSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeImageUpload"
            accept="image/*"
          >
            <div v-if="form.serviceQrcode" class="qrcode-preview-wrap">
              <img :src="form.serviceQrcode" class="qrcode-preview" />
              <div class="qrcode-overlay">
                <i class="el-icon-refresh"></i>
                <span>重新上传</span>
              </div>
            </div>
            <div v-else class="qrcode-uploader">
              <i class="el-icon-plus"></i>
              <span class="upload-text">点击上传</span>
            </div>
            <div slot="tip" class="el-upload__tip">建议上传正方形二维码图片，大小不超过 5MB。用户端将优先展示此图片。</div>
          </el-upload>
          <el-button v-if="form.serviceQrcode" type="text" size="small" @click="form.serviceQrcode = ''" style="margin-left: 12px">
            <i class="el-icon-delete"></i> 清除图片
          </el-button>
        </el-form-item>
        <el-form-item label="关于我们二维码链接" prop="qrcodeLink">
          <el-input v-model="form.qrcodeLink" placeholder="请输入二维码指向的链接,如 https://example.com 或官方公众号链接" maxlength="500" show-word-limit clearable />
          <div class="form-tip">用户端所有页面右下角都会显示二维码。优先展示上方上传的二维码图片；未上传图片时根据此链接生成二维码。留空且未上传图片则不显示。</div>
        </el-form-item>
        <el-form-item label="平台介绍" prop="content">
          <RichEditor v-model="form.content" :height="380" placeholder="请输入平台介绍内容(支持富文本: 文字+图片+链接+表格+视频)" />
          <div class="form-tip">支持富文本: 加粗/字号/颜色/链接/图片/表格/视频等;最多 5000 字</div>
        </el-form-item>
        <el-form-item label="证书说明" prop="disclaimer">
          <RichEditor v-model="form.disclaimer" :height="260" placeholder="请输入证书说明内容(支持富文本)" />
          <div class="form-tip">用户端"关于我们"页面底部展示;留空则不显示证书说明</div>
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
import { getAbout, updateAbout } from '@/api/setting'
import RichEditor from '@/components/RichEditor'
import store from '@/store'

export default {
  name: 'SettingAbout',
  components: { RichEditor },
  data() {
    return {
      loading: false,
      submitting: false,
      form: {
        servicePhone: '',
        serviceQrcode: '',
        qrcodeLink: '',
        content: '',
        disclaimer: ''
      },
      rules: {
        servicePhone: [{ required: true, message: '请输入客服电话', trigger: 'blur' }]
      },
      uploadAction: '/api/file/upload',
      uploadHeaders: {
        'Admin-Token': store.getters.token,
        Authorization: 'Bearer ' + store.getters.token
      }
    }
  },
  created() {
    this.fetchDetail()
  },
  methods: {
    fetchDetail() {
      this.loading = true
      getAbout()
        .then((res) => {
          const data = res.data || {}
          this.form = {
            servicePhone: data.servicePhone || '',
            serviceQrcode: data.serviceQrcode || '',
            qrcodeLink: data.qrcodeLink || '',
            content: data.content || '',
            disclaimer: data.disclaimer || ''
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    beforeImageUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isImage) {
        this.$message.error('只能上传图片文件')
        return false
      }
      if (!isLt5M) {
        this.$message.error('图片大小不能超过 5MB')
        return false
      }
      return true
    },
    handleQrcodeSuccess(response, file) {
      const res = response.data || response
      this.form.serviceQrcode = (res && (res.url || res.path || res)) || file.url
    },
    handleUploadError() {
      this.$message.error('上传失败，请重试')
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return
        this.submitting = true
        updateAbout(this.form)
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

<style lang="scss" scoped>
.qrcode-uploader {
  width: 120px;
  height: 120px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8c939d;
  font-size: 28px;
  gap: 4px;

  .upload-text {
    font-size: 12px;
  }

  &:hover {
    border-color: #409eff;
    color: #409eff;
  }
}

.qrcode-preview-wrap {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;

  &:hover .qrcode-overlay {
    opacity: 1;
  }
}

.qrcode-preview {
  width: 120px;
  height: 120px;
  object-fit: cover;
  display: block;
}

.qrcode-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;

  i {
    font-size: 22px;
  }
}
</style>
