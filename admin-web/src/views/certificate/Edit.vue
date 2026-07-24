<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>{{ isEdit ? '编辑证书' : '新增证书' }}</span>
        <el-button style="float:right" size="small" @click="$router.back()">返回</el-button>
      </div>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px" size="small" style="max-width:720px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
          <span style="margin-left:8px;color:#999;font-size:12px">从身份证号第17位自动识别</span>
        </el-form-item>
        <el-form-item label="专业名称" prop="profession">
          <el-select v-model="form.profession" placeholder="请选择专业" style="width:100%" filterable>
            <el-option v-for="p in professionOptions" :key="p.id" :label="p.name" :value="p.id.toString()" />
          </el-select>
        </el-form-item>
        <el-form-item label="技能等级">
          <el-select v-model="form.skillLevel" placeholder="默认 三级/高级" allow-create filterable>
            <el-option v-for="o in skillLevelOptions" :key="o" :label="o" :value="o" />
          </el-select>
          <span style="margin-left:8px;color:#999;font-size:12px">可选择或自定义输入</span>
        </el-form-item>
        <el-form-item label="颁发日期（年）">
          <el-select v-model="form.issueYear" placeholder="请选择年份" style="width: 100%">
            <el-option v-for="year in yearOptions" :key="year" :label="year + '年'" :value="year" />
          </el-select>
        </el-form-item>
        <el-form-item label="颁发日期（月）">
          <el-select v-model="form.issueMonth" placeholder="请选择月份" style="width: 100%">
            <el-option v-for="month in monthOptions" :key="month.value" :label="month.label" :value="month.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="颁发日期（日）">
          <el-select v-model="form.issueDay" placeholder="请选择日期" style="width: 100%">
            <el-option v-for="day in dayOptions" :key="day" :label="day + '日'" :value="day" />
          </el-select>
        </el-form-item>
        <el-form-item label="证书编号">
          <el-input v-model="form.certNo" />
        </el-form-item>
        <el-form-item label="学员编号">
          <el-input v-model="form.studentNo" />
        </el-form-item>
        <el-form-item label="报单机构" prop="agency">
          <el-input v-model="form.agency" />
        </el-form-item>
        <el-form-item label="报单机构费用">
          <el-input-number v-model="form.agencyFee" :min="0" :precision="2" :step="100" />
        </el-form-item>
        <el-form-item label="证书类型">
          <el-select v-model="form.certType" placeholder="请选择证书类型" clearable filterable style="width: 100%">
            <el-option v-for="t in certTypeOptions" :key="t.id" :label="t.name" :value="t.name" />
          </el-select>
          <div class="form-tip">选择证书类型后,保存时将自动绑定同名的证书模板</div>
        </el-form-item>
        <el-form-item label="证书二维码1">
          <el-input v-model="form.qrUrl1" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="证书二维码2">
          <el-input v-model="form.qrUrl2" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="证书二维码3">
          <el-input v-model="form.qrUrl3" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="学员考试二维码">
          <el-input v-model="form.examQrUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="考试二维码">
          <el-switch v-model="examQrEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="证书模板">
          <el-select v-model="form.templateId" placeholder="不选则使用默认模板" clearable style="width: 100%">
            <el-option v-for="t in templateOptions" :key="t.id" :label="(t.isDefault ? '★ ' : '') + t.name" :value="t.id" />
          </el-select>
          <div class="form-tip">为该证书指定模板;不选则使用系统默认模板</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>

        <!-- 成绩与培训信息(系统字段,值存储在 extra) -->
        <el-form-item v-if="scoreFields.length" label-width="0">
          <el-divider content-position="left">成绩与培训信息</el-divider>
          <el-form-item v-for="f in scoreFields" :key="f.id" :label="f.fieldName">
            <el-input v-if="f.fieldType === 1" v-model="form.extra[f.fieldKey]" />
            <el-input-number v-else-if="f.fieldType === 2" v-model="form.extra[f.fieldKey]" :min="0" />
            <el-date-picker v-else-if="f.fieldType === 3" v-model="form.extra[f.fieldKey]" type="date" value-format="yyyy-MM-dd" />
            <el-select v-else-if="f.fieldType === 4" v-model="form.extra[f.fieldKey]">
              <el-option v-for="o in (f.options || '').split(',').filter(Boolean)" :key="o" :label="o" :value="o" />
            </el-select>
            <el-input v-else v-model="form.extra[f.fieldKey]" />
          </el-form-item>
        </el-form-item>

        <!-- 自定义字段 -->
        <el-form-item v-if="customFields.length" label-width="0">
          <el-divider content-position="left">自定义字段</el-divider>
          <el-form-item v-for="f in customFields" :key="f.id" :label="f.fieldName">
            <el-input v-if="f.fieldType === 1" v-model="form.extra[f.fieldKey]" />
            <el-input-number v-else-if="f.fieldType === 2" v-model="form.extra[f.fieldKey]" :min="0" />
            <el-date-picker v-else-if="f.fieldType === 3" v-model="form.extra[f.fieldKey]" type="date" value-format="yyyy-MM-dd" />
            <el-select v-else-if="f.fieldType === 4" v-model="form.extra[f.fieldKey]">
              <el-option v-for="o in (f.options || '').split(',').filter(Boolean)" :key="o" :label="o" :value="o" />
            </el-select>
            <el-input v-else v-model="form.extra[f.fieldKey]" />
          </el-form-item>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { certificateDetail, addCertificate, updateCertificate } from '@/api/certificate'
