<template>
  <div class="app-container">
    <!-- 筛选区 -->
    <el-form :inline="true" :model="query" size="small" class="filter-form">
      <el-form-item label="姓名">
        <el-input v-model="query.name" clearable placeholder="姓名" />
      </el-form-item>
      <el-form-item label="身份证">
        <el-input v-model="query.idCard" clearable placeholder="证件号码" />
      </el-form-item>
      <el-form-item label="报单机构">
        <el-input v-model="query.agency" clearable placeholder="报单机构" />
      </el-form-item>
      <el-form-item label="专业">
        <el-input v-model="query.profession" clearable placeholder="专业/职业名称" />
      </el-form-item>
      <el-form-item label="颁发日期">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="yyyy-MM-dd"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="onSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏(只保留新增/同步/批量开关/批量删除) -->
    <div class="toolbar">
      <el-button type="primary" icon="el-icon-plus" size="small" @click="$router.push('/certificate/edit')">新增证书用户</el-button>
      <el-button type="warning" icon="el-icon-refresh" size="small" :loading="syncingUsers" @click="onSyncFromStudents">从学生管理同步</el-button>
      <el-button type="warning" icon="el-icon-picture-outline" size="small" @click="openBatchPhotoDialog">批量导入照片</el-button>
      <el-button
        :type="unboundTemplateFilter ? 'primary' : 'default'"
        icon="el-icon-filter"
        size="small"
        @click="toggleUnboundTemplate"
      >
        {{ unboundTemplateFilter ? '显示全部' : '未绑定证书模板' }}
      </el-button>
      <el-button type="primary" icon="el-icon-download" size="small" :disabled="selection.length === 0" @click="openDownloadFormatDialog">批量下载(选中)</el-button>
      <el-button type="primary" icon="el-icon-files" size="small" @click="openDownloadAllFormatDialog">批量下载(全部)</el-button>
      <el-button type="success" icon="el-icon-document" size="small" :disabled="selection.length === 0" :loading="exporting" @click="onExportSelected">导出数据(选中)</el-button>
      <el-button type="success" icon="el-icon-document" size="small" :loading="exporting" @click="onExportAll">导出数据(全部)</el-button>
      <el-button type="info" icon="el-icon-position" size="small" :disabled="selection.length === 0" @click="onSwitchExamQr(1)">开启考试二维码</el-button>
      <el-button type="info" icon="el-icon-close" size="small" :disabled="selection.length === 0" @click="onSwitchExamQr(0)">关闭考试二维码</el-button>
      <el-button type="success" icon="el-icon-files" size="small" :disabled="selection.length === 0" @click="openBatchTemplateDialog">批量绑定模板</el-button>
      <el-button type="danger" icon="el-icon-delete" size="small" :disabled="selection.length === 0" @click="onDelete">批量删除</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="list"
      border
      stripe
      @selection-change="rows => (selection = rows)"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column label="照片" width="80" align="center">
        <template slot-scope="s">
          <img v-if="s.row.photoUrl" :src="apiUrl(s.row.photoUrl)" class="table-thumb" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="certType" label="证书类型" min-width="140" show-overflow-tooltip>
        <template slot-scope="s">
          <el-tag v-if="s.row.certType" size="mini" type="warning">{{ s.row.certType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="性别" width="60">
        <template slot-scope="s">{{ s.row.gender === 1 ? '男' : s.row.gender === 2 ? '女' : '-' }}</template>
      </el-table-column>
      <el-table-column prop="idCard" label="身份证" min-width="180">
        <template slot-scope="{ row }">
          <span :class="{ 'id-card-invalid': row.idCard && !validateIdCard(row.idCard) }">{{ row.idCard || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="出生日期" width="120">
        <template slot-scope="s">{{ getBirthdayFromIdCard(s.row.idCard) || '-' }}</template>
      </el-table-column>
      <el-table-column prop="profession" label="专业" min-width="120" />
      <el-table-column prop="skillLevel" label="技能等级" width="120" />
      <el-table-column label="颁发日期" width="150">
        <template slot-scope="s">{{ formatIssueDate(s.row.issueDateStr, s.row.issueDate) }}</template>
      </el-table-column>
      <el-table-column prop="certNo" label="证书编号" min-width="160" />
      <el-table-column prop="studentNo" label="学员编号" min-width="160" />
      <el-table-column prop="agency" label="报单机构" min-width="160" />
      <el-table-column prop="theoryScore" label="理论成绩" width="100" />
      <el-table-column prop="practicalScore" label="实操成绩" width="100" />
      <el-table-column prop="comprehensiveEvaluation" label="综合测评" width="100" />
      <el-table-column label="导入时间" width="170">
        <template slot-scope="s">
          <span>{{ formatDateTime(s.row.uploadTime || s.row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="templateName" label="模板" min-width="120">
        <template slot-scope="s">
          <el-tag v-if="s.row.templateName" type="success" size="mini">{{ s.row.templateName }}</el-tag>
          <el-tag v-else type="info" size="mini">未绑定</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="考试二维码" width="100">
        <template slot-scope="s">
          <el-tag v-if="s.row.examQrEnabled === 1" type="success" size="mini">已开启</el-tag>
          <el-tag v-else type="info" size="mini">已关闭</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template slot-scope="s">
          <div class="action-row">
            <el-button size="mini" type="primary" @click="$router.push('/certificate/edit/' + s.row.id)">编辑</el-button>
            <el-button size="mini" type="success" @click="onOpenTemplateDialog(s.row)">绑定模板</el-button>
            <el-button size="mini" type="info" @click="onPreviewCert(s.row)">预览</el-button>
            <el-dropdown size="mini" split-button type="info" @click="onDownloadSingle(s.row, 'image')" @command="cmd => onDownloadSingle(s.row, cmd)" :loading="s.row._downloading">
              下载
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="image">下载图片</el-dropdown-item>
                <el-dropdown-item command="pdf">下载PDF</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
          <div class="action-row">
            <el-button size="mini" type="warning" @click="onOpenPhotoUpload(s.row)">上传照片</el-button>
            <el-button size="mini" type="danger" @click="onDeleteOne(s.row.id)">删除</el-button>
          </div>
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

    <!-- 导入对话框(一次性解析+校验+入库) -->
    <el-dialog
      title="Excel 导入证书用户"
      :visible.sync="importDialog"
      width="900px"
      @closed="onImportClosed"
      :close-on-click-modal="false"
    >
      <div v-if="!importResult">
        <el-alert
          title="提示:上传后会自动解析 + 校验 + 入库(< 50 行同步,>= 50 行异步)。统一导入模板包含20列:序号、姓名、性别、证件号码、职业名称、技能等级、证书编号、颁发日期、报单机构、报单机构费用统计、培训专业、培训学时、培训日期、理论成绩、实操成绩、综合测评、证书二维码生成1-3、学员考试二维码"
          type="info"
          show-icon
          :closable="false"
          style="margin-bottom:12px"
        />
        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :limit="1"
          accept=".xls,.xlsx"
          :on-change="onFileChange"
          :on-exceed="onExceed"
          :file-list="fileList"
          action="#"
        >
          <i class="el-icon-upload"></i>
          <div class="el-upload__text">将 Excel 文件拖到此处,或<em>点击上传</em></div>
          <div slot="tip" class="el-upload__tip">支持 .xls / .xlsx 格式</div>
        </el-upload>
      </div>
      <div v-else>
        <el-alert
          :title="'导入完成:成功 ' + importResult.successCount + ' 条,失败 ' + importResult.failCount + ' 条'"
          :type="importResult.failCount > 0 ? 'warning' : 'success'"
          show-icon
          :closable="false"
          style="margin-bottom:12px"
        />
        <el-table
          v-if="((importResult.failedRows || importResult.failList) || []).length"
          :data="importResult.failedRows || importResult.failList"
          border
          size="mini"
          max-height="360"
        >
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="idCard" label="证件号码" min-width="180" />
          <el-table-column label="失败原因" min-width="240" show-overflow-tooltip>
            <template slot-scope="{ row }">{{ row.reason || row.error || '-' }}</template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-tip">无失败行</div>
      </div>
      <div slot="footer">
        <el-button @click="importDialog = false">{{ importResult ? '关闭' : '取消' }}</el-button>
        <el-button
          v-if="!importResult"
          type="primary"
          :loading="importLoading"
          :disabled="!currentFile"
          @click="onParse"
        >开始导入</el-button>
      </div>
    </el-dialog>

    <!-- 批量导入照片弹窗 -->
    <el-dialog
      title="批量导入学员照片"
      :visible.sync="batchPhotoDialog.visible"
      width="780px"
      :close-on-click-modal="false"
      @closed="onBatchPhotoDialogClosed"
    >
      <div v-if="!batchPhotoDialog.result">
        <el-alert
          title="提示:支持多张图片同时上传,文件名请使用 18 位身份证号(可带前缀/后缀,例:张三_110101199001011234.jpg)。后端会自动解析身份证号并关联到对应学员。"
          type="info"
          show-icon
          :closable="false"
          style="margin-bottom:12px"
        />
        <el-upload
          ref="batchPhotoUploadRef"
          multiple
          :auto-upload="false"
          accept="image/*"
          :on-change="onBatchPhotoChange"
          :on-remove="onBatchPhotoRemove"
          :file-list="batchPhotoDialog.fileList"
          drag
          action="#"
        >
          <i class="el-icon-upload"></i>
          <div class="el-upload__text">将多张图片拖到此处,或<em>点击上传</em></div>
          <div slot="tip" class="el-upload__tip">仅支持图片文件,每张图不超过 5MB</div>
        </el-upload>
      </div>
      <div v-else>
        <el-alert
          :title="'导入完成:成功 ' + (batchPhotoDialog.result.successCount || 0) + ' 张,失败 ' + (batchPhotoDialog.result.failCount || 0) + ' 张'"
          :type="(batchPhotoDialog.result.failCount || 0) > 0 ? 'warning' : 'success'"
          show-icon
          :closable="false"
          style="margin-bottom:12px"
        />
        <el-table
          v-if="(batchPhotoDialog.result.failedItems || []).length"
          :data="batchPhotoDialog.result.failedItems"
          border
          size="mini"
          max-height="360"
        >
          <el-table-column prop="fileName" label="文件名" min-width="240" />
          <el-table-column prop="reason" label="失败原因" min-width="280" />
        </el-table>
        <div v-else class="empty-tip">无失败项</div>
      </div>
      <div slot="footer">
        <el-button @click="batchPhotoDialog.visible = false">
          {{ batchPhotoDialog.result ? '关闭' : '取消' }}
        </el-button>
        <el-button
          v-if="!batchPhotoDialog.result"
          type="primary"
          :loading="batchPhotoDialog.uploading"
          :disabled="batchPhotoDialog.fileList.length === 0"
          @click="onBatchPhotoSubmit"
        >开始导入</el-button>
      </div>
    </el-dialog>

    <!-- 上传学员照片(从证书用户列表行内触发) -->
    <el-dialog
      title="上传学员照片"
      :visible.sync="photoDialog.visible"
      width="500px"
      @closed="onPhotoDialogClosed"
    >
      <el-form :model="photoDialog.form" label-width="100px" size="small">
        <el-form-item label="身份证">
          <el-input v-model="photoDialog.form.idCard" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="photoDialog.form.name" disabled />
        </el-form-item>
        <el-form-item label="照片">
          <el-upload
            :show-file-list="false"
            :before-upload="beforePhotoUpload"
            :http-request="onPhotoUpload"
            accept="image/*"
            action="#"
          >
            <el-button type="success" icon="el-icon-upload" :disabled="photoDialog.uploading">
              {{ photoDialog.form.url ? '重新上传' : '选择照片' }}
            </el-button>
          </el-upload>
          <el-image
            v-if="photoDialog.form.url"
            :src="resolvePhotoUrl(photoDialog.form.url)"
            style="width:120px;height:120px;margin-top:8px"
            fit="cover"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="photoDialog.visible = false">取消</el-button>
        <!-- 有 certificateId 时走直传(上传后自动关闭),不需要手动保存;仅通用上传分支需要手动保存 -->
        <el-button
          v-if="!photoDialog.form.certificateId"
          type="primary"
          :loading="photoDialog.submitting"
          :disabled="!photoDialog.form.url || photoDialog.form.url === 'uploaded'"
          @click="onPhotoSubmit"
        >保存</el-button>
      </div>
    </el-dialog>

    <!-- 行内"绑定模板"弹窗(为单张证书指定模板;支持一人多证) -->
    <el-dialog title="绑定证书模板" :visible.sync="templateDialog.visible" width="500px">
      <el-form label-width="100px" size="small">
        <el-form-item label="学员">
          <span>{{ templateDialog.form.name }} ({{ templateDialog.form.idCard }})</span>
        </el-form-item>
        <el-form-item label="证书模板">
          <el-select v-model="templateDialog.form.templateId" placeholder="选择模板" clearable style="width: 100%">
            <el-option v-for="t in templateOptions" :key="t.id" :label="(t.isDefault ? '★ ' : '') + t.name" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="templateDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="templateDialog.submitting" @click="onSubmitTemplate">确定绑定</el-button>
      </div>
    </el-dialog>

    <!-- 批量"绑定模板"弹窗 -->
    <el-dialog title="批量绑定证书模板" :visible.sync="batchTemplateDialog.visible" width="500px">
      <el-form label-width="100px" size="small">
        <el-form-item label="选中证书">
          <span>共 {{ selection.length }} 张证书</span>
        </el-form-item>
        <el-form-item label="证书模板">
          <el-select v-model="batchTemplateDialog.templateId" placeholder="选择模板" clearable style="width: 100%">
            <el-option v-for="t in templateOptions" :key="t.id" :label="(t.isDefault ? '★ ' : '') + t.name" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="batchTemplateDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="batchTemplateDialog.submitting" @click="onSubmitBatchTemplate">确定绑定</el-button>
      </div>
    </el-dialog>

    <!-- 下载格式选择弹窗 -->
    <el-dialog :title="downloadFormatDialog.allMode ? '批量下载(全部) - 选择格式' : '批量下载(选中) - 选择格式'" :visible.sync="downloadFormatDialog.visible" width="400px" :close-on-click-modal="false">
      <div v-if="downloadFormatDialog.allMode" class="download-all-tip">
        <i class="el-icon-info"></i> 将下载当前筛选条件下所有已绑定模板的证书,每张证书使用其各自绑定的模板渲染。
      </div>
      <div class="download-format-options">
        <div class="format-option" @click="selectDownloadFormat('image')">
          <i class="el-icon-picture" style="font-size: 32px; color: #1989fa" />
          <div class="format-name">下载为图片</div>
          <div class="format-desc">PNG格式，便于查看和分享</div>
        </div>
        <div class="format-option" @click="selectDownloadFormat('pdf')">
          <i class="el-icon-document" style="font-size: 32px; color: #67c23a" />
          <div class="format-name">下载为PDF</div>
          <div class="format-desc">PDF格式，便于打印和存档</div>
        </div>
      </div>
    </el-dialog>

    <!-- 证书预览弹窗 -->
    <div v-if="certPreview.visible" class="cert-preview-modal" @click.self="onClosePreview">
      <div class="cert-preview-content">
        <div class="cert-preview-header">
          <span>{{ certPreview.certName }}</span>
          <i class="el-icon-close close-btn" @click="onClosePreview" />
        </div>
        <div class="cert-preview-body" @wheel.prevent="onPreviewWheel">
          <img v-if="certPreview.url" :src="certPreview.url" alt="证书预览" @error="onPreviewError"
               :style="previewImgStyle" @mousedown="onPreviewMouseDown" />
          <div v-if="certPreview.url" class="zoom-controls">
            <span class="zoom-btn" @click="onZoomIn" title="放大">+</span>
            <span class="zoom-info">{{ Math.round(previewScale * 100) }}%</span>
            <span class="zoom-btn" @click="onZoomOut" title="缩小">−</span>
            <span class="zoom-btn" @click="onZoomReset" title="重置">↺</span>
          </div>
          <div v-if="certPreview.error" class="preview-error">
            <i class="el-icon-warning" style="font-size: 48px; color: #c0c4cc" />
            <p>证书预览加载失败</p>
          </div>
          <div v-if="certPreview.loading" class="preview-loading">
            <i class="el-icon-loading" style="font-size: 48px; color: #1989fa" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  certificatePage,
  importCertificate,
  importPhotoBatch,
  deleteCertificate,
  switchExamQr,
  downloadFile,
  triggerDownload,
  downloadCertificateBatch,
  certificateAllIds,
  downloadSingleCertificate,
  exportCertificates,
  addPhoto,
  uploadPhotoForCertificate,
  issueCertificates,
  certificateUserSync
} from '@/api/certificate'
import { uploadFile as uploadRequest } from '@/api/upload'
import { apiUrl } from '@/utils/apiBase'
import { templateList } from '@/api/certificateTemplate'

export default {
  name: 'CertificateList',
  data() {
    return {
      loading: false,
      query: { page: 1, size: 10, name: '', idCard: '', agency: '', profession: '', issueDateStart: '', issueDateEnd: '' },
      dateRange: null,
      list: [],
      total: 0,
      selection: [],
      batchDownloading: null,
      exporting: false,
      syncingUsers: false,
      unboundTemplateFilter: false,
      importDialog: false,
      importLoading: false,
      currentFile: null,
      fileList: [],
      importResult: null,
      // 批量导入照片
      batchPhotoDialog: {
        visible: false,
        fileList: [],
        uploading: false,
        result: null
      },
      // 上传学员照片(行内触发,身份证+姓名从当前行预填)
      photoDialog: {
        visible: false,
        submitting: false,
        uploading: false,
        form: { idCard: '', name: '', url: '' }
      },
      // 证书模板选项(行内"绑定模板"弹窗用)
      templateOptions: [],
      // 行内"绑定模板"弹窗(支持一人多证,为单张证书指定/更换模板)
      templateDialog: {
        visible: false,
        submitting: false,
        form: { id: null, name: '', idCard: '', templateId: null }
      },
      // 批量绑定模板弹窗
      batchTemplateDialog: {
        visible: false,
        submitting: false,
        templateId: null
      },
      // 下载格式选择弹窗
      downloadFormatDialog: {
        visible: false,
        allMode: false
      },
      // 证书预览弹窗
      certPreview: {
        visible: false,
        url: '',
        certName: '',
        loading: false,
        error: false
      },
      // 证书预览缩放
      previewScale: 1,
      previewDragging: false,
      previewDragStart: { x: 0, y: 0 },
      previewTranslate: { x: 0, y: 0 }
    }
  },
  computed: {
    currentCertType() {
      // 从路由参数获取证书类型名称
      const idx = this.$route.params.idx
      if (idx) {
        const certTypes = this.$store.getters.certTypes || []
        const t = certTypes[parseInt(idx) - 1]
        return t ? (t.name || t) : ''
      }
      return this.$route.meta.certType || ''
    },
    previewImgStyle() {
      return {
        transform: 'scale(' + this.previewScale + ') translate(' + this.previewTranslate.x + 'px, ' + this.previewTranslate.y + 'px)',
        cursor: this.previewScale > 1 ? (this.previewDragging ? 'grabbing' : 'grab') : 'default',
        transition: this.previewDragging ? 'none' : 'transform 0.15s ease'
      }
    }
  },
  mounted() {
    this.loadList()
    // 加载证书模板选项(行内"绑定模板"弹窗用)
    templateList().then(r => { this.templateOptions = r.data || [] }).catch(() => {})
  },
  watch: {
    '$route.params.idx'() {
      this.query = { page: 1, size: 10, name: '', idCard: '', agency: '', profession: '', issueDateStart: '', issueDateEnd: '' }
      this.dateRange = null
      this.loadList()
    },
    '$route.meta.certType'() {
      this.query = { page: 1, size: 10, name: '', idCard: '', agency: '', profession: '', issueDateStart: '', issueDateEnd: '' }
      this.dateRange = null
      this.loadList()
    }
  },
  methods: {
    apiUrl,
    // 身份证号 18 位校验(含校验位算法 GB 11643)
    validateIdCard(idCard) {
      if (!idCard) return true
      if (!/^[1-9]\d{5}(?:18|19|20)\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/.test(idCard)) {
        return false
      }
      var weight = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
      var checkCodes = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']
      var sum = 0
      for (var i = 0; i < 17; i++) {
        sum += parseInt(idCard.charAt(i)) * weight[i]
      }
      var expectedCheck = checkCodes[sum % 11]
      return expectedCheck === idCard.charAt(17).toUpperCase()
    },
    // 从身份证号提取出生日期(yyyy-MM-dd)
    getBirthdayFromIdCard(idCard) {
      if (!idCard) return ''
      // 18位身份证: 第7-14位为出生日期
      if (idCard.length === 18) {
        var year = idCard.substring(6, 10)
        var month = idCard.substring(10, 12)
        var day = idCard.substring(12, 14)
        return year + '-' + month + '-' + day
      }
      // 15位身份证: 第7-12位为出生日期(年份补19)
      if (idCard.length === 15) {
        var y = '19' + idCard.substring(6, 8)
        var m = idCard.substring(8, 10)
        var d = idCard.substring(10, 12)
        return y + '-' + m + '-' + d
      }
      return ''
    },
    formatDate(d) {
      if (!d) return ''
      if (typeof d === 'string') return d.substring(0, 10)
      return new Date(d).toISOString().substring(0, 10)
    },
    // 颁发日期格式化:统一显示为 yyyy年MM月dd日
    formatIssueDate(dateStr, rawDate) {
      // 优先用后端返回的中文格式
      if (dateStr && dateStr.includes('年')) return dateStr
      // 尝试解析原始日期(支持 yyyy-MM-dd, yyyy/MM/dd, yyyy.MM.dd)
      if (rawDate) {
        var s = String(rawDate).replace(/[\/.]/g, '-').substring(0, 10)
        var parts = s.split('-')
        if (parts.length === 3) {
          var y = parts[0]
          var m = parts[1].padStart(2, '0')
          var d2 = parts[2].padStart(2, '0')
          return y + '年' + m + '月' + d2 + '日'
        }
        return rawDate
      }
      if (dateStr) return dateStr
      return '--'
    },
    formatDateTime(d) {
      if (!d) return ''
      // 支持 "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd'T'HH:mm:ss.SSS" / Date
      if (typeof d === 'string') {
        // 去掉毫秒与时区
        const idx = d.indexOf('T')
        if (idx > 0) {
          // ISO 8601 形式
          return d.substring(0, 19).replace('T', ' ')
        }
        return d.substring(0, 19)
      }
      const dt = new Date(d)
      const pad = n => (n < 10 ? '0' + n : '' + n)
      return (
        dt.getFullYear() + '-' +
        pad(dt.getMonth() + 1) + '-' +
        pad(dt.getDate()) + ' ' +
        pad(dt.getHours()) + ':' +
        pad(dt.getMinutes()) + ':' +
        pad(dt.getSeconds())
      )
    },
    onSearch() {
      this.query.page = 1
      this.loadList()
    },
    onReset() {
      this.query = { page: 1, size: 10, name: '', idCard: '', agency: '', profession: '', issueDateStart: '', issueDateEnd: '' }
      this.dateRange = null
      this.loadList()
    },
    onSyncFromStudents() {
      const certType = this.currentCertType
      const msg = certType
        ? `确定要从「学生管理」同步「${certType}」类型的学员数据到证书用户吗？\n（按专业维度，每个专业创建一条记录；已删除的记录会自动恢复）`
        : '确定要从「学生管理」同步学员数据到证书用户吗？\n（按专业维度，每个专业创建一条记录；已删除的记录会自动恢复）'
      this.$confirm(
        msg,
        '同步确认',
        { type: 'warning', confirmButtonText: '确定同步', cancelButtonText: '取消' }
      ).then(() => {
        this.syncingUsers = true
        certificateUserSync(certType)
          .then((res) => {
            const data = (res && res.data) || {}
            const synced = data.synced != null ? data.synced : 0
            const created = data.created != null ? data.created : 0
            this.$message.success('同步完成，共同步 ' + synced + ' 名学员，新建 ' + created + ' 条证书记录')
            this.loadList()
          })
          .catch(() => {
            this.$message.error('同步失败，请稍后重试')
          })
          .finally(() => {
            this.syncingUsers = false
          })
      }).catch(() => {})
    },
    async loadList() {
      if (this.dateRange && this.dateRange.length === 2) {
        this.query.issueDateStart = this.dateRange[0]
        this.query.issueDateEnd = this.dateRange[1]
      } else {
        this.query.issueDateStart = ''
        this.query.issueDateEnd = ''
      }
      // 未绑定模板筛选
      if (this.unboundTemplateFilter) {
        this.query.unboundTemplate = 1
      } else {
        this.query.unboundTemplate = undefined
      }
      this.loading = true
      try {
        const params = { ...this.query }
        if (this.currentCertType) {
          params.certType = this.currentCertType
        }
        const res = await certificatePage(params)
        this.list = res.data.records || []
        this.total = res.data.total || 0
      } finally {
        this.loading = false
      }
    },
    toggleUnboundTemplate() {
      this.unboundTemplateFilter = !this.unboundTemplateFilter
      this.query.page = 1
      this.loadList()
    },
    onDownloadTemplate() {
      // 用 fetch + blob 方式下载,自动带 JWT
      this.$message.info('正在下载模板...')
      downloadFile('/admin/certificate/template')
        .then(({ blob, fileName }) => triggerDownload(blob, fileName || '证书导入模板.xlsx'))
        .catch(err => this.$message.error('下载模板失败: ' + (err.message || '未知错误')))
    },
    openDownloadFormatDialog() {
      this.downloadFormatDialog.allMode = false
      this.downloadFormatDialog.visible = true
    },
    openDownloadAllFormatDialog() {
      this.downloadFormatDialog.allMode = true
      this.downloadFormatDialog.visible = true
    },
    selectDownloadFormat(format) {
      this.downloadFormatDialog.visible = false
      if (this.downloadFormatDialog.allMode) {
        this.onDownloadAllBatch(format)
      } else {
        this.onDownloadBatch(format)
      }
    },
    // 批量下载证书(图片/PDF): 一个人可有多张证书,选中多条打包下载
    // 使用每张证书各自绑定的模板渲染(不会更换模板)
    async onDownloadBatch(format) {
      const ids = this.selection.map(s => s.id)
      if (!ids.length) {
        this.$message.warning('请先勾选要下载的证书')
        return
      }
      this.batchDownloading = format
      try {
        const res = await downloadCertificateBatch(ids, format)
        if (res && res.async) {
          this.$message.success(res.message || '已提交批量生成任务,请在右下角"任务中心"查看进度')
        } else if (res && res.blob) {
          triggerDownload(res.blob, res.fileName || ('certificates_' + format + '.zip'))
          this.$message.success('证书已打包下载')
        } else {
          this.$message.warning('未返回可下载的内容')
        }
      } catch (e) {
        this.$message.error('批量下载失败: ' + (e.message || '未知错误'))
      } finally {
        this.batchDownloading = null
      }
    },
    // 批量下载全部证书(按当前筛选条件): 先拉取所有已绑定模板的证书 ID,再走批量下载
    async onDownloadAllBatch(format) {
      this.batchDownloading = 'all-' + format
      try {
        const params = {
          name: this.query.name,
          idCard: this.query.idCard,
          agency: this.query.agency,
          profession: this.query.profession,
          issueDateStart: this.query.issueDateStart,
          issueDateEnd: this.query.issueDateEnd
        }
        if (this.currentCertType) {
          params.certType = this.currentCertType
        }
        const idRes = await certificateAllIds(params)
        const ids = (idRes && idRes.data) || []
        if (!ids.length) {
          this.$message.warning('当前筛选条件下没有可下载的证书(仅已绑定模板的证书可下载)')
          return
        }
        const res = await downloadCertificateBatch(ids, format)
        if (res && res.async) {
          this.$message.success(res.message || ('已提交批量生成任务(共 ' + ids.length + ' 张),请在右下角"任务中心"查看进度'))
        } else if (res && res.blob) {
          triggerDownload(res.blob, res.fileName || ('certificates_all_' + format + '.zip'))
          this.$message.success('证书已打包下载(共 ' + ids.length + ' 张)')
        } else {
          this.$message.warning('未返回可下载的内容')
        }
      } catch (e) {
        this.$message.error('批量下载失败: ' + (e.message || '未知错误'))
      } finally {
        this.batchDownloading = null
      }
    },
    async onSwitchExamQr(enabled) {
      const ids = this.selection.map(s => s.id)
      try {
        const res = await switchExamQr({ ids, enabled })
        if (res.data && res.data.async) {
          this.$message.success('已提交批量操作,请在右下角"任务中心"查看进度')
          return
        }
        this.$message.success('操作成功')
        this.loadList()
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    // 导出选中数据(Excel,与导入模板相同的20列结构)
    async onExportSelected() {
      if (this.selection.length === 0) return
      const ids = this.selection.map(s => s.id)
      this.exporting = true
      try {
        const { blob, fileName } = await exportCertificates({ ids })
        triggerDownload(blob, fileName)
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败: ' + (e.message || '未知错误'))
      } finally {
        this.exporting = false
      }
    },
    // 导出全部数据(按当前筛选条件)
    async onExportAll() {
      this.exporting = true
      try {
        const params = {
          name: this.query.name,
          idCard: this.query.idCard,
          agency: this.query.agency,
          profession: this.query.profession,
          issueDateStart: this.query.issueDateStart,
          issueDateEnd: this.query.issueDateEnd
        }
        const { blob, fileName } = await exportCertificates(params)
        triggerDownload(blob, fileName)
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败: ' + (e.message || '未知错误'))
      } finally {
        this.exporting = false
      }
    },
    // 单张证书下载(图片/PDF)
    async onDownloadSingle(row, format) {
      if (!row.templateId) {
        this.$message.warning('该证书未绑定模板，请先绑定模板后再下载')
        return
      }
      this.$set(row, '_downloading', true)
      try {
        const { fileName, blob } = await downloadSingleCertificate(row.id, format)
        triggerDownload(blob, fileName)
        this.$message.success('证书下载成功')
      } catch (e) {
        this.$message.error((e && e.message) || '下载失败')
      } finally {
        this.$set(row, '_downloading', false)
      }
    },
    onDeleteOne(id) {
      this.$confirm(
        '确定删除该证书吗?\n证书数据删除后无法恢复,已生成的证书图片/PDF不会自动删除。',
        '删除确认',
        { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
      ).then(() => {
        return deleteCertificate([id])
      }).then(() => {
        this.$message.success('删除成功')
        this.loadList()
      }).catch(err => {
        if (err && err !== 'cancel' && err !== 'close') {
          this.$message.error('删除失败')
        }
      })
    },
    // ===== 学员照片上传(行内触发) =====
    onOpenPhotoUpload(row) {
      // 身份证/姓名从当前行预填,只读;同时保存 certificateId 用于关联
      this.photoDialog.form = { idCard: row.idCard || '', name: row.name || '', url: '', certificateId: row.id || null }
      // 预加载已有照片
      if (row.photoUrl) {
        this.photoDialog.form.url = row.photoUrl
      }
      this.photoDialog.visible = true
    },
    beforePhotoUpload(file) {
      const isImg = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isImg) { this.$message.error('只能上传图片'); return false }
      if (!isLt5M) { this.$message.error('图片大小不能超过 5MB'); return false }
      return true
    },
    async onPhotoUpload({ file }) {
      this.photoDialog.uploading = true
      try {
        const fd = new FormData()
        fd.append('file', file)
        // 直接调用 upload-for-certificate 接口,把文件和 certificateId 一起提交
        if (this.photoDialog.form.certificateId) {
          fd.append('certificateId', this.photoDialog.form.certificateId)
          fd.append('idCard', this.photoDialog.form.idCard)
          fd.append('name', this.photoDialog.form.name)
          await uploadPhotoForCertificate(fd)
          this.photoDialog.form.url = 'uploaded'
          this.$message.success('照片已保存')
          this.photoDialog.visible = false
          this.loadList()
        } else {
          // 没有 certificateId 时走通用上传
          const res = await uploadRequest(fd)
          this.photoDialog.form.url = res.data
          this.$message.success('照片已上传,请点击"保存"完成绑定')
        }
      } catch (e) {
        this.$message.error('上传失败: ' + (e.message || '未知错误'))
      } finally {
        this.photoDialog.uploading = false
      }
    },
    async onPhotoSubmit() {
      const f = this.photoDialog.form
      if (!f.idCard) { this.$message.warning('身份证号为空,无法保存'); return }
      if (!f.url) { this.$message.warning('请先上传照片'); return }
      this.photoDialog.submitting = true
      try {
        await addPhoto({ idCard: f.idCard, name: f.name, url: f.url, certificateId: f.certificateId || null })
        this.$message.success('学员照片已保存')
        this.photoDialog.visible = false
      } catch (e) {
        this.$message.error('保存失败: ' + (e.message || '未知错误'))
      } finally {
        this.photoDialog.submitting = false
      }
    },
    onPhotoDialogClosed() {
      this.photoDialog.form = { idCard: '', name: '', url: '', certificateId: null }
      this.photoDialog.uploading = false
    },
    resolvePhotoUrl(u) {
      if (!u) return ''
      if (u.startsWith('http')) return u
      // /uploads/** 后端已排除 JWT 鉴权
      return apiUrl(u)
    },
    // ===== 行内"绑定模板"(支持一人多证,为单张证书指定/更换模板) =====
    onOpenTemplateDialog(row) {
      // 预填学员信息与当前已绑定模板
      this.templateDialog.form = {
        id: row.id,
        name: row.name || '',
        idCard: row.idCard || '',
        templateId: row.templateId || null
      }
      this.templateDialog.visible = true
    },
    async onSubmitTemplate() {
      this.templateDialog.submitting = true
      try {
        await issueCertificates({
          certificateIds: [this.templateDialog.form.id],
          templateId: this.templateDialog.form.templateId
        })
        this.$message.success('模板绑定成功')
        this.templateDialog.visible = false
        this.loadList()
      } catch (e) {
        this.$message.error('绑定失败: ' + (e.message || '未知错误'))
      } finally {
        this.templateDialog.submitting = false
      }
    },
    // ====== 批量绑定模板 ======
    openBatchTemplateDialog() {
      if (this.selection.length === 0) {
        this.$message.warning('请先选择证书')
        return
      }
      this.batchTemplateDialog.templateId = null
      this.batchTemplateDialog.visible = true
    },
    async onSubmitBatchTemplate() {
      if (!this.batchTemplateDialog.templateId) {
        this.$message.warning('请选择模板')
        return
      }
      this.batchTemplateDialog.submitting = true
      try {
        const ids = this.selection.map(c => c.id)
        await issueCertificates({
          certificateIds: ids,
          templateId: this.batchTemplateDialog.templateId
        })
        this.$message.success('批量绑定模板成功')
        this.batchTemplateDialog.visible = false
        this.loadList()
      } catch (e) {
        this.$message.error('批量绑定失败: ' + (e.message || '未知错误'))
      } finally {
        this.batchTemplateDialog.submitting = false
      }
    },
    async onPreviewCert(row) {
      if (!row.templateId) {
        this.$message.warning('该证书未绑定模板，请先绑定模板后再预览')
        return
      }
      this.certPreview = {
        visible: true,
        url: '',
        certName: row.name + ' - ' + (row.certNo || row.id),
        loading: true,
        error: false
      }
      try {
        var fullUrl = apiUrl('/admin/certificate/generate/single/' + row.id + '?format=image')
        var token = localStorage.getItem('admin_token') || ''
        if (!token) {
          var raw = localStorage.getItem('vuex') || sessionStorage.getItem('vuex') || ''
          if (raw) {
            var state = JSON.parse(raw)
            token = state.token || (state.auth && state.auth.token) || ''
          }
        }
        var headers = { Accept: '*/*' }
        if (token) headers.Authorization = 'Bearer ' + token
        var res = await fetch(fullUrl, { headers })
        if (!res.ok) {
          // 尝试读取后端返回的错误信息
          var errText = '请求失败(HTTP ' + res.status + ')'
          try {
            var errData = await res.clone().json()
            if (errData && errData.message) errText = errData.message
          } catch (_) {
            try { errText = await res.clone().text() } catch (_) {}
          }
          throw new Error(errText)
        }
        var blob = await res.blob()
        if (blob.size < 100) {
          throw new Error('生成的图片数据异常,可能是模板配置不完整')
        }
        this.certPreview.url = URL.createObjectURL(blob)
      } catch (e) {
        this.certPreview.error = true
        this.$message.error('预览失败: ' + (e.message || '未知错误'))
      } finally {
        this.certPreview.loading = false
      }
    },
    onPreviewError() {
      this.certPreview.error = true
    },
    onClosePreview() {
      if (this.certPreview.url && this.certPreview.url.startsWith('blob:')) {
        URL.revokeObjectURL(this.certPreview.url)
      }
      this.certPreview.visible = false
      this.certPreview.url = ''
      this.certPreview.loading = false
      this.certPreview.error = false
      this.previewScale = 1
      this.previewTranslate = { x: 0, y: 0 }
    },
    // ===== 证书预览缩放 =====
    onPreviewWheel(e) {
      const delta = e.deltaY > 0 ? -0.1 : 0.1
      this.previewScale = Math.max(0.2, Math.min(5, this.previewScale + delta))
    },
    onZoomIn() { this.previewScale = Math.min(5, this.previewScale + 0.2) },
    onZoomOut() { this.previewScale = Math.max(0.2, this.previewScale - 0.2) },
    onZoomReset() {
      this.previewScale = 1
      this.previewTranslate = { x: 0, y: 0 }
    },
    onPreviewMouseDown(e) {
      if (this.previewScale <= 1) return
      this.previewDragging = true
      this.previewDragStart = { x: e.clientX - this.previewTranslate.x, y: e.clientY - this.previewTranslate.y }
      document.addEventListener('mousemove', this._onPreviewMouseMove)
      document.addEventListener('mouseup', this._onPreviewMouseUp)
    },
    _onPreviewMouseMove(e) {
      if (!this.previewDragging) return
      this.previewTranslate = {
        x: e.clientX - this.previewDragStart.x,
        y: e.clientY - this.previewDragStart.y
      }
    },
    _onPreviewMouseUp() {
      this.previewDragging = false
      document.removeEventListener('mousemove', this._onPreviewMouseMove)
      document.removeEventListener('mouseup', this._onPreviewMouseUp)
    },
    onDelete() {
      const ids = this.selection.map(s => s.id)
      this.$confirm(
        '确定删除选中的 ' + ids.length + ' 条证书吗?\n证书数据删除后无法恢复。',
        '删除确认',
        { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
      ).then(() => {
        return deleteCertificate(ids)
      }).then(() => {
        this.$message.success('删除成功')
        this.loadList()
      }).catch(err => {
        if (err && err !== 'cancel' && err !== 'close') {
          this.$message.error('删除失败')
        }
      })
    },
    onFileChange(file) {
      this.currentFile = file.raw
    },
    onExceed() {
      this.$message.warning('只允许上传一个文件')
    },
    onImportClosed() {
      this.currentFile = null
      this.fileList = []
      this.importResult = null
    },
    async onParse() {
      if (!this.currentFile) {
        this.$message.warning('请先选择文件')
        return
      }
      const form = new FormData()
      form.append('file', this.currentFile)
      this.importLoading = true
      try {
        const res = await importCertificate(form)
        const data = res.data || {}
        // 1) 异步路径: 行数 >= 50 走异步入库
        if (data.async) {
          this.$alert(
            '已提交异步入库任务,请在右下角"任务中心"查看进度。\n' +
              '任务完成后,可前往"证书用户"列表查看结果;失败行可在任务中心下载。',
            '已提交入库',
            { type: 'success' }
          )
          this.importDialog = false
          this.onImportClosed()
          return
        }
        // 2) 同步路径: 一次性展示结果(成功/失败明细)
        this.importResult = data
        const ok = data.successCount || 0
        const fail = data.failCount || 0
        const failedRows = data.failedRows || data.failList || []
        if (fail > 0 && ok === 0) {
          this.$alert(
            '全部 ' + fail + ' 条数据未通过校验,请查看下方"失败明细"修正 Excel 后重传。',
            '校验未通过',
            { type: 'error' }
          )
        } else if (fail > 0) {
          this.$alert(
            '成功 ' + ok + ' 条,失败 ' + fail + ' 条\n请查看下方"失败明细"修正后重新上传。',
            '导入完成(有失败)',
            { type: 'warning' }
          )
        } else if (ok > 0) {
          this.$message.success('导入成功 ' + ok + ' 条')
          this.importDialog = false
          this.loadList()
        }
      } catch (err) {
        // axios 拦截器已弹 Message,这里把错误信息展示在导入结果区域
        const msg = (err && err.message) || '导入失败,请检查文件格式或网络后重试'
        this.importResult = {
          successCount: 0,
          failCount: 1,
          failedRows: [{ name: '-', idCard: '-', error: msg }]
        }
      } finally {
        this.importLoading = false
      }
    },
    // ===== 批量导入照片 =====
    openBatchPhotoDialog() {
      this.batchPhotoDialog.visible = true
    },
    onBatchPhotoChange(file, fileList) {
      if (file.raw) {
        const isImg = file.raw.type.startsWith('image/')
        const isLt5M = file.raw.size / 1024 / 1024 < 5
        if (!isImg) {
          this.$message.error('只能上传图片: ' + file.name)
          return false
        }
        if (!isLt5M) {
          this.$message.error('图片不能超过 5MB: ' + file.name)
          return false
        }
      }
      this.batchPhotoDialog.fileList = fileList
    },
    onBatchPhotoRemove(file, fileList) {
      this.batchPhotoDialog.fileList = fileList
    },
    async onBatchPhotoSubmit() {
      const files = this.batchPhotoDialog.fileList
        .map(f => f.raw)
        .filter(Boolean)
      if (files.length === 0) {
        this.$message.warning('请先选择要上传的图片')
        return
      }
      const form = new FormData()
      for (const f of files) {
        form.append('files', f)
      }
      this.batchPhotoDialog.uploading = true
      try {
        const res = await importPhotoBatch(form)
        this.batchPhotoDialog.result = res.data || { successCount: 0, failCount: 0, failedItems: [] }
        // 提示语
        const ok = this.batchPhotoDialog.result.successCount || 0
        const fail = this.batchPhotoDialog.result.failCount || 0
        if (fail > 0) {
          this.$message.warning('导入完成:成功 ' + ok + ' 张,失败 ' + fail + ' 张,请查看失败明细')
        } else if (ok > 0) {
          this.$message.success('导入成功 ' + ok + ' 张')
        }
      } catch (e) {
        this.$message.error('导入失败: ' + (e.message || '未知错误'))
      } finally {
        this.batchPhotoDialog.uploading = false
      }
    },
    onBatchPhotoDialogClosed() {
      this.batchPhotoDialog.fileList = []
      this.batchPhotoDialog.uploading = false
      this.batchPhotoDialog.result = null
    },
  }
}
</script>

<style scoped>
.filter-form { margin-bottom: 12px; }
.toolbar { margin-bottom: 12px; }
.toolbar .el-button { margin-right: 8px; }
.pagination { margin-top: 16px; text-align: right; }
.async-hint { margin-top: 12px; }
.empty-tip { padding: 24px; text-align: center; color: #909399; }
.action-row {
  display: flex;
  gap: 4px;
  margin-bottom: 4px;
}

.cert-preview-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.cert-preview-content {
  position: relative;
  max-width: 95vw;
  max-height: 95vh;
  display: flex;
  flex-direction: column;
}
.cert-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  font-size: 14px;
  padding: 8px 12px;
  margin-bottom: 8px;
}
.cert-preview-header .close-btn {
  cursor: pointer;
}
.cert-preview-body {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  max-height: 85vh;
}
.cert-preview-body img {
  max-width: 90vw;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 4px;
}
.zoom-controls {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(0, 0, 0, 0.6);
  padding: 6px 16px;
  border-radius: 20px;
  z-index: 10001;
}
.zoom-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  user-select: none;
  &:hover { background: rgba(255, 255, 255, 0.3); }
}
.zoom-info {
  color: #fff;
  font-size: 13px;
  min-width: 40px;
  text-align: center;
  user-select: none;
}
.preview-error, .preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #909399;
  padding: 60px;
}
.preview-error p, .preview-loading p {
  font-size: 14px;
}

.download-format-options {
  display: flex;
  gap: 10px;
  justify-content: center;
  padding: 15px 0;
}
.download-all-tip {
  background: #ecf5ff;
  color: #409eff;
  padding: 10px 12px;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 8px;
  i {
    margin-right: 4px;
  }
}

.format-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 20px;
  border: 2px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  min-width: 120px;
  &:hover {
    border-color: #1989fa;
    background: #f5f7fa;
  }
}

.format-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-top: 12px;
}

.format-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.id-card-invalid {
  background-color: #fef0f0;
  color: #f56c6c;
  padding: 2px 6px;
  border-radius: 3px;
}
</style>
