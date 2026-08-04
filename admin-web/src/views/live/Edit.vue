<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="isEdit ? '编辑直播' : '新增直播'" class="page-header" />

    <el-card shadow="never" v-loading="loading">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" style="max-width: 720px">
        <el-form-item label="直播标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入直播标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="主播名称" prop="anchorName">
          <el-input v-model="form.anchorName" placeholder="请输入主播/讲师名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="封面图" prop="coverUrl">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :data="{ type: 'image' }"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeImageUpload"
            accept="image/*"
          >
            <img v-if="form.coverUrl" :src="apiUrl(form.coverUrl)" class="cover-preview" />
            <div v-else class="cover-uploader">
              <i class="el-icon-plus"></i>
            </div>
          </el-upload>
          <div v-if="form.coverUrl" class="cover-actions">
            <el-button type="text" icon="el-icon-refresh-left" @click="form.coverUrl = ''">
              重新上传
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择直播开始时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择直播结束时间(可选)"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :step="1" controls-position="right" />
          <span class="form-tip">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="直播简介" prop="intro">
          <el-input
            v-model="form.intro"
            type="textarea"
            :rows="4"
            placeholder="请输入直播简介/内容介绍"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitForm">保 存</el-button>
          <el-button @click="goBack">取 消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { liveDetail, addLive, updateLive } from '@/api/live'
import store from '@/store'
import { apiUrl } from '@/utils/apiBase'

export default {
  name: 'LiveEdit',
  data() {
    return {
      loading: false,
      submitting: false,
      isEdit: false,
      form: {
        id: undefined,
        title: '',
        anchorName: '',
        coverUrl: '',
        intro: '',
        startTime: '',
        endTime: '',
        sort: 0
      },
      rules: {
        title: [{ required: true, message: '请输入直播标题', trigger: 'blur' }]
      },
      uploadAction: '/api/file/upload',
      uploadHeaders: {
        'Admin-Token': store.getters.token,
        Authorization: 'Bearer ' + store.getters.token
      }
    }
  },
  created() {
    const id = this.$route.params.id
    if (id) {
      this.isEdit = true
      this.fetchDetail(id)
    }
  },
  methods: {
    apiUrl,
    fetchDetail(id) {
      this.loading = true
      liveDetail(id)
        .then((res) => {
          const data = res.data || {}
          this.form = {
            id: data.id,
            title: data.title || '',
            anchorName: data.anchorName || '',
            coverUrl: data.coverUrl || '',
            intro: data.intro || '',
            startTime: data.startTime || '',
            endTime: data.endTime || '',
            sort: data.sort || 0
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
    handleCoverSuccess(response, file) {
      let url = ''
      if (response && typeof response === 'object') {
        const inner = response.data !== undefined ? response.data : response
        if (inner && typeof inner === 'object') {
          url = inner.url || inner.path || inner.link || ''
        } else if (typeof inner === 'string') {
          url = inner
        }
        if (!url && response.code !== 200 && response.code !== 0) {
          this.$message.error(response.message || '封面上传失败')
          return
        }
      } else if (typeof response === 'string') {
        url = response
      }
      if (!url) {
        url = (file && file.url) || ''
      }
      if (!url) {
        this.$message.error('封面上传失败，未获取到文件地址')
        return
      }
      this.form.coverUrl = url
      this.$message.success('封面上传成功')
    },
    handleUploadError() {
      this.$message.error('上传失败，请重试')
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return
        this.submitting = true
        const payload = { ...this.form }
        const action = this.isEdit ? updateLive(payload) : addLive(payload)
        action
          .then(() => {
            this.$message.success(this.isEdit ? '更新成功' : '新增成功')
            this.$router.push('/live/list').catch(() => {})
          })
          .catch(() => {
            this.$message.error('保存失败')
          })
          .finally(() => {
            this.submitting = false
          })
      })
    },
    goBack() {
      this.$router.push('/live/list').catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.page-header {
  margin-bottom: 16px;
}

.form-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}

.cover-uploader {
  width: 180px;
  height: 120px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8c939d;
  font-size: 28px;

  &:hover {
    border-color: #409eff;
  }
}

.cover-preview {
  width: 180px;
  height: 120px;
  object-fit: cover;
  border-radius: 6px;
  display: block;
}

.cover-actions {
  margin-top: 8px;
}
</style>
