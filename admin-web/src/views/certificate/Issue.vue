<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>模板绑定</span>
        <el-link
          type="info"
          :underline="false"
          style="margin-left:12px;font-size:12px"
          @click="dialogVisible = true"
        >本页说明</el-link>
      </div>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom:12px"
      >
        <template #title>
          <p>本页用于: <b>① 给证书用户绑定模板</b>（绑定后, 证书用户列表会显示模板名; 单/批量下载都用此模板）</p>
          <p style="margin-top:6px"><b>② 单/批量下载</b>(选中 cert 记录, 点底部的"下载证书", 单条走 PNG/PDF, 多条走 ZIP 压缩)</p>
        </template>
      </el-alert>

      <!-- 模板选择(全局) -->
      <el-form label-width="120px" size="small" class="issue-form">
        <el-form-item label="证书模板">
          <el-select v-model="form.templateId" placeholder="默认使用系统默认模板" style="width:300px">
            <el-option
              v-for="t in templates"
              :key="t.id"
              :label="(t.isDefault === 1 ? '★ ' : '') + t.name"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <el-divider>选择要绑定模板/下载的证书记录</el-divider>

      <!-- 证书记录筛选(支持按学员筛选:同一人可有多张证书) -->
      <el-form :inline="true" size="small" class="cert-filter">
        <el-form-item label="姓名">
          <el-input v-model="filter.name" placeholder="按姓名查找该学员全部证书" clearable />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="filter.idCard" placeholder="按身份证查找该学员多张证书" clearable />
        </el-form-item>
        <el-form-item label="证书编号">
          <el-input v-model="filter.certNo" placeholder="精确匹配" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="onSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="onReset">重置</el-button>
        </el-form-item>
        <el-form-item>
          <span class="filter-tip">提示:同一学员(姓名+身份证)可持有多张证书,可分别绑定不同模板</span>
        </el-form-item>
      </el-form>

      <!-- 证书记录表(从 cert 表加载) -->
      <el-table
        v-loading="loading"
        :data="certRecords"
        border
        stripe
        max-height="380"
        @selection-change="rows => (selected = rows)"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="idCard" label="身份证号" min-width="180" />
        <el-table-column prop="certNo" label="证书编号" min-width="160" />
        <el-table-column prop="profession" label="职业" width="120" />
        <el-table-column prop="skillLevel" label="技能等级" width="100" />
        <el-table-column prop="agency" label="报单机构" min-width="120" />
        <el-table-column label="当前模板" min-width="140">
          <template slot-scope="s">
            <el-tag v-if="s.row.templateName" type="success" size="mini">{{ s.row.templateName }}</el-tag>
            <el-tag v-else type="info" size="mini">未绑定</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next"
        :current-page.sync="filter.page"
        :page-size.sync="filter.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @current-change="loadList"
        @size-change="loadList"
      />

      <el-divider />

      <!-- 底部: 绑定模板 + 单/批量下载 -->
      <el-form :model="form" label-width="160px" size="small">
        <el-form-item label="绑定/下载格式">
          <el-radio-group v-model="downloadFormat" size="mini">
            <el-radio-button label="image">图片(PNG)</el-radio-button>
            <el-radio-button label="pdf">PDF</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            icon="el-icon-medal"
            :disabled="selected.length === 0"
            :loading="submitting"
            @click="onBind"
          >
            绑定模板到选中记录({{ selected.length }})
          </el-button>
          <el-button
            icon="el-icon-download"
            :disabled="selected.length === 0"
            :loading="downloading"
            @click="onDownload"
          >
            下载选中证书({{ downloadFormat === 'pdf' ? 'PDF' : '图片' }},{{ selected.length }})
          </el-button>
          <el-button
            icon="el-icon-plus"
            @click="$router.push('/certificate/list')"
          >
            去新增证书用户
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 绑定结果弹窗 -->
    <el-dialog title="绑定结果" :visible.sync="resultVisible" width="700px">
      <el-result
        :icon="result.failedCount === 0 ? 'success' : 'warning'"
        :title="`绑定成功 ${result.issuedCount} 条 / 失败 ${result.failedCount} 条`"
      >
        <div v-if="result.failedDetails && result.failedDetails.length" class="result-detail">
          <h4>失败明细</h4>
          <el-table :data="result.failedDetails" border size="mini" max-height="240">
            <el-table-column prop="certificateId" label="证书ID" width="100" />
            <el-table-column prop="error" label="原因" />
          </el-table>
        </div>
      </el-result>
      <div slot="footer">
        <el-button @click="resultVisible = false">关闭</el-button>
      </div>
    </el-dialog>

    <!-- 说明弹窗 -->
    <el-dialog title="本页说明" :visible.sync="dialogVisible" width="600px">
      <h4>证书用户和证书使用者的区别</h4>
      <ul class="help-text">
        <li><b>证书用户</b>: 实际拿证书的人(姓名/身份证/职业/技能等级/报单机构/报单费)</li>
        <li><b>录入位置</b>: <router-link to="/certificate/list" @click="dialogVisible=false">证书管理 → 证书用户</router-link> → 新增证书用户 / Excel 导入</li>
        <li><b>学员</b>(账号系统)和<b>证书用户</b>(证书业务)是<b>两个独立的概念</b>,请勿混用</li>
      </ul>
      <h4 style="margin-top:16px">本页做什么</h4>
      <ul class="help-text">
        <li><b>绑定模板</b>: 把当前选的模板写入到证书用户(覆盖式), 绑定后证书用户列表会显示模板名</li>
        <li><b>下载证书</b>: 选中 cert 记录后, 用当前模板/格式渲染并下载
            <ul style="margin-top:4px">
              <li>1 条 → 直接下载 (PNG/PDF)</li>
              <li>多条 → 自动 ZIP 压缩 (内含 N 张 PNG 或 N 个 PDF)</li>
            </ul>
        </li>
      </ul>
    </el-dialog>
  </div>
