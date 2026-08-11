<template>
  <div class="app-container">
    <el-card>
      <div slot="header">学员照片</div>
      <el-form :inline="true" :model="query" size="small" class="filter-form">
        <el-form-item label="身份证">
          <el-input v-model="query.idCard" clearable />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.name" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
        </el-form-item>
        <el-form-item>
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="onUpload"
            accept="image/*"
            action="#"
          >
            <el-button type="success" icon="el-icon-upload">上传照片</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="warning" icon="el-icon-picture-outline" @click="openCertPhotoDialog">为指定证书上传照片</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="idCard" label="身份证" min-width="180" align="center" />
        <el-table-column prop="name" label="姓名" width="120" align="center" />
        <el-table-column label="关联证书" width="200" align="center">
          <template slot-scope="s">
            <span v-if="s.row.certificateId">证书#{{ s.row.certificateId }}</span>
            <span v-else style="color:#909399">通用(按身份证)</span>
          </template>
        </el-table-column>
        <el-table-column label="照片" width="160" align="center">
          <template slot-scope="s">
            <el-image :src="resolveUrl(s.row.url)" style="width:80px;height:80px" fit="cover" :preview-src-list="[resolveUrl(s.row.url)]" />
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="180" align="center" />
        <el-table-column label="操作" width="120" align="center">
          <template slot-scope="s">
            <el-button size="mini" type="danger" @click="onDelete(s.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :current-page.sync="query.page"
        :page-size.sync="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @current-change="load"
        @size-change="load"
      />

      <!-- 普通上传弹窗 -->
      <el-dialog title="上传学员照片" :visible.sync="dialogVisible" width="500px" append-to-body @closed="onDialogClosed">
        <el-form :model="form" label-width="100px" size="small">
          <el-form-item label="身份证" required>
            <el-input v-model="form.idCard" />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="照片">
            <el-image v-if="form.url" :src="resolveUrl(form.url)" style="width:120px;height:120px" fit="cover" />
          </el-form-item>
        </el-form>
        <div slot="footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
        </div>
      </el-dialog>

      <!-- 为指定证书上传照片弹窗 -->
      <el-dialog title="为指定证书上传照片" :visible.sync="certPhotoDialog.visible" width="640px" append-to-body @closed="onCertDialogClosed">
        <el-form :model="certPhotoDialog.form" label-width="120px" size="small">
          <el-form-item label="身份证号" required>
            <el-input v-model="certPhotoDialog.form.idCard" placeholder="输入身份证号后点击查询" style="width:300px">
              <el-button slot="append" icon="el-icon-search" @click="searchCerts">查询证书</el-button>
            </el-input>
          </el-form-item>
          <el-form-item label="选择证书记录" required>
            <el-select v-model="certPhotoDialog.form.certificateId" placeholder="请先查询证书" style="width:400px" @change="onCertSelect">
              <el-option
                v-for="cert in certPhotoDialog.certList"
                :key="cert.id"
                :label="'证书#' + cert.id + ' - ' + (cert.profession || '通用') + (cert.skillLevel ? ' [' + cert.skillLevel + ']' : '') + (cert.certNo ? ' (' + cert.certNo + ')' : '')"
                :value="cert.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="certPhotoDialog.form.name" style="width:200px" />
          </el-form-item>
          <el-form-item label="照片" required>
            <el-upload
              :show-file-list="false"
              :before-upload="beforeCertPhotoUpload"
              :http-request="onCertPhotoUpload"
              accept="image/*"
              action="#"
            >
              <el-button type="primary" icon="el-icon-upload">选择照片</el-button>
            </el-upload>
            <el-image v-if="certPhotoDialog.form.url" :src="resolveUrl(certPhotoDialog.form.url)" style="width:120px;height:120px;margin-top:8px" fit="cover" />
          </el-form-item>
        </el-form>
        <div slot="footer">
          <el-button @click="certPhotoDialog.visible = false">取消</el-button>
          <el-button type="primary" :loading="certPhotoDialog.submitting" @click="onCertPhotoSubmit">保存</el-button>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import { photoPage, addPhoto, deletePhoto, uploadPhotoForCertificate, getCertificatesByIdCard } from '@/api/certificate'
import { uploadFile as uploadRequest } from '@/api/upload'
import { apiUrl } from '@/utils/apiBase'

