<template>
  <div class="app-container">
    <!-- 筛选区 -->
    <div class="filter-container">
      <el-input
        v-model="query.unitName"
        placeholder="单位名称(模糊)"
        clearable
        class="filter-item"
        style="width: 200px"
        @keyup.enter.native="onSearch"
      />
      <el-input
        v-model="query.authCode"
        placeholder="授权管理编号(精确)"
        clearable
        class="filter-item"
        style="width: 200px"
        @keyup.enter.native="onSearch"
      />
      <el-select
        v-model="query.status"
        placeholder="状态"
        clearable
        class="filter-item"
        style="width: 130px"
      >
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
      <el-button type="primary" icon="el-icon-search" class="filter-item" @click="onSearch">查询</el-button>
      <el-button icon="el-icon-refresh" class="filter-item" @click="onReset">重置</el-button>
      <el-button type="success" icon="el-icon-plus" class="filter-item" @click="openAdd">新增</el-button>
      <el-button
        type="danger"
        icon="el-icon-delete"
        class="filter-item"
        :disabled="selection.length === 0"
        @click="onBatchDelete"
      >批量删除</el-button>
      <el-button type="warning" icon="el-icon-picture-outline" class="filter-item" @click="openCertEditor">编辑授权证书</el-button>
    </div>

    <!-- 列表 -->
    <el-table
      v-loading="listLoading"
      :fit="false"
      :data="list"
      border
      stripe
      @selection-change="rows => (selection = rows)"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="unitName" label="单位名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="creditCode" label="统一社会信用代码" width="200" show-overflow-tooltip />
      <el-table-column prop="legalPerson" label="法人姓名" width="110" />
      <el-table-column prop="authCode" label="授权管理编号" width="160" show-overflow-tooltip />
      <el-table-column prop="authStartDate" label="授权开始日期" width="120" align="center" />
      <el-table-column prop="authExpireDate" label="授权有效期" width="120" align="center" />
      <el-table-column label="联系人/电话" width="160">
        <template slot-scope="s">
          {{ s.row.contactName || '-' }} / {{ s.row.contactPhone || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template slot-scope="s">
          <el-tag :type="statusTagType(s.row.status)" size="mini">
            {{ statusText(s.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
      <el-table-column label="操作" width="150" align="center">
        <template slot-scope="s">
          <el-button type="text" @click="openEdit(s.row)">编辑</el-button>
          <el-button type="text" class="danger-text" @click="onDeleteOne(s.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        :current-page="query.page"
        :page-sizes="[10, 20, 50]"
        :page-size="query.size"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="size => { query.size = size; query.page = 1; loadList() }"
        @current-change="page => { query.page = page; loadList() }"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="75%"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="160px"
        v-loading="formLoading"
      >
        <!-- 一、单位基本信息 -->
        <el-divider content-position="left">单位基本信息</el-divider>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="单位名称" prop="unitName">
              <el-input v-model="form.unitName" placeholder="请输入单位名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="统一社会信用代码" prop="creditCode">
              <el-input v-model="form.creditCode" placeholder="请输入统一社会信用代码" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="法人姓名" prop="legalPerson">
              <el-input v-model="form.legalPerson" placeholder="请输入法人姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="法人联系电话" prop="legalPersonPhone">
              <el-input v-model="form.legalPersonPhone" placeholder="请输入法人联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="法人身份证号" prop="legalPersonIdCard">
              <el-input v-model="form.legalPersonIdCard" placeholder="请输入法人身份证号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="注册资金" prop="registeredCapital">
              <el-input v-model="form.registeredCapital" placeholder="请输入注册资金" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="实缴资金" prop="paidCapital">
              <el-input v-model="form.paidCapital" placeholder="请输入实缴资金" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="成立日期" prop="establishedDate">
              <el-date-picker
                v-model="form.establishedDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择成立日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备案情况" prop="filingStatus">
              <el-select v-model="form.filingStatus" placeholder="请选择备案情况" style="width: 100%">
                <el-option label="已备案" value="已备案" />
                <el-option label="未备案" value="未备案" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="16">
            <el-form-item label="单位地址" prop="unitAddress">
              <el-input v-model="form.unitAddress" placeholder="请输入单位地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="法人身份证正面" prop="legalIdFrontImg">
              <el-upload
                :file-list="legalIdFrontList"
                list-type="picture-card"
                :limit="1"
                :before-upload="beforeUpload"
                :http-request="(opt) => onUpload(opt, 'legalIdFrontImg')"
                :on-remove="() => { form.legalIdFrontImg = '' }"
                :on-preview="handlePreview"
                action="#"
                accept="image/*"
              >
                <i class="el-icon-plus"></i>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="法人身份证反面" prop="legalIdBackImg">
              <el-upload
                :file-list="legalIdBackList"
                list-type="picture-card"
                :limit="1"
                :before-upload="beforeUpload"
                :http-request="(opt) => onUpload(opt, 'legalIdBackImg')"
                :on-remove="() => { form.legalIdBackImg = '' }"
                :on-preview="handlePreview"
                action="#"
                accept="image/*"
              >
                <i class="el-icon-plus"></i>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="营业执照" prop="businessLicenseImg">
              <el-upload
                :file-list="businessLicenseList"
                list-type="picture-card"
                :limit="1"
                :before-upload="beforeUpload"
                :http-request="(opt) => onUpload(opt, 'businessLicenseImg')"
                :on-remove="() => { form.businessLicenseImg = '' }"
                :on-preview="handlePreview"
                action="#"
                accept="image/*"
              >
                <i class="el-icon-plus"></i>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 二、主营业务信息 -->
        <el-divider content-position="left">主营业务信息</el-divider>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="主营业务" prop="mainBusiness">
              <el-input v-model="form.mainBusiness" placeholder="请输入主营业务" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="员工人数" prop="empCount">
              <el-input-number v-model="form.empCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="培训经验年数" prop="trainingYears">
              <el-input-number v-model="form.trainingYears" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="培训场地面积" prop="trainingArea">
              <el-input v-model="form.trainingArea" placeholder="如: 500平方米" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="培训设施设备" prop="trainingFacilities">
              <el-input v-model="form.trainingFacilities" placeholder="请输入培训设施设备" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="24">
            <el-form-item label="经验介绍" prop="expIntro">
              <el-input v-model="form.expIntro" type="textarea" :rows="3" placeholder="请输入经验介绍" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="24">
            <el-form-item label="招生资源介绍" prop="recruitResource">
              <el-input v-model="form.recruitResource" type="textarea" :rows="3" placeholder="请输入招生资源介绍" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="24">
            <el-form-item label="其他主营业务" prop="otherBusiness">
              <el-input v-model="form.otherBusiness" type="textarea" :rows="3" placeholder="请输入其他主营业务" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="授权管理编号" prop="authCode">
              <el-input v-model="form.authCode" placeholder="请输入授权管理编号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="授权开始日期" prop="authStartDate">
              <el-date-picker
                v-model="form.authStartDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择授权开始日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="授权有效期" prop="authExpireDate">
              <el-date-picker
                v-model="form.authExpireDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择授权有效期截止日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 三、合作意向与联系信息 -->
        <el-divider content-position="left">合作意向与联系信息</el-divider>
        <el-row :gutter="24">
          <el-col :span="24">
            <el-form-item label="合作意向" prop="cooperationIntent">
              <el-checkbox-group v-model="form.cooperationIntent">
                <el-checkbox label="全媒体运营师" />
                <el-checkbox label="网络视听主播" />
                <el-checkbox label="其他" />
              </el-checkbox-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="联系人姓名" prop="contactName">
              <el-input v-model="form.contactName" placeholder="请输入联系人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系人电话" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="0">待审核</el-radio>
                <el-radio :label="1">已通过</el-radio>
                <el-radio :label="2">已拒绝</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 图片预览 -->
    <el-dialog :visible.sync="previewVisible" append-to-body title="图片预览">
      <img :src="previewUrl" style="max-width: 100%; max-height: 70vh; display: block; margin: 0 auto" />
    </el-dialog>

    <!-- 授权证书编辑弹窗 -->
    <el-dialog title="编辑授权证书" :visible.sync="certDialogVisible" width="850px" append-to-body @opened="onCertDialogOpen">
      <div class="cert-editor-wrap">
        <!-- 上传证书背景图片 -->
        <div class="cert-upload-section">
          <el-upload
            v-if="!certForm.imageUrl"
            class="cert-uploader"
            action="#"
            :show-file-list="false"
            :before-upload="beforeCertUpload"
            :http-request="onCertImageUpload"
            accept="image/*"
          >
            <div class="cert-upload-placeholder">
              <i class="el-icon-plus"></i>
              <span>上传证书背景图片</span>
            </div>
          </el-upload>
          <div v-else class="cert-image-preview">
            <img :src="resolveCertImg(certForm.imageUrl)" alt="证书背景" />
            <el-button type="text" class="cert-change-btn" @click="certForm.imageUrl = ''">更换图片</el-button>
          </div>
        </div>

        <!-- 富文本编辑器(覆盖在图片上的文字) -->
        <div v-if="certForm.imageUrl" class="cert-text-section">
          <div class="cert-text-label">编辑覆盖在图片上的文字内容(支持富文本):</div>
          <RichEditor ref="certRichEditor" v-model="certForm.richText" :height="300" :background-image="resolveCertImg(certForm.imageUrl)" placeholder="请输入要覆盖在证书图片上的文字内容..." />
        </div>
      </div>

      <div slot="footer">
        <el-button @click="certDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="certSaving" @click="saveCertContent">保 存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  cooperationApplyPage,
  addCooperationApply,
  updateCooperationApply,
  deleteCooperationApply,
  batchDeleteCooperationApply,
  getCertContent,
  saveCertContent as saveCertContentApi
} from '@/api/cooperationApply'
import { uploadFile as uploadRequest } from '@/api/upload'
import { apiUrl } from '@/utils/apiBase'
import tableMaxHeight from '@/mixins/tableMaxHeight'
import RichEditor from '@/components/RichEditor/index.vue'

export default {
  name: 'CooperationApplyList',
  mixins: [tableMaxHeight],
  components: { RichEditor },
  data() {
    return {
      // 列表相关
      listLoading: false,
      list: [],
      total: 0,
      selection: [],
      query: {
        page: 1,
        size: 10,
        unitName: '',
        authCode: '',
        status: undefined
      },
      // 弹窗相关
      dialogVisible: false,
      dialogTitle: '新增合作申请',
      isEdit: false,
      editId: null,
      formLoading: false,
      submitting: false,
      form: this.initForm(),
      rules: {
        unitName: [{ required: true, message: '请输入单位名称', trigger: 'blur' }],
        authCode: [{ required: true, message: '请输入授权管理编号', trigger: 'blur' }]
      },
      // 图片预览
      previewVisible: false,
      previewUrl: '',
      // 授权证书编辑
      certDialogVisible: false,
      certSaving: false,
      certForm: { imageUrl: '', richText: '' }
    }
  },
  computed: {
    legalIdFrontList() {
      return this.form.legalIdFrontImg ? [{ name: '法人身份证正面', url: this.resolveFile(this.form.legalIdFrontImg) }] : []
    },
    legalIdBackList() {
      return this.form.legalIdBackImg ? [{ name: '法人身份证反面', url: this.resolveFile(this.form.legalIdBackImg) }] : []
    },
    businessLicenseList() {
      return this.form.businessLicenseImg ? [{ name: '营业执照', url: this.resolveFile(this.form.businessLicenseImg) }] : []
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    apiUrl,

    resolveFile(u) {
      if (!u) return ''
      if (u.startsWith('http')) return u
      return apiUrl(u)
    },

    statusText(status) {
      const map = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
      return map[status] !== undefined ? map[status] : '未知'
    },

    statusTagType(status) {
      const map = { 0: 'warning', 1: 'success', 2: 'danger' }
      return map[status] || 'info'
    },

    initForm() {
      return {
        unitName: '',
        creditCode: '',
        legalPerson: '',
        legalPersonPhone: '',
        legalPersonIdCard: '',
        legalIdFrontImg: '',
        legalIdBackImg: '',
        businessLicenseImg: '',
        registeredCapital: '',
        paidCapital: '',
        establishedDate: '',
        unitAddress: '',
        filingStatus: '',
        mainBusiness: '',
        empCount: 0,
        trainingYears: 0,
        trainingArea: '',
        trainingFacilities: '',
        expIntro: '',
        recruitResource: '',
        otherBusiness: '',
        authCode: '',
        authExpireDate: '',
        authStartDate: '',
        cooperationIntent: [],
        contactName: '',
        contactPhone: '',
        status: 0,
        remark: ''
      }
    },

    // ---- 列表 ----
    onSearch() {
      this.query.page = 1
      this.loadList()
    },
    onReset() {
      this.query = { page: 1, size: 10, unitName: '', authCode: '', status: undefined }
      this.loadList()
    },
    async loadList() {
      this.listLoading = true
      try {
        const params = {
          page: this.query.page,
          size: this.query.size,
          unitName: this.query.unitName || undefined,
          authCode: this.query.authCode || undefined,
          status: this.query.status
        }
        const res = await cooperationApplyPage(params)
        this.list = (res.data && (res.data.records || res.data.list || [])) || []
        this.total = (res.data && res.data.total) || 0
      } catch (e) {
        this.list = []
        this.total = 0
      } finally {
        this.listLoading = false
      }
    },

    onDeleteOne(id) {
      this.$confirm('确定要删除该合作申请吗?', '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
        .then(() => deleteCooperationApply(id))
        .then(() => { this.$message.success('删除成功'); this.loadList() })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') this.$message.error('删除失败')
        })
    },

    onBatchDelete() {
      const ids = this.selection.map(s => s.id)
      if (ids.length === 0) return
      this.$confirm(`确定要删除选中的 ${ids.length} 条记录吗?`, '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
        .then(() => batchDeleteCooperationApply(ids))
        .then(() => { this.$message.success('删除成功'); this.loadList() })
        .catch(err => {
          if (err && err !== 'cancel' && err !== 'close') this.$message.error('删除失败')
        })
    },

    // ---- 弹窗 ----
    openAdd() {
      this.isEdit = false
      this.editId = null
      this.dialogTitle = '新增合作申请'
      this.form = this.initForm()
      this.dialogVisible = true
    },

    openEdit(row) {
      this.isEdit = true
      this.editId = row.id
      this.dialogTitle = '编辑合作申请'
      this.form = {
        unitName: row.unitName || '',
        creditCode: row.creditCode || '',
        legalPerson: row.legalPerson || '',
        legalPersonPhone: row.legalPersonPhone || '',
        legalPersonIdCard: row.legalPersonIdCard || '',
        legalIdFrontImg: row.legalIdFrontImg || '',
        legalIdBackImg: row.legalIdBackImg || '',
        businessLicenseImg: row.businessLicenseImg || '',
        registeredCapital: row.registeredCapital || '',
        paidCapital: row.paidCapital || '',
        establishedDate: row.establishedDate || '',
        unitAddress: row.unitAddress || '',
        filingStatus: row.filingStatus || '',
        mainBusiness: row.mainBusiness || '',
        empCount: row.empCount || 0,
        trainingYears: row.trainingYears || 0,
        trainingArea: row.trainingArea || '',
        trainingFacilities: row.trainingFacilities || '',
        expIntro: row.expIntro || '',
        recruitResource: row.recruitResource || '',
        otherBusiness: row.otherBusiness || '',
        authCode: row.authCode || '',
        authExpireDate: row.authExpireDate || '',
        authStartDate: row.authStartDate || '',
        cooperationIntent: row.cooperationIntent
          ? (Array.isArray(row.cooperationIntent) ? row.cooperationIntent : row.cooperationIntent.split(','))
          : [],
        contactName: row.contactName || '',
        contactPhone: row.contactPhone || '',
        status: row.status !== undefined ? row.status : 0,
        remark: row.remark || ''
      }
      this.dialogVisible = true
    },

    resetForm() {
      this.form = this.initForm()
      this.$nextTick(() => {
        this.$refs.formRef && this.$refs.formRef.clearValidate()
      })
    },

    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        this.submitting = true
        const data = { ...this.form }
        // cooperationIntent: 后端是String(逗号分隔)，前端checkbox-group是数组，需要转换
        if (Array.isArray(data.cooperationIntent)) {
          data.cooperationIntent = data.cooperationIntent.join(',')
        }
        // 日期字段：空字符串转为null，避免后端LocalDate解析失败
        ;['establishedDate', 'authStartDate', 'authExpireDate'].forEach(field => {
          if (data[field] === '') {
            data[field] = null
          }
        })
        const api = this.isEdit
          ? updateCooperationApply({ ...data, id: this.editId })
          : addCooperationApply(data)
        api
          .then(() => {
            this.$message.success(this.isEdit ? '编辑成功' : '新增成功')
            this.dialogVisible = false
            this.loadList()
          })
          .catch(e => {
            this.$message.error((this.isEdit ? '编辑' : '新增') + '失败: ' + (e.message || ''))
          })
          .finally(() => {
            this.submitting = false
          })
      })
    },

    // ---- 图片上传 ----
    beforeUpload(file) {
      const isImage = /^image\//.test(file.type)
      if (!isImage) {
        this.$message.error('只能上传图片文件')
        return false
      }
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isLt5M) {
        this.$message.error('图片大小不能超过 5MB')
        return false
      }
      return true
    },

    async onUpload({ file }, field) {
      try {
        const fd = new FormData()
        fd.append('file', file)
        const res = await uploadRequest(fd)
        this.$set(this.form, field, res.data)
        this.$message.success('上传成功')
      } catch (e) {
        this.$message.error('上传失败: ' + (e.message || '未知错误'))
      }
    },

    handlePreview(file) {
      this.previewUrl = file.url
      this.previewVisible = true
    },

    // ===== 授权证书编辑相关 =====
    async openCertEditor() {
      this.certDialogVisible = true
      this.certForm = { imageUrl: '', richText: '' }
      try {
        const res = await getCertContent()
        const data = res.data || res
        if (data) {
          this.certForm.imageUrl = data.imageUrl || ''
          this.certForm.richText = data.richText || ''
        }
      } catch (e) {
        // 首次编辑,无数据,正常
      }
    },
    onCertDialogOpen() {
      // 弹窗打开后,等富文本编辑器渲染完毕再设置内容
      this.$nextTick(() => {
        if (this.$refs.certRichEditor && this.certForm.richText) {
          this.$refs.certRichEditor.setHtml(this.certForm.richText)
        }
      })
    },
    beforeCertUpload(file) {
      if (file.size > 10 * 1024 * 1024) {
        this.$message.error('图片大小不能超过10MB')
        return false
      }
      return true
    },
    async onCertImageUpload(opt) {
      try {
        const fd = new FormData()
        fd.append('file', opt.file)
        const res = await uploadRequest(fd)
        this.certForm.imageUrl = res.data
        this.$message.success('证书图片上传成功')
      } catch (e) {
        this.$message.error('上传失败: ' + (e.message || '未知错误'))
      }
    },
    resolveCertImg(url) {
      if (!url) return ''
      if (url.startsWith('http')) return url
      return apiUrl(url)
    },
    async saveCertContent() {
      if (!this.certForm.imageUrl) {
        this.$message.warning('请先上传证书背景图片')
        return
      }
      this.certSaving = true
      try {
        // 如果编辑器实例存在,获取最新HTML
        if (this.$refs.certRichEditor) {
          this.certForm.richText = this.$refs.certRichEditor.getHtml()
        }
        await saveCertContentApi({
          imageUrl: this.certForm.imageUrl,
          richText: this.certForm.richText || ''
        })
        this.$message.success('保存成功')
        this.certDialogVisible = false
      } catch (e) {
        this.$message.error('保存失败: ' + (e.message || '未知错误'))
      } finally {
        this.certSaving = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.filter-container {
  margin-bottom: 12px;
}
.filter-item {
  margin-right: 8px;
}
.pagination-container {
  margin-top: 16px;
  text-align: right;
}
.danger-text {
  color: #f56c6c;
}

/* 授权证书编辑 */
.cert-editor-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.cert-upload-section {
  text-align: center;
}
.cert-uploader {
  display: inline-block;
}
.cert-upload-placeholder {
  width: 300px;
  height: 180px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #999;
  cursor: pointer;
  transition: border-color 0.2s;
  &:hover { border-color: #409eff; color: #409eff; }
  i { font-size: 32px; }
  span { font-size: 14px; }
}
.cert-image-preview {
  position: relative;
  display: inline-block;
  img {
    max-width: 100%;
    max-height: 300px;
    border-radius: 8px;
    border: 1px solid #eee;
  }
  .cert-change-btn {
    display: block;
    margin-top: 8px;
  }
}
.cert-text-section {
  .cert-text-label {
    font-size: 14px;
    color: #606266;
    margin-bottom: 8px;
    font-weight: 500;
  }
}
</style>
