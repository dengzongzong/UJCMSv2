<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="isEdit ? '编辑课程' : '新增课程'" class="page-header" />

    <el-card shadow="never" v-loading="loading">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="课程名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入课程名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="标签" prop="tag">
              <el-input v-model="form.tag" placeholder="多个标签用逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">上架</el-radio>
                <el-radio :label="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
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
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择课程分类(用户端按此分组)" clearable filterable style="width: 100%">
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="基础学过人数" prop="baseStudyCount">
              <el-input-number v-model="form.baseStudyCount" :min="0" controls-position="right" style="width: 100%" />
              <div style="font-size:12px;color:#909399;line-height:1.5;margin-top:4px">用户端显示"已有X人学过"=此基数+实际开通人数</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="基础学时" prop="baseStudyHours">
              <el-input-number v-model="form.baseStudyHours" :min="0" controls-position="right" style="width: 100%" />
              <div style="font-size:12px;color:#909399;line-height:1.5;margin-top:4px">用户端显示的学时(小时),无单位</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="是否置顶" prop="isTop">
              <el-radio-group v-model="form.isTop">
                <el-radio :label="1">置顶</el-radio>
                <el-radio :label="0">不置顶</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="置顶排序" prop="topSort">
              <el-input-number v-model="form.topSort" :min="0" controls-position="right" style="width: 100%" />
              <div style="font-size:12px;color:#909399;line-height:1.5;margin-top:4px">置顶课程中越小越靠前</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="缩略图" prop="thumb">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :data="{ type: 'image' }"
            :show-file-list="false"
            :on-success="handleThumbSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeImageUpload"
            accept="image/*"
          >
            <img v-if="form.coverUrl" :src="apiUrl(form.coverUrl)" class="cover-preview" />
            <div v-else class="cover-uploader">
              <i class="el-icon-plus"></i>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="课程简介" prop="intro">
          <el-input
            v-model="form.intro"
            type="textarea"
            :rows="4"
            placeholder="请输入课程简介"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-divider content-position="left">
          <span>小节管理</span>
          <el-button
            type="primary"
            size="mini"
            icon="el-icon-plus"
            style="margin-left: 12px"
            @click="addSection"
          >
            添加小节
          </el-button>
        </el-divider>

        <el-table :data="form.sections" border style="width: 100%">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="小节标题" min-width="200">
            <template slot-scope="{ row }">
              <el-input v-model="row.title" placeholder="请输入小节标题" />
            </template>
          </el-table-column>
          <el-table-column label="关联视频" min-width="240">
            <template slot-scope="{ row }">
              <el-select
                v-model="row.videoId"
                placeholder="请选择视频"
                filterable
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="v in videoOptions"
                  :key="v.id"
                  :label="v.name"
                  :value="v.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template slot-scope="{ $index }">
              <el-button
                type="text"
                icon="el-icon-arrow-up"
                :disabled="$index === 0"
                @click="moveSection($index, -1)"
              >
                上移
              </el-button>
              <el-button
                type="text"
                icon="el-icon-arrow-down"
                :disabled="$index === form.sections.length - 1"
                @click="moveSection($index, 1)"
              >
                下移
              </el-button>
              <el-button
                type="text"
                icon="el-icon-delete"
                class="danger-text"
                @click="removeSection($index)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="form.sections.length === 0" description="暂无小节，请点击添加小节" />

        <el-form-item style="margin-top: 24px">
          <el-button type="primary" :loading="submitting" @click="submitForm">保 存</el-button>
          <el-button @click="goBack">取 消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { courseDetail, addCourse, updateCourse } from '@/api/course'
import { videoPage } from '@/api/video'
import { professions, videoCategories } from '@/api/setting'
import store from '@/store'
import { apiUrl } from '@/utils/apiBase'