export default {
  name: 'CertificatePhotoList',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      query: { page: 1, size: 10, idCard: '', name: '' },
      dialogVisible: false,
      submitting: false,
      form: { idCard: '', name: '', url: '' },
      certPhotoDialog: {
        visible: false,
        submitting: false,
        certList: [],
        form: { idCard: '', certificateId: null, name: '', url: '' }
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
        const res = await photoPage(this.query)
        this.list = res.data.records || []
        this.total = res.data.total || 0
      } finally { this.loading = false }
    },
    beforeUpload(file) {
      const isImg = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isImg) { this.$message.error('只能上传图片'); return false }
      if (!isLt5M) { this.$message.error('图片大小不能超过 5MB'); return false }
      return true
    },
    async onUpload({ file }) {
      const fd = new FormData()
      fd.append('file', file)
      const res = await uploadRequest(fd)
      this.form.url = res.data
      this.dialogVisible = true
    },
    onSubmit() {
      if (!this.form.idCard) { this.$message.warning('请输入身份证号'); return }
      if (!this.form.url) { this.$message.warning('请先上传照片'); return }
      this.submitting = true
      addPhoto(this.form).then(() => {
        this.$message.success('保存成功')
        this.dialogVisible = false
        this.load()
      }).finally(() => { this.submitting = false })
    },
    onDelete(id) {
      this.$confirm('确定删除?', '提示', { type: 'warning' }).then(() => deletePhoto([id])).then(() => {
        this.$message.success('已删除'); this.load()
      }).catch(() => {})
    },
    onDialogClosed() {
      this.form = { idCard: '', name: '', url: '' }
    },
    // === 为指定证书上传照片 ===
    openCertPhotoDialog() {
      this.certPhotoDialog.visible = true
    },
    async searchCerts() {
      if (!this.certPhotoDialog.form.idCard) {
        this.$message.warning('请输入身份证号')
        return
      }
      try {
        const res = await getCertificatesByIdCard(this.certPhotoDialog.form.idCard)
        this.certPhotoDialog.certList = res.data || []
        if (this.certPhotoDialog.certList.length === 0) {
          this.$message.info('未找到该身份证号的证书记录')
        } else {
          this.$message.success('找到 ' + this.certPhotoDialog.certList.length + ' 条证书记录')
        }
      } catch (e) {
        this.$message.error('查询失败')
      }
    },
    onCertSelect(certId) {
      var cert = this.certPhotoDialog.certList.find(c => c.id === certId)
      if (cert) {
        this.certPhotoDialog.form.name = cert.name || ''
      }
    },
    beforeCertPhotoUpload(file) {
      return this.beforeUpload(file)
    },
    async onCertPhotoUpload({ file }) {
      const fd = new FormData()
      fd.append('file', file)
      const res = await uploadRequest(fd)
      this.certPhotoDialog.form.url = res.data
      this.$message.success('照片已上传,请点击保存')
    },
    onCertPhotoSubmit() {
      if (!this.certPhotoDialog.form.idCard) { this.$message.warning('请输入身份证号'); return }
      if (!this.certPhotoDialog.form.certificateId) { this.$message.warning('请选择证书记录'); return }
      if (!this.certPhotoDialog.form.url) { this.$message.warning('请先上传照片'); return }
      this.certPhotoDialog.submitting = true
      const fd = new FormData()
      fd.append('file', this.dataURItoFile(this.certPhotoDialog.form.url))
      // 由于已通过 uploadRequest 上传了文件,这里直接用 url 提交
      // 改用直接调用 addPhoto 带 certificateId
      addPhoto({
        idCard: this.certPhotoDialog.form.idCard,
        name: this.certPhotoDialog.form.name,
        url: this.certPhotoDialog.form.url,
        certificateId: this.certPhotoDialog.form.certificateId
      }).then(() => {
        this.$message.success('照片已关联到指定证书')
        this.certPhotoDialog.visible = false
        this.load()
      }).catch(() => {
        this.$message.error('保存失败')
      }).finally(() => {
        this.certPhotoDialog.submitting = false
      })
    },
    onCertDialogClosed() {
      this.certPhotoDialog.form = { idCard: '', certificateId: null, name: '', url: '' }
      this.certPhotoDialog.certList = []
    }
  }
}
</script>
