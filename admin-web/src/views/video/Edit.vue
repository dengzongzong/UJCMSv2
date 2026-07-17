<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="isEdit ? '编辑视频' : '新增视频'" class="page-header" />

    <el-card shadow="never" v-loading="loading">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" style="max-width: 720px">
        <el-form-item label="视频名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入视频名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="所属课程" prop="courseId">
          <el-select v-model="form.courseId" placeholder="请选择所属课程" clearable style="width: 100%">
            <el-option
              v-for="item in courses"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属专业" prop="professionId">
          <el-select v-model="form.professionId" placeholder="请选择专业" clearable filterable style="width: 100%">
            <el-option
              v-for="item in professionOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="视频文件" prop="url">
          <el-upload
            ref="videoUpload"
            :action="uploadAction"
            :headers="uploadHeaders"
            :data="{ type: 'video' }"
            :show-file-list="true"
            :file-list="videoFileList"
            :limit="1"
            :on-success="handleVideoSuccess"
            :on-error="handleVideoError"
            :on-progress="handleVideoProgress"
            :on-remove="handleVideoRemove"
            :on-exceed="handleExceed"
            :before-upload="beforeVideoUpload"
            accept="video/*"
          >
            <el-button size="small" type="primary" icon="el-icon-upload2" :loading="videoUploading">
              {{ videoUploading ? `上传中 ${videoProgress}%` : '点击上传视频' }}
            </el-button>
            <div slot="tip" class="el-upload__tip">
              支持 mp4/avi/mov 等格式，单个文件不超过 2GB
            </div>
          </el-upload>
          <video
            v-if="form.url && !videoUploading"
            :src="apiUrl(form.url)"
            controls
            style="width: 100%; max-height: 280px; margin-top: 12px; background: #000"
          />
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
        <el-form-item label="时长(秒)" prop="duration">
          <el-input-number v-model="form.duration" :min="0" :step="1" controls-position="right" />
          <span class="form-tip">{{ formatDuration(form.duration) }}</span>
        </el-form-item>
        <el-form-item label="大小(MB)" prop="size">
          <el-input-number v-model="form.size" :min="0" :step="0.01" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="基础学习人数" prop="baseStudyCount">
          <el-input-number v-model="form.baseStudyCount" :min="0" :step="1" controls-position="right" />
          <span class="form-tip">展示人数 = 基础人数 + 实际播放量</span>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入备注信息"
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
import { videoDetail, addVideo, updateVideo } from '@/api/video'
import { professions } from '@/api/setting'
import { coursePage } from '@/api/course'
import { formatDuration } from '@/utils'
import store from '@/store'
import { apiUrl } from '@/utils/apiBase'

export default {
  name: 'VideoEdit',
  data() {
    return {
      loading: false,
      submitting: false,
      isEdit: false,
      courses: [],
      professionOptions: [],
      form: {
        id: undefined,
        name: '',
        courseId: undefined,
        professionId: undefined,
        url: '',
        coverUrl: '',
        duration: 0,
        size: 0,
        baseStudyCount: 0,
        remark: ''
      },
      rules: {
        name: [{ required: true, message: '请输入视频名称', trigger: 'blur' }],
        url: [{ required: true, message: '请上传视频文件', trigger: 'change' }]
      },
      videoFileList: [],
      videoUploading: false,
      videoProgress: 0,
      uploadAction: '/api/file/upload',
      uploadHeaders: {
        'Admin-Token': store.getters.token,
        Authorization: 'Bearer ' + store.getters.token
      }
    }
  },
  created() {
    this.fetchCourses()
    this.fetchProfessions()
    const id = this.$route.params.id
    if (id) {
      this.isEdit = true
      this.fetchDetail(id)
    }
  },
  methods: {
    apiUrl,
    formatDuration,
    fetchCourses() {
      coursePage({ page: 1, size: 1000 })
        .then((res) => {
          const data = res.data || {}
          this.courses = data.records || data.list || data.rows || []
        })
        .catch(() => {
          this.courses = []
        })
    },
    fetchProfessions() {
      professions()
        .then((res) => {
          this.professionOptions = res.data || []
        })
        .catch(() => {
          this.professionOptions = []
        })
    },
    fetchDetail(id) {
      this.loading = true
      videoDetail(id)
        .then((res) => {
          const data = res.data || {}
          this.form = {
            id: data.id,
            name: data.name || '',
            courseId: data.courseId,
            professionId: data.professionId,
            url: data.url || '',
            coverUrl: data.coverUrl || '',
            duration: data.duration || 0,
            size: data.size || 0,
            baseStudyCount: data.baseStudyCount || 0,
            remark: data.remark || ''
          }
          if (data.url) {
            this.videoFileList = [
              { name: data.name || 'video.mp4', url: data.url }
            ]
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    beforeVideoUpload(file) {
      const isVideo = file.type.startsWith('video/')
      const isLt2G = file.size / 1024 / 1024 / 1024 < 2
      if (!isVideo) {
        this.$message.error('只能上传视频文件')
        return false
      }
      if (!isLt2G) {
        this.$message.error('视频大小不能超过 2GB')
        return false
      }
      this.videoUploading = true
      this.videoProgress = 0
      return true
    },
    handleVideoProgress(event) {
      this.videoProgress = Math.floor(event.percent) || 0
    },
    handleVideoSuccess(response, file) {
      this.videoUploading = false
      this.videoProgress = 100
      // 兼容多种后端返回结构：{code:200,data:'url'} / {code:200,data:{url}} / 'url'
      let url = ''
      if (response && typeof response === 'object') {
        const inner = response.data !== undefined ? response.data : response
        if (inner && typeof inner === 'object') {
          url = inner.url || inner.path || inner.link || ''
        } else if (typeof inner === 'string') {
          url = inner
        }
        if (!url && response.code !== 200 && response.code !== 0) {
          this.$message.error(response.message || '视频上传失败')
          return
        }
      } else if (typeof response === 'string') {
        url = response
      }
      if (!url) {
        url = (file && file.url) || ''
      }
      if (!url) {
        this.$message.error('视频上传失败，未获取到文件地址')
        return
      }
      this.form.url = url
      if (!this.form.name) this.form.name = file.name.replace(/\.[^.]+$/, '')
      if (file.size) {
        this.form.size = Number((file.size / 1024 / 1024).toFixed(2))
      }
      this.$message.success('视频上传成功')
      this.$refs.form.validateField('url')
    },
    handleVideoError() {
      this.videoUploading = false
      this.videoProgress = 0
      this.$message.error('视频上传失败，请重试')
    },
    handleVideoRemove() {
      this.form.url = ''
      this.videoFileList = []
      this.videoUploading = false
      this.videoProgress = 0
    },
    handleExceed() {
      this.$message.warning('只能上传一个视频，请先删除已上传的视频')
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
        const action = this.isEdit ? updateVideo(payload) : addVideo(payload)
        action
          .then(() => {
            this.$message.success(this.isEdit ? '更新成功' : '新增成功')
            this.$router.push('/video/list').catch(() => {})
          })
          .finally(() => {
            this.submitting = false
          })
      })
    },
    goBack() {
      this.$router.push('/video/list').catch(() => {})
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