export default {
  name: 'CourseEdit',
  data() {
    return {
      loading: false,
      submitting: false,
      isEdit: false,
      videoOptions: [],
      professionOptions: [],
      categoryOptions: [],
      form: {
        id: undefined,
        name: '',
        price: 0,
        tag: '',
        status: 1,
        coverUrl: '',
        intro: '',
        professionId: undefined,
        categoryId: undefined,
        baseStudyCount: 0,
        baseStudyHours: 0,
        isTop: 0,
        topSort: 0,
        sections: []
      },
      rules: {
        name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
        price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
      },
      uploadAction: '/api/file/upload',
      uploadHeaders: {
        'Admin-Token': store.getters.token,
        Authorization: 'Bearer ' + store.getters.token
      }
    }
  },
  created() {
    this.fetchVideoOptions()
    this.fetchProfessions()
    this.fetchVideoCategories()
    const id = this.$route.params.id
    if (id) {
      this.isEdit = true
      this.fetchDetail(id)
    }
  },
  methods: {
    apiUrl,
    fetchVideoOptions() {
      videoPage({ page: 1, size: 1000 })
        .then((res) => {
          const data = res.data || {}
          this.videoOptions = data.records || data.list || data.rows || []
        })
        .catch(() => {
          this.videoOptions = []
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
    fetchVideoCategories() {
      videoCategories()
        .then((res) => {
          this.categoryOptions = res.data || []
        })
        .catch(() => {
          this.categoryOptions = []
        })
    },
    fetchDetail(id) {
      this.loading = true
      courseDetail(id)
        .then((res) => {
          // 后端 detail 返回结构:{ course: {...}, sections: [{ id, name, videos: [{ videoId, ... }] }] }
          // 前端 form 直接用扁平字段,字段名 coverUrl/title 与后端对齐(经 submitForm 再转 name/videoIds)
          const data = res.data || {}
          const c = data.course || data
          const list = data.sections || []
          this.form = {
            id: c.id,
            name: c.name || '',
            price: c.price || 0,
            tag: c.tag || '',
            status: c.status === undefined ? 1 : c.status,
            coverUrl: c.coverUrl || '',
            intro: c.intro || '',
            professionId: c.professionId,
            categoryId: c.categoryId,
            baseStudyCount: c.baseStudyCount || 0,
            baseStudyHours: c.baseStudyHours || 0,
            isTop: c.isTop === undefined ? 0 : c.isTop,
            topSort: c.topSort || 0,
            sections: list.map((s) => ({
              id: s.id,
              title: s.name || '',
              // 小节有 videos 数组,前端只展示一个 videoId(取第一个)
              videoId: (s.videos && s.videos.length > 0 ? s.videos[0].videoId : undefined)
            }))
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    addSection() {
      this.form.sections.push({ title: '', videoId: undefined })
    },
    removeSection(index) {
      this.form.sections.splice(index, 1)
    },
    moveSection(index, direction) {
      const target = index + direction
      if (target < 0 || target >= this.form.sections.length) return
      const arr = this.form.sections
      const tmp = arr[index]
      this.$set(arr, index, arr[target])
      this.$set(arr, target, tmp)
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
    handleThumbSuccess(response, file) {
      const res = response.data || response
      this.form.coverUrl = (res && (res.url || res.path || res)) || file.url
    },
    handleUploadError() {
      this.$message.error('上传失败，请重试')
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return
        const emptyTitle = this.form.sections.find((s) => !s.title)
        if (this.form.sections.length > 0 && emptyTitle) {
          this.$message.warning('请填写所有小节的标题')
          return
        }
        this.submitting = true
        // 后端 CourseDTO/SectionDTO 期望的字段:
        //   sections[].name(不是 title),
        //   sections[].videoIds 是数组(不是单值 videoId)
        // 提交前做一次字段映射
        const payload = {
          id: this.form.id,
          name: this.form.name,
          price: this.form.price,
          tag: this.form.tag,
          status: this.form.status,
          coverUrl: this.form.coverUrl,
          intro: this.form.intro,
          professionId: this.form.professionId,
          categoryId: this.form.categoryId,
          baseStudyCount: this.form.baseStudyCount,
          baseStudyHours: this.form.baseStudyHours,
          isTop: this.form.isTop,
          topSort: this.form.topSort,
          sections: this.form.sections.map((s) => ({
            id: s.id,
            name: s.title,
            remark: s.remark || '',
            sort: s.sort,
            videoIds: s.videoId ? [s.videoId] : []
          }))
        }
        const action = this.isEdit ? updateCourse(payload) : addCourse(payload)
        action
          .then(() => {
            this.$message.success(this.isEdit ? '更新成功' : '新增成功')
            this.$router.push('/course/list').catch(() => {})
          })
          .finally(() => {
            this.submitting = false
          })
      })
    },
    goBack() {
      this.$router.push('/course/list').catch(() => {})
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

.danger-text {
  color: #f56c6c;
}
</style>
