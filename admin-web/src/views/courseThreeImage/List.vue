<template>
  <div class="app-container">
    <!-- 筛选区 -->
    <el-form :inline="true" :model="query" size="small" class="filter-form">
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width:120px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="onSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" icon="el-icon-plus" size="small" @click="openDialog()">新增</el-button>
      <el-button type="danger" icon="el-icon-delete" size="small" :disabled="selection.length === 0" @click="onDelete">批量删除</el-button>
      <span class="hint">提示:课程ID 留空表示全站通用图;每个课程最多展示 3 张图(用户端"测评服务平台"区块使用)。</span>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :max-height="tableMaxHeight"
      :data="list"
      border
      stripe
      @selection-change="rows => (selection = rows)"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column label="图片" width="120" align="center">
        <template slot-scope="s">
          <el-image
            v-if="s.row.imageUrl"
            :src="resolveImg(s.row.imageUrl)"
            :preview-src-list="[resolveImg(s.row.imageUrl)]"
            style="width:90px;height:60px"
            fit="cover"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column label="跳转类型" width="100" align="center">
        <template slot-scope="s">
          <el-tag v-if="s.row.linkType === 0" size="mini">不跳转</el-tag>
          <el-tag v-else-if="s.row.linkType === 1" type="success" size="mini">外链</el-tag>
          <el-tag v-else-if="s.row.linkType === 2" type="warning" size="mini">试卷</el-tag>
          <el-tag v-else-if="s.row.linkType === 3" type="info" size="mini">课程</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="跳转目标" min-width="220">
        <template slot-scope="s">
          <span v-if="s.row.linkType === 1" class="link">{{ s.row.linkUrl }}</span>
          <span v-else-if="s.row.linkType === 2 || s.row.linkType === 3">ID: {{ s.row.linkId }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="课程ID" width="100" align="center">
        <template slot-scope="s">
          <span v-if="s.row.courseId">{{ s.row.courseId }}</span>
          <el-tag v-else type="info" size="mini">全站通用</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template slot-scope="s">
          <el-tag :type="s.row.status === 1 ? 'success' : 'info'" size="mini">
            {{ s.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template slot-scope="s">
          <el-button size="mini" type="primary" @click="openDialog(s.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="onDeleteOne(s.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :current-page.sync="query.page"
      :page-size.sync="query.size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="loadList"
      @size-change="loadList"
    />

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="form.id ? '编辑三图' : '新增三图'"
      :visible.sync="dialogVisible"
      width="640px"
      :close-on-click-modal="false"
      @closed="onDialogClosed"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" size="small">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="图片标题(选填)" maxlength="100" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <RichEditor v-model="form.description" :height="200" placeholder="描述文字(支持富文本，会替代标题在首页展示)" />
          <div class="form-tip">支持富文本: 加粗/字号/颜色/链接/图片/表格等;此描述会替代标题在首页展示</div>
        </el-form-item>
        <el-form-item label="图片" prop="imageUrl">
          <el-upload
            class="image-uploader"
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="onUpload"
            accept="image/*"
            action="#"
          >
            <el-button type="success" icon="el-icon-upload" :loading="uploading">
              {{ form.imageUrl ? '重新上传' : '选择图片' }}
            </el-button>
            <span style="margin-left:12px;color:#909399;font-size:12px">不填则纯文字展示</span>
          </el-upload>
          <div v-if="form.imageUrl" class="image-preview-wrapper">
            <el-image
              :src="resolveImg(form.imageUrl)"
              :preview-src-list="[resolveImg(form.imageUrl)]"
              style="width:160px;height:90px"
              fit="cover"
            />
            <el-button type="danger" size="mini" icon="el-icon-delete" class="delete-image-btn" @click="onDeleteImage">删除图片</el-button>
          </div>
        </el-form-item>
        <el-form-item label="关联课程">
          <el-input v-model.number="form.courseId" placeholder="留空=全站通用" type="number" />
        </el-form-item>
        <el-form-item label="跳转类型">
          <el-select v-model="form.linkType" style="width:100%">
            <el-option label="不跳转" :value="0" />
            <el-option label="跳转外链" :value="1" />
            <el-option label="跳转试卷" :value="2" />
            <el-option label="跳转课程" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.linkType === 1" label="外链URL">
          <el-input v-model="form.linkUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item v-if="form.linkType === 2 || form.linkType === 3" label="目标ID">
          <el-input v-model.number="form.linkId" type="number" placeholder="试卷/课程ID" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  courseThreeImagePage,
  addCourseThreeImage,
  updateCourseThreeImage,
  deleteCourseThreeImage
} from '@/api/courseThreeImage'
import { uploadFile as uploadRequest } from '@/api/upload'
import { apiUrl } from '@/utils/apiBase'
import RichEditor from '@/components/RichEditor'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'CourseThreeImageList',
  mixins: [tableMaxHeight],
  components: { RichEditor },
  data() {
    return {
      loading: false,
      query: { page: 1, size: 10, status: null },
      list: [],
      total: 0,
      selection: [],
      dialogVisible: false,
      saving: false,
      uploading: false,
      form: this.initForm(),
      rules: {
        title: [{ max: 100, message: '最多 100 字', trigger: 'blur' }],
        description: [{ max: 500, message: '最多 500 字', trigger: 'blur' }]
      }
    }
  },
  mounted() {
    this.loadList()
  },
  methods: {
    apiUrl,
    resolveImg(u) {
      if (!u) return ''
      if (u.startsWith('http')) return u
      return apiUrl(u)
    },
    initForm() {
      return {
        id: null,
        courseId: null,
        title: '',
        description: '',
        imageUrl: '',
        linkType: 0,
        linkUrl: '',
        linkId: null,
        sort: 0,
        status: 1
      }
    },
    onSearch() {
      this.query.page = 1
      this.loadList()
    },
    onReset() {
      this.query = { page: 1, size: 10, status: null }
      this.loadList()
    },
    async loadList() {
      this.loading = true
      try {
        const res = await courseThreeImagePage(this.query)
        this.list = res.data.records || []
        this.total = res.data.total || 0
      } finally {
        this.loading = false
      }
    },
    openDialog(row) {
      if (row) {
        this.form = {
          id: row.id,
          courseId: row.courseId,
          title: row.title || '',
          description: row.description || '',
          imageUrl: row.imageUrl || '',
          linkType: row.linkType,
          linkUrl: row.linkUrl || '',
          linkId: row.linkId,
          sort: row.sort,
          status: row.status
        }
      } else {
        this.form = this.initForm()
      }
      this.dialogVisible = true
    },
    onDeleteImage() {
      this.$confirm('确定删除图片吗?', '删除确认', { type: 'warning' })
        .then(() => {
          this.form.imageUrl = ''
          this.$message.success('图片已删除')
        })
        .catch(() => {})
    },
    onDialogClosed() {
      this.form = this.initForm()
      this.saving = false
    },
    beforeUpload(file) {
      const isImg = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isImg) { this.$message.error('只能上传图片'); return false }
      if (!isLt5M) { this.$message.error('图片不能超过 5MB'); return false }
      return true
    },
    async onUpload({ file }) {
      this.uploading = true
      try {
        const fd = new FormData()
        fd.append('file', file)
        const res = await uploadRequest(fd)
        this.form.imageUrl = res.data
        this.$message.success('上传成功')
      } catch (e) {
        this.$message.error('上传失败: ' + (e.message || '未知错误'))
      } finally {
        this.uploading = false
      }
    },
    async onSubmit() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        this.saving = true
        try {
          const payload = { ...this.form }
          // 跳转类型 0:清空 linkUrl/linkId
          if (payload.linkType === 0) {
            payload.linkUrl = ''
            payload.linkId = null
          } else if (payload.linkType === 1) {
            payload.linkId = null
          } else {
            payload.linkUrl = ''
          }
          if (payload.id) {
            await updateCourseThreeImage(payload)
          } else {
            delete payload.id
            await addCourseThreeImage(payload)
          }
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.loadList()
        } catch (e) {
          this.$message.error('保存失败: ' + (e.message || '未知错误'))
        } finally {
          this.saving = false
        }
      })
    },
    onDeleteOne(id) {
      this.$confirm('确定删除该图片吗?', '删除确认', {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }).then(() => deleteCourseThreeImage([id]))
        .then(() => { this.$message.success('删除成功'); this.loadList() })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') this.$message.error('删除失败')
        })
    },
    onDelete() {
      const ids = this.selection.map(s => s.id)
      this.$confirm('确定删除选中的 ' + ids.length + ' 张图片吗?', '删除确认', {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }).then(() => deleteCourseThreeImage(ids))
        .then(() => { this.$message.success('删除成功'); this.loadList() })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') this.$message.error('删除失败')
        })
    }
  }
}
</script>

<style scoped>
.filter-form { margin-bottom: 12px; }
.toolbar { margin-bottom: 12px; }
.toolbar .el-button { margin-right: 8px; }
.hint { margin-left: 12px; color: #909399; font-size: 12px; }
.pagination { margin-top: 16px; text-align: right; }
.link { color: #409EFF; word-break: break-all; }
.image-preview-wrapper {
  position: relative;
  display: inline-block;
  margin-top: 8px;
}
.image-preview-wrapper .el-image {
  border-radius: 4px;
  border: 1px solid #ddd;
}
.delete-image-btn {
  position: absolute;
  top: 4px;
  right: 4px;
}
</style>
