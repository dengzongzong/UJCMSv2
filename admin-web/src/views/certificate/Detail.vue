<template>
  <div class="app-container">
    <el-card v-loading="loading">
      <div slot="header">
        <span>证书详情</span>
        <el-button style="float:right" size="small" @click="$router.back()">返回</el-button>
      </div>
      <el-descriptions :column="2" border size="medium">
        <el-descriptions-item label="证书编号">{{ data.certNo }}</el-descriptions-item>
        <el-descriptions-item label="学员编号">{{ data.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ data.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ data.genderName || (data.gender === 1 ? '男' : data.gender === 2 ? '女' : '-') }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ data.idCard }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ getBirthdayFromIdCard(data.idCard) }}</el-descriptions-item>
        <el-descriptions-item label="职业名称">{{ data.profession }}</el-descriptions-item>
        <el-descriptions-item label="技能等级">{{ data.skillLevel }}</el-descriptions-item>
        <el-descriptions-item label="颁发日期">{{ data.issueDate }}</el-descriptions-item>
        <el-descriptions-item label="报单机构">{{ data.agency }}</el-descriptions-item>
        <el-descriptions-item label="报单机构费用">{{ data.agencyFee }}</el-descriptions-item>
        <el-descriptions-item label="证书二维码1">
          <a v-if="data.qrUrl1" :href="data.qrUrl1" target="_blank">查看</a>
        </el-descriptions-item>
        <el-descriptions-item label="证书二维码2">
          <a v-if="data.qrUrl2" :href="data.qrUrl2" target="_blank">查看</a>
        </el-descriptions-item>
        <el-descriptions-item label="证书二维码3">
          <a v-if="data.qrUrl3" :href="data.qrUrl3" target="_blank">查看</a>
        </el-descriptions-item>
        <el-descriptions-item label="学员考试二维码">
          <a v-if="data.examQrUrl" :href="data.examQrUrl" target="_blank">查看</a>
        </el-descriptions-item>
        <el-descriptions-item label="考试二维码状态">
          <el-tag v-if="data.examQrEnabled === 1" type="success">已开启</el-tag>
          <el-tag v-else type="info">已关闭</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ data.remark }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ data.createTime }}</el-descriptions-item>
      </el-descriptions>

      <!-- 自定义字段 -->
      <el-divider v-if="customFieldValues.length" content-position="left">自定义字段</el-divider>
      <el-descriptions v-if="customFieldValues.length" :column="2" border size="medium">
        <el-descriptions-item v-for="(f, i) in customFieldValues" :key="i" :label="f.label">{{ f.value }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top:24px">
        <el-dropdown @command="onDownload" trigger="click">
          <el-button type="primary">下载证书 <i class="el-icon-arrow-down el-icon--right"></i></el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="image">下载为图片(PNG)</el-dropdown-item>
            <el-dropdown-item command="pdf">下载为 PDF</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </el-card>
  </div>
</template>

<script>
import { certificateDetail, downloadFile, triggerDownload } from '@/api/certificate'

export default {
  name: 'CertificateDetail',
  data() {
    return { loading: false, data: {}, customFieldValues: [] }
  },
  computed: { id() { return this.$route.params.id } },
  async mounted() {
    this.loading = true
    try {
      const res = await certificateDetail(this.id)
      this.data = res.data
      const fields = (res.data.fields || []).filter(f => f.isSystem === 0)
      this.customFieldValues = fields.map(f => ({
        label: f.fieldName,
        value: res.data.extra && res.data.extra[f.fieldKey] !== undefined ? res.data.extra[f.fieldKey] : '-'
      }))
    } finally { this.loading = false }
  },
  methods: {
    getBirthdayFromIdCard(idCard) {
      if (!idCard) return '-'
      if (idCard.length === 18) {
        return idCard.substring(6, 10) + '-' + idCard.substring(10, 12) + '-' + idCard.substring(12, 14)
      }
      if (idCard.length === 15) {
        return '19' + idCard.substring(6, 8) + '-' + idCard.substring(8, 10) + '-' + idCard.substring(10, 12)
      }
      return '-'
    },
    onDownload(format) {
      const f = format || 'image'
      this.$message.info('正在下载...')
      downloadFile('/admin/certificate/generate/single/' + this.id + '?format=' + f)
        .then(({ blob, fileName }) => {
          const ext = f === 'pdf' ? 'pdf' : 'png'
          triggerDownload(blob, fileName || ('证书_' + this.id + '.' + ext))
        })
        .catch(err => this.$message.error('下载失败: ' + (err.message || '未知错误')))
    }
  }
}
</script>
