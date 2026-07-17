<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="isEdit ? '编辑考试' : '新增考试'" class="page-header" />

    <el-card shadow="never" v-loading="loading">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="考试名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入考试名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总时长(分)" prop="duration">
              <el-input-number v-model="form.duration" :min="30" :max="180" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="考试分类">
              <el-input v-model="form.category" placeholder="请输入考试分类" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">考试设置</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="考试开始时间">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                placeholder="选择开始时间(留空=不限)"
                value-format="yyyy-MM-dd HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试结束时间">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                placeholder="选择结束时间(留空=不限)"
                value-format="yyyy-MM-dd HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最大考试次数">
              <el-input-number v-model="form.maxAttempts" :min="0" :max="99" controls-position="right" style="width: 100%" placeholder="0=不限" />
              <div style="font-size:12px;color:#909399;line-height:1.5;margin-top:4px">0表示不限次数，学生可在时间段内多次考试</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="允许重新作答">
              <el-radio-group v-model="form.allowRetry">
                <el-radio :label="1">允许</el-radio>
                <el-radio :label="0">不允许</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属专业" prop="professionId">
              <el-select
                v-model="form.professionId"
                placeholder="请选择专业"
                filterable
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="item in professionOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">已发布</el-radio>
                <el-radio :label="0">未发布</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="基础考过人数" prop="baseExamCount">
              <el-input-number v-model="form.baseExamCount" :min="0" controls-position="right" style="width: 100%" />
              <div style="font-size:12px;color:#909399;line-height:1.5;margin-top:4px">用户端显示"已有X人考过"=此基数+实际开通权限人数</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面图" prop="cover">
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
            <img v-if="form.cover" :src="apiUrl(form.cover)" class="cover-preview" />
            <div v-else class="cover-uploader">
              <i class="el-icon-plus"></i>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="考试简介" prop="intro">
          <el-input
            v-model="form.intro"
            type="textarea"
            :rows="3"
            placeholder="请输入考试简介"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-divider content-position="left">试卷选择</el-divider>
        <el-form-item label="选择试卷" prop="paperId">
          <el-select
            v-model="form.paperId"
            placeholder="请选择已发布的试卷"
            filterable
            clearable
            style="width: 100%"
            @change="handlePaperChange"
          >
            <el-option
              v-for="item in paperOptions"
              :key="item.id"
              :label="`${item.name}（${item.questionCount || 0}题 / ${item.totalScore || 0}分）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="试卷信息">
          <el-descriptions v-if="selectedPaper" :column="3" border size="small">
            <el-descriptions-item label="试卷名称">{{ selectedPaper.name }}</el-descriptions-item>
            <el-descriptions-item label="题目数量">{{ selectedPaper.questionCount || 0 }} 题</el-descriptions-item>
            <el-descriptions-item label="总分">{{ selectedPaper.totalScore || 0 }} 分</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="请先选择试卷" :image-size="60" />
        </el-form-item>

        <el-form-item style="margin-top: 24px">
          <el-button type="primary" :loading="submitting" @click="submitForm">保 存</el-button>
          <el-button @click="goBack">取 消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { examDetail, addExam, updateExam } from '@/api/exam'
import { paperList } from '@/api/paper'
import { professions } from '@/api/setting'
import store from '@/store'
import { apiUrl } from '@/utils/apiBase'

export default {
  name: 'ExamEdit',
  data() {
    return {
      loading: false,
      submitting: false,
      isEdit: false,
      paperOptions: [],
      professionOptions: [],
      form: {
        id: undefined,
        name: '',
        duration: 90,
        status: 1,
        cover: '',
        intro: '',
        paperId: undefined,
        professionId: undefined,
        startTime: undefined,
        endTime: undefined,
        allowRetry: undefined,
        maxAttempts: 0,
        category: '',
        baseExamCount: 0
      },
      rules: {
        name: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
        duration: [
          { required: true, message: '请输入总时长', trigger: 'blur' },
          { type: 'number', min: 30, max: 180, message: '考试时长应在30到180分钟之间', trigger: 'blur' }
        ],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
        paperId: [{ required: true, message: '请选择试卷', trigger: 'change' }]
      },
      uploadAction: '/api/file/upload',
      uploadHeaders: {
        'Admin-Token': store.getters.token,
        Authorization: 'Bearer ' + store.getters.token
      }
    }
  },
  computed: {
    selectedPaper() {
      if (!this.form.paperId) return null
      return this.paperOptions.find((p) => p.id === this.form.paperId) || null
    },
    totalScore() {
      return this.selectedPaper ? (this.selectedPaper.totalScore || 0) : 0
    }
  },
  created() {
    this.fetchProfessions()
    this.fetchPapers()
    const id = this.$route.params.id
    if (id) {
      this.isEdit = true
      this.fetchDetail(id)
    }
  },
  methods: {
    apiUrl,
    fetchProfessions() {
      professions()
        .then((res) => {
          const data = res.data || []
          this.professionOptions = Array.isArray(data) ? data : (data.list || data.records || [])
        })
        .catch(() => {
          this.professionOptions = []
        })
    },
    fetchPapers() {
      paperList()
        .then((res) => {
          const data = res.data || []
          this.paperOptions = Array.isArray(data) ? data : (data.list || data.records || [])
        })
        .catch(() => {
          this.paperOptions = []
        })
    },
    handlePaperChange() {
      // 试卷变更时触发校验
      this.$refs.form && this.$refs.form.validateField('paperId')
    },
    fetchDetail(id) {
      this.loading = true
      examDetail(id)
        .then((res) => {
          const data = res.data || {}
          this.form.id = data.id
          this.form.name = data.name || ''
          this.form.cover = data.cover || data.coverUrl || ''
          this.form.intro = data.intro || ''
          this.form.duration = data.duration
          this.form.startTime = data.startTime
          this.form.endTime = data.endTime
          this.form.allowRetry = data.allowRetry
          this.form.maxAttempts = data.maxAttempts
          this.form.status = data.status
          this.form.professionId = data.professionId
          this.form.paperId = data.paperId
          this.form.category = data.category || ''
          this.form.baseExamCount = data.baseExamCount || 0
        })
        .catch(() => {
          this.$message.error('获取详情失败')
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
      } else if (typeof response === 'string') {
        url = response
      }
      if (!url) {
        url = (file && file.url) || ''
      }
      this.form.cover = url
    },
    handleUploadError() {
      this.$message.error('上传失败，请重试')
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return
        if (!this.form.paperId) {
          this.$message.warning('请选择试卷')
          return
        }
        this.submitting = true
        const payload = { ...this.form }
        const action = this.isEdit ? updateExam(payload) : addExam(payload)
        action
          .then(() => {
            this.$message.success(this.isEdit ? '更新成功' : '新增成功')
            this.$router.push('/exam/list').catch(() => {})
          })
          .finally(() => {
            this.submitting = false
          })
      })
    },
    goBack() {
      this.$router.push('/exam/list').catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.page-header {
  margin-bottom: 16px;
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
</style>
