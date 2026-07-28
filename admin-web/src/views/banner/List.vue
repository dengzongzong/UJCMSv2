<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>轮播图管理</span>
        <el-button
          type="primary"
          icon="el-icon-plus"
          size="small"
          style="float: right; margin-top: -5px"
          @click="onAdd"
        >新增轮播图</el-button>
      </div>

      <el-form :inline="true" size="small" class="filter">
        <el-form-item label="标题">
          <el-input v-model="query.title" placeholder="模糊匹配" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border stripe :max-height="tableMaxHeight">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="图片" width="200">
          <template slot-scope="s">
            <el-image
              v-if="s.row.imageUrl"
              :src="resolveUrl(s.row.imageUrl)"
              :preview-src-list="[resolveUrl(s.row.imageUrl)]"
              style="width:180px;height:80px;cursor:pointer"
              fit="cover"
            />
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="linkType" label="跳转类型" width="120">
          <template slot-scope="s">
            <el-tag v-if="s.row.linkType === 1" type="success" size="mini">试卷</el-tag>
            <el-tag v-else-if="s.row.linkType === 2" type="primary" size="mini">课程</el-tag>
            <el-tag v-else size="info">纯展示</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="linkId" label="关联ID" width="100" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template slot-scope="s">
            <el-tag :type="s.row.status === 1 ? 'success' : 'info'" size="mini">
              {{ s.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="s">
            <el-button size="mini" type="primary" @click="onEdit(s.row)">编辑</el-button>
            <el-button size="mini" :type="s.row.status === 1 ? 'info' : 'success'" @click="onToggle(s.row)">
              {{ s.row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button size="mini" type="danger" @click="onDelete(s.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        background
        class="pagination"
        layout="total, sizes, prev, pager, next"
        :current-page.sync="query.page"
        :page-size.sync="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @current-change="load"
        @size-change="load"
      />
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog
      :title="form.id ? '编辑轮播图' : '新增轮播图'"
      :visible.sync="dialogVisible"
      width="640px"
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="如:开课通知 / 报名入口" />
        </el-form-item>
        <el-form-item label="图片" prop="imageUrl">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="onUpload"
            accept="image/*"
            action="#"
          >
            <el-button size="small" type="primary" icon="el-icon-upload2">点击上传</el-button>
            <div slot="tip" class="el-upload__tip">建议 1920×600 或更大分辨率,小于 10MB,优先使用 PNG 格式</div>
          </el-upload>
          <el-image
            v-if="form.imageUrl"
            :src="resolveUrl(form.imageUrl)"
            style="width:300px;height:130px;margin-top:8px;border-radius:6px"
            fit="cover"
          />
          <el-input v-model="form.imageUrl" placeholder="或直接粘贴图片 URL" style="margin-top:6px" />
        </el-form-item>
        <el-form-item label="跳转类型">
          <el-radio-group v-model="form.linkType" size="mini">
            <el-radio-button :label="0">纯展示</el-radio-button>
            <el-radio-button :label="1">试卷</el-radio-button>
            <el-radio-button :label="2">课程</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.linkType > 0" label="关联ID">
          <el-input-number v-model="form.linkId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
          <span class="hint">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status" size="mini">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  bannerPage, bannerAdd, bannerUpdate, bannerDelete
} from '@/api/banner'
import { uploadFile as uploadRequest } from '@/api/upload'
import { apiUrl } from '@/utils/apiBase'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'BannerList',
  mixins: [tableMaxHeight],
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      query: { page: 1, size: 10, title: '' },
      dialogVisible: false,
      submitting: false,
      form: {
        id: null,
        title: '',
        imageUrl: '',
        linkType: 0,
        linkId: null,
        sort: 0,
        status: 1
      },
      rules: {
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
        imageUrl: [{ required: true, message: '请上传图片', trigger: 'change' }],
        sort: [{ required: true, message: '请输入排序', trigger: 'blur' }]
      }
    }
  },
  mounted() { this.load() },
  methods: {
    resolveUrl(u) {
      if (!u) return ''
      if (u.startsWith('http')) return u
      return apiUrl(u)
    },
    async load() {
      this.loading = true
      try {
        const res = await bannerPage(this.query)
        this.list = (res.data && res.data.records) || []
        this.total = (res.data && res.data.total) || 0
      } finally { this.loading = false }
    },
    onAdd() {
      this.form = { id: null, title: '', imageUrl: '', linkType: 0, linkId: null, sort: 0, status: 1 }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate())
    },
    onEdit(row) {
      this.form = {
        id: row.id, title: row.title, imageUrl: row.imageUrl,
        linkType: row.linkType || 0, linkId: row.linkId,
        sort: row.sort || 0, status: row.status == null ? 1 : row.status
      }
      this.dialogVisible = true
    },
    onDialogClosed() {
      this.$refs.formRef && this.$refs.formRef.resetFields()
    },
    beforeUpload(file) {
      const isImg = file.type.startsWith('image/')
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isImg) { this.$message.error('只能上传图片'); return false }
      if (!isLt10M) { this.$message.error('图片大小不能超过 10MB'); return false }
      return true
    },
    async onUpload({ file }) {
      const fd = new FormData()
      fd.append('file', file)
      const res = await uploadRequest(fd)
      this.form.imageUrl = res.data
      this.$message.success('上传成功')
    },
    onSubmit() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        this.submitting = true
        try {
          if (this.form.id) {
            await bannerUpdate(this.form)
            this.$message.success('已更新')
          } else {
            await bannerAdd(this.form)
            this.$message.success('已新增')
          }
          this.dialogVisible = false
          this.load()
        } catch (e) {
          this.$message.error('保存失败: ' + (e.message || '未知错误'))
        } finally {
          this.submitting = false
        }
      })
    },
    async onToggle(row) {
      const newStatus = row.status === 1 ? 0 : 1
      await bannerUpdate({ id: row.id, status: newStatus })
      this.$message.success(newStatus === 1 ? '已启用' : '已停用')
      this.load()
    },
    onDelete(id) {
      this.$confirm('确定删除此轮播图?', '提示', { type: 'warning' })
        .then(() => bannerDelete(id))
        .then(() => { this.$message.success('已删除'); this.load() })
        .catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.filter { margin-bottom: 12px; }
.pagination { margin-top: 12px; text-align: right; }
.text-muted { color: #c0c4cc; }
.hint { margin-left: 8px; color: #999; font-size: 12px; }
</style>
