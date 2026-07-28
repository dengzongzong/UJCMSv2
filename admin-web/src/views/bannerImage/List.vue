<template>
  <div class="app-container">
    <el-card>
      <div slot="header" class="clearfix">
        <span>首页横幅图片管理</span>
        <el-button type="primary" size="mini" icon="el-icon-plus" style="float: right" @click="handleAdd">新增横幅</el-button>
      </div>

      <el-table :data="list" border v-loading="loading" size="small" :fit="false">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="预览" width="160" align="center">
          <template slot-scope="{ row }">
            <el-image v-if="row.imageUrl" :src="apiUrl(row.imageUrl)" style="width: 140px; height: 50px" fit="cover" :preview-src-list="[apiUrl(row.imageUrl)]" />
            <span v-else class="text-muted">无图片</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题文字" min-width="180" show-overflow-tooltip />
        <el-table-column prop="linkUrl" label="跳转链接" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.linkUrl || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">{{ row.status === 1 ? '显示' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column label="操作" width="150" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" style="color: #f56c6c" icon="el-icon-delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialog.id ? '编辑横幅' : '新增横幅'" :visible.sync="dialog.visible" width="600px" :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="图片" prop="imageUrl">
          <el-upload
            action="#"
            :show-file-list="false"
            :http-request="handleUpload"
            accept="image/*"
          >
            <div v-if="form.imageUrl" class="upload-preview">
              <img :src="apiUrl(form.imageUrl)" style="width: 100%; max-height: 120px; object-fit: cover" />
              <div class="upload-mask">点击更换</div>
            </div>
            <el-button v-else type="primary" icon="el-icon-upload" size="small">上传图片</el-button>
          </el-upload>
          <div style="font-size: 12px; color: #999; margin-top: 4px">建议尺寸: 宽750px-1920px, 高度自适应的长条图片</div>
        </el-form-item>
        <el-form-item label="标题文字">
          <el-input v-model="form.title" placeholder="显示在图片上的标题(可选)" maxlength="100" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.linkUrl" placeholder="点击图片跳转的链接(可选)" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">显示</el-radio>
            <el-radio :label="0">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="submit">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { bannerImageList, addBannerImage, updateBannerImage, deleteBannerImage } from '@/api/bannerImage'
import { uploadFile as uploadRequest } from '@/api/upload'
import { apiUrl } from '@/utils/apiBase'
import tableMaxHeight from '@/mixins/tableMaxHeight'

export default {
  name: 'BannerImageList',
  mixins: [tableMaxHeight],
  data() {
    return {
      loading: false,
      list: [],
      dialog: { visible: false, id: null, submitting: false },
      form: { title: '', imageUrl: '', linkUrl: '', status: 1, sort: 0 },
      rules: {
        imageUrl: [{ required: true, message: '请上传图片', trigger: 'change' }]
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    apiUrl,
    fetchList() {
      this.loading = true
      bannerImageList().then(res => {
        this.list = res.data || []
      }).catch(() => {
        this.list = []
      }).finally(() => {
        this.loading = false
      })
    },
    handleAdd() {
      this.dialog.id = null
      this.form = { title: '', imageUrl: '', linkUrl: '', status: 1, sort: 0 }
      this.dialog.visible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialog.id = row.id
      this.form = { title: row.title || '', imageUrl: row.imageUrl, linkUrl: row.linkUrl || '', status: row.status, sort: row.sort || 0 }
      this.dialog.visible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    async handleUpload(option) {
      const formData = new FormData()
      formData.append('file', option.file)
      try {
        const res = await uploadRequest(formData)
        if (res && res.data) {
          this.form.imageUrl = res.data
          this.$message.success('上传成功')
        }
      } catch (e) {
        this.$message.error('上传失败')
      }
    },
    submit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.dialog.submitting = true
        const action = this.dialog.id
          ? updateBannerImage({ ...this.form, id: this.dialog.id })
          : addBannerImage(this.form)
        action.then(() => {
          this.$message.success(this.dialog.id ? '更新成功' : '新增成功')
          this.dialog.visible = false
          this.fetchList()
        }).finally(() => {
          this.dialog.submitting = false
        })
      })
    },
    handleDelete(row) {
      this.$confirm('确定要删除这个横幅图片吗?', '删除确认', { type: 'warning' }).then(() => {
        deleteBannerImage(row.id).then(() => {
          this.$message.success('删除成功')
          this.fetchList()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.text-muted {
  color: #999;
}
.upload-preview {
  position: relative;
  width: 100%;
  max-width: 460px;
  cursor: pointer;
  .upload-mask {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background: rgba(0, 0, 0, 0.5);
    color: #fff;
    text-align: center;
    font-size: 12px;
    padding: 4px;
    opacity: 0;
    transition: opacity 0.2s;
  }
  &:hover .upload-mask {
    opacity: 1;
  }
}
</style>
