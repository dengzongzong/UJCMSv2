<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>证书模板</span>
        <el-button style="float:right" type="primary" size="small" icon="el-icon-plus" @click="$router.push('/certificate/template/edit')">新增模板</el-button>
      </div>
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="预览" width="120">
          <template slot-scope="s">
            <el-image v-if="s.row.bgImageUrl" :src="resolveUrl(s.row.bgImageUrl)" style="width:80px;height:60px" fit="contain" :preview-src-list="[resolveUrl(s.row.bgImageUrl)]" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="证书类型" min-width="160" />
        <el-table-column label="默认" width="80">
          <template slot-scope="s">
            <el-tag v-if="s.row.isDefault === 1" type="warning" size="mini">★ 默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="背景尺寸" width="140">
          <template slot-scope="s">{{ s.row.bgWidth || '?' }} × {{ s.row.bgHeight || '?' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template slot-scope="s">
            <el-button size="mini" @click="$router.push('/certificate/template/edit/' + s.row.id)">编辑字段位置</el-button>
            <el-button v-if="s.row.isDefault !== 1" size="mini" type="warning" @click="onSetDefault(s.row.id)">设为默认</el-button>
            <el-button size="mini" type="danger" @click="onDelete(s.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { templateList, deleteTemplate, setDefaultTemplate } from '@/api/certificateTemplate'
import { apiUrl } from '@/utils/apiBase'

export default {
  name: 'CertificateTemplateList',
  data() { return { loading: false, list: [] } },
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
        const res = await templateList()
        this.list = res.data || []
      } finally { this.loading = false }
    },
    onSetDefault(id) {
      setDefaultTemplate(id).then(() => { this.$message.success('已设为默认'); this.load() })
    },
    onDelete(id) {
      this.$confirm(
        '确定删除该模板吗?\n删除后无法恢复,该模板关联的字段位置也会一并删除。',
        '删除确认',
        { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
      ).then(() => deleteTemplate(id)).then(() => {
        this.$message.success('已删除'); this.load()
      }).catch(err => {
        if (err && err !== 'cancel' && err !== 'close') {
          this.$message.error('删除失败')
        }
      })
    }
  }
}
</script>