</template>

<script>
import { templateList } from '@/api/certificateTemplate'
import { issueCertificates, certificatePage, generateCertificateBatch } from '@/api/certificate'
import { apiUrl } from '@/utils/apiBase'

export default {
  name: 'CertificateIssue',
  data() {
    return {
      templates: [],
      form: { templateId: null },
      certRecords: [],
      selected: [],
      loading: false,
      total: 0,
      filter: { name: '', idCard: '', certNo: '', page: 1, size: 10 },
      submitting: false,
      downloading: false,
      downloadFormat: 'image',
      resultVisible: false,
      result: { issuedCount: 0, failedCount: 0, failedDetails: [] },
      dialogVisible: false
    }
  },
  async mounted() {
    const t = await templateList()
    this.templates = t.data || []
    const def = this.templates.find(x => x.isDefault === 1)
    if (def) this.form.templateId = def.id
    this.loadList()
  },
  methods: {
    onSearch() { this.filter.page = 1; this.loadList() },
    onReset() {
      this.filter = { name: '', idCard: '', certNo: '', page: 1, size: 10 }
      this.loadList()
    },
    async loadList() {
      this.loading = true
      try {
        const res = await certificatePage(this.filter)
        this.certRecords = (res.data && res.data.records) || []
        this.total = (res.data && res.data.total) || 0
      } finally { this.loading = false }
    },
    /** 绑定模板(覆盖式, 写入 cert.templateId) */
    async onBind() {
      if (this.selected.length === 0) return
      this.submitting = true
      try {
        const certificateIds = this.selected.map(s => s.id)
        const payload = {
          certificateIds,
          templateId: this.form.templateId
        }
        const res = await issueCertificates(payload)
        this.result = res.data || { issuedCount: 0, failedCount: 0, failedDetails: [] }
        this.resultVisible = true
        this.loadList()
      } catch (e) {
        this.$message.error('绑定失败: ' + (e.message || '未知错误'))
      } finally {
        this.submitting = false
      }
    },
    /** 单/批量下载 */
    async onDownload() {
      if (this.selected.length === 0) return
      const ids = this.selected.map(s => s.id)
      const fmt = this.downloadFormat
      // 1 条 -> 走 single 端点(直接 PNG/PDF, 不压缩)
      if (ids.length === 1) {
        this.$message.info('正在下载...')
        try {
          // 模板优先级: 1) 选中行已绑定的 templateId  2) 全局下拉框 templateId  3) null(用系统默认)
          // 这样当用户已经给该证书绑定了模板时, 即便下拉框选的是别的模板, 也按"已绑定的"渲染
          const row = this.selected[0]
          const templateId = (row && row.templateId) || this.form.templateId || null
          // 先用 fetch + blob 拿响应, 再 triggerDownload
          const url = apiUrl('/admin/certificate/generate/single/' + ids[0]
            + '?format=' + fmt
            + (templateId ? '&templateId=' + templateId : ''))
          const token = this.$store.getters.token || ''
          const r = await fetch(url, { headers: { 'Authorization': 'Bearer ' + token } })
          if (!r.ok) throw new Error('HTTP ' + r.status)
          const blob = await r.blob()
          const a = document.createElement('a')
          a.href = URL.createObjectURL(blob)
          a.download = '证书_' + ids[0] + '.' + (fmt === 'pdf' ? 'pdf' : 'png')
          a.click()
          URL.revokeObjectURL(a.href)
        } catch (e) {
          this.$message.error('下载失败: ' + (e.message || '未知错误'))
        }
        return
      }
      // 多条 -> 走 batch 端点(返回 ZIP)
      // 模板策略: 传 null, 后端 resolveTemplate 会按行 cert.templateId 自动选
      // 这与 Issue.vue 顶部说明一致: 优先用列表中已绑定的模板
      this.downloading = true
      try {
        const res = await generateCertificateBatch({
          ids,
          templateId: null,
          format: fmt
        })
        if (res.data && res.data.async) {
          this.$message.success('已提交批量下载任务, 请在右下角"任务中心"查看进度')
          return
        }
        // 同步下载
        const token = this.$store.getters.token || ''
        const url = apiUrl('/admin/certificate/generate/batch-sync')
        const r = await fetch(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
          },
          body: JSON.stringify({
            ids, templateId: null, format: fmt
          })
        })
        if (!r.ok) throw new Error('HTTP ' + r.status)
        const blob = await r.blob()
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = 'certificates_' + Date.now() + '_' + (fmt === 'pdf' ? 'pdf' : 'png') + '.zip'
        a.click()
        URL.revokeObjectURL(a.href)
      } catch (e) {
        this.$message.error('下载失败: ' + (e.message || '未知错误'))
      } finally {
        this.downloading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.issue-form { margin-bottom: 0; }
.cert-filter { margin-bottom: 12px; }
.pagination { margin-top: 12px; text-align: right; }
.result-detail { margin-top: 12px; }
.result-detail h4 { margin: 0 0 8px; font-size: 14px; }
.help-text { padding-left: 20px; line-height: 1.8; font-size: 13px; }
.help-text b { color: #409eff; }
.filter-tip { color: #909399; font-size: 12px; }
</style>