import { fieldList } from '@/api/certificateField'
import { templateList } from '@/api/certificateTemplate'
import request from '@/utils/request'

export default {
  name: 'CertificateEdit',
  data() {
    return {
      submitting: false,
      isEdit: false,
      form: {
        id: null,
        name: '', idCard: '', gender: null,
        profession: '', skillLevel: '三级/高级',
        issueYear: null, issueMonth: null, issueDay: null,
        certNo: '', studentNo: '',
        agency: '', agencyFee: 0,
        qrUrl1: '', qrUrl2: '', qrUrl3: '', examQrUrl: '',
        examQrEnabled: 0, remark: '',
        templateId: null,
        certType: '',
        extra: {}
      },
      examQrEnabled: 0,
      customFields: [],
      scoreFields: [],
      templateOptions: [],
      certTypeOptions: [],
      professionOptions: [],
      skillLevelOptions: ['五级/初级', '四级/中级', '三级/高级', '二级/技师', '一级/高级技师'],
      rules: {
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
        profession: [{ required: true, message: '请选择专业名称', trigger: 'change' }],
        agency: [{ required: true, message: '请输入报单机构', trigger: 'blur' }]
      }
    }
  },
  computed: {
    id() { return this.$route.params.id },
    yearOptions() {
      const options = []
      const currentYear = new Date().getFullYear()
      for (let year = currentYear - 10; year <= currentYear + 5; year++) {
        options.push(year.toString())
      }
      return options
    },
    monthOptions() {
      const options = []
      for (let month = 1; month <= 12; month++) {
        options.push({ value: month.toString().padStart(2, '0'), label: month + '月' })
      }
      return options
    },
    dayOptions() {
      const options = []
      let maxDay = 31
      if (this.form.issueMonth) {
        const month = parseInt(this.form.issueMonth)
        const year = parseInt(this.form.issueYear) || new Date().getFullYear()
        const daysInMonth = new Date(year, month, 0).getDate()
        maxDay = daysInMonth
      }
      for (let day = 1; day <= maxDay; day++) {
        options.push(day.toString().padStart(2, '0'))
      }
      return options
    }
  },
  watch: {
    examQrEnabled(v) { this.form.examQrEnabled = v }
  },
  async mounted() {
    const fields = await fieldList().then(r => r.data)
    // 成绩与培训相关系统字段(值存储在 extra 对象中,fieldKey -> value)
    const SCORE_FIELD_KEYS = ['theoryScore', 'practicalScore', 'comprehensiveEvaluation', 'trainingMajor', 'trainingHours', 'trainingDate']
    this.scoreFields = (fields || []).filter(f => SCORE_FIELD_KEYS.includes(f.fieldKey))
    this.customFields = (fields || []).filter(f => f.isSystem === 0 && !SCORE_FIELD_KEYS.includes(f.fieldKey))
    // 加载证书模板选项(用于"证书模板"下拉;支持一人多证绑定不同模板)
    templateList().then(r => { this.templateOptions = r.data || [] }).catch(() => {})
    // 加载证书类型选项(用于"证书类型"下拉)
    request({ url: '/public/certificate-types', method: 'get' }).then(r => {
      this.certTypeOptions = r.data || []
    }).catch(() => {})
    // 加载专业选项(用于"专业名称"下拉)
    request({ url: '/public/professions', method: 'get' }).then(r => {
      this.professionOptions = r.data || []
    }).catch(() => {})
    if (this.id) {
      this.isEdit = true
      const res = await certificateDetail(this.id)
      const d = res.data
      let issueYear = null, issueMonth = null, issueDay = null
        if (d.issueDate) {
          const dateParts = d.issueDate.split('-')
          if (dateParts.length === 3) {
            issueYear = dateParts[0]
            issueMonth = dateParts[1]
            issueDay = dateParts[2]
          }
        }
        this.form = {
          id: d.id,
          name: d.name, idCard: d.idCard, gender: d.gender,
          profession: d.profession, skillLevel: d.skillLevel,
          issueYear, issueMonth, issueDay,
          certNo: d.certNo, studentNo: d.studentNo,
          agency: d.agency, agencyFee: d.agencyFee ? Number(d.agencyFee) : 0,
          qrUrl1: d.qrUrl1, qrUrl2: d.qrUrl2, qrUrl3: d.qrUrl3, examQrUrl: d.examQrUrl,
          examQrEnabled: d.examQrEnabled || 0, remark: d.remark,
          templateId: d.templateId || null,
          certType: d.certType || '',
          extra: d.extra || {}
        }
      this.examQrEnabled = d.examQrEnabled || 0
    }
  },
  methods: {
    onSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitting = true
        const submitData = { ...this.form }
        if (submitData.issueYear && submitData.issueMonth && submitData.issueDay) {
          submitData.issueDate = `${submitData.issueYear}-${submitData.issueMonth}-${submitData.issueDay}`
        }
        delete submitData.issueYear
        delete submitData.issueMonth
        delete submitData.issueDay
        const api = this.isEdit ? updateCertificate : addCertificate
        api(submitData).then(() => {
          this.$message.success('保存成功')
          this.$router.push('/certificate/list')
        }).finally(() => { this.submitting = false })
      })
    }
  }
}
</script>

<style scoped>
.form-tip { font-size: 12px; color: #909399; line-height: 1.4; margin-top: 4px; }
</style>
