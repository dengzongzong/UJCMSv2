<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="filter-container">
        <el-select
          v-model="query.professionId"
          placeholder="专业"
          clearable
          filterable
          class="filter-item"
          style="width: 180px"
        >
          <el-option
            v-for="item in professionOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="关键词(姓名/手机号/学号/身份证)"
          clearable
          class="filter-item"
          style="width: 240px"
          @keyup.enter.native="handleSearch"
        />
        <el-date-picker
          v-model="query.dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="注册开始日期"
          end-placeholder="注册结束日期"
          value-format="yyyy-MM-dd HH:mm:ss"
          class="filter-item"
          style="width: 380px"
        />
        <el-select
          v-model="query.status"
          placeholder="状态"
          clearable
          class="filter-item"
          style="width: 140px"
        >
          <el-option label="正常" :value="1" />
          <el-option label="冻结" :value="0" />
        </el-select>
        <el-input-number
          v-model="query.exactCount"
          :min="1"
          :max="10000"
          placeholder="显示最新N条"
          controls-position="right"
          class="filter-item"
          style="width: 160px"
        />
        <el-button type="primary" icon="el-icon-search" class="filter-item" @click="handleSearch">
          搜索
        </el-button>
        <el-button icon="el-icon-refresh" class="filter-item" @click="handleReset">重置</el-button>
        <el-button
          type="success"
          icon="el-icon-upload2"
          class="filter-item"
          style="float: right"
          @click="openImportDialog"
        >
          批量导入
        </el-button>
        <el-button
          type="primary"
          icon="el-icon-plus"
          class="filter-item"
          style="float: right"
          @click="openAddDialog"
        >
          新增学生
        </el-button>
        <el-button
          type="danger"
          icon="el-icon-delete"
          class="filter-item"
          style="float: right"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >
          批量删除({{ selection.length }})
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%" @selection-change="rows => (selection = rows)">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="realName" label="姓名" min-width="120" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.realName || row.name || '-' }}</template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学号" min-width="140" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.studentNo || row.studentNumber || row.studentId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="idCard" label="身份证号" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <span v-if="row.idCard" class="id-card" :class="{ 'id-card-invalid': !validateIdCard(row.idCard) }">{{ maskIdCard(row.idCard) }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="certType" label="证书类型" min-width="140" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <span v-if="row.certType">{{ row.certType }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="专业" min-width="160" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <template v-if="row.professionNames && row.professionNames.length">
              <el-tag v-for="(name, idx) in row.professionNames" :key="idx" size="mini" style="margin-right: 4px; margin-bottom: 2px;">{{ name }}</el-tag>
            </template>
            <span v-else-if="row.professionName">{{ row.professionName }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="registerTime" label="注册时间" width="170" align="center" />
        <el-table-column prop="lastLoginTime" label="上次登录时间" width="170" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ row.status === 1 ? '正常' : '冻结' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="460" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button
              type="text"
              :icon="row.status === 1 ? 'el-icon-lock' : 'el-icon-unlock'"
              @click="handleFreeze(row)"
            >
              {{ row.status === 1 ? '冻结' : '解冻' }}
            </el-button>
            <el-button type="text" icon="el-icon-reading" @click="handleOpenCourse(row)">
              开通课程
            </el-button>
            <el-button type="text" icon="el-icon-document" @click="handleOpenExam(row)">
              开通考试
            </el-button>
            <el-button type="text" icon="el-icon-delete" class="danger-text" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          :current-page="query.page"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="query.size"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 开通课程弹窗 -->
    <el-dialog
      title="开通课程"
      :visible.sync="courseDialog.visible"
      width="720px"
      :close-on-click-modal="false"
    >
      <div v-loading="courseDialog.loading">
        <el-alert
          :title="`当前学生：${courseDialog.studentPhone || ''}`"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 12px"
        />
        <el-tabs v-model="courseDialog.activeTab">
          <el-tab-pane label="未开通课程" name="unopened">
            <el-checkbox-group v-model="courseDialog.selected">
              <el-table
                :data="courseDialog.unopened"
                border
                size="mini"
                max-height="360"
                @selection-change="handleCourseSelectionChange"
              >
                <el-table-column type="selection" width="50" align="center" />
                <el-table-column prop="name" label="课程名称" min-width="180" show-overflow-tooltip />
                <el-table-column prop="price" label="价格" width="100" align="center">
                  <template slot-scope="{ row }">¥{{ row.price || 0 }}</template>
                </el-table-column>
                <el-table-column prop="sectionCount" label="小节数" width="90" align="center" />
              </el-table>
            </el-checkbox-group>
          </el-tab-pane>
          <el-tab-pane :label="`已开通课程(${courseDialog.opened.length})`" name="opened">
            <el-table :data="courseDialog.opened" border size="mini" max-height="360">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="name" label="课程名称" min-width="180" show-overflow-tooltip />
              <el-table-column prop="openTime" label="开通时间" width="170" align="center" />
              <el-table-column label="操作" width="100" align="center">
                <template slot-scope="{ row }">
                  <el-button
                    type="text"
                    icon="el-icon-close"
                    class="danger-text"
                    @click="handleCloseCourse(row)"
                  >
                    取消开通
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
      <div slot="footer">
        <el-button @click="courseDialog.visible = false">取 消</el-button>
        <el-button
          type="primary"
          :loading="courseDialog.submitting"
          :disabled="courseDialog.selected.length === 0"
          @click="submitOpenCourse"
        >
          确认开通({{ courseDialog.selected.length }})
        </el-button>
      </div>
    </el-dialog>

    <!-- 开通考试弹窗 -->
    <el-dialog
      title="开通考试"
      :visible.sync="examDialog.visible"
      width="720px"
      :close-on-click-modal="false"
    >
      <div v-loading="examDialog.loading">
        <el-alert
          :title="`当前学生：${examDialog.studentPhone || ''}`"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 12px"
        />
        <el-tabs v-model="examDialog.activeTab">
          <el-tab-pane label="未开通考试" name="unopened">
            <el-table
              :data="examDialog.unopened"
              border
              size="mini"
              max-height="360"
              @selection-change="handleExamSelectionChange"
            >
              <el-table-column type="selection" width="50" align="center" />
              <el-table-column prop="name" label="考试名称" min-width="180" show-overflow-tooltip />
              <el-table-column prop="totalScore" label="总分" width="90" align="center" />
              <el-table-column prop="duration" label="时长(分)" width="100" align="center" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`已开通考试(${examDialog.opened.length})`" name="opened">
            <el-table :data="examDialog.opened" border size="mini" max-height="360">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="name" label="考试名称" min-width="180" show-overflow-tooltip />
              <el-table-column prop="openTime" label="开通时间" width="170" align="center" />
              <el-table-column label="操作" width="100" align="center">
                <template slot-scope="{ row }">
                  <el-button
                    type="text"
                    icon="el-icon-close"
                    class="danger-text"
                    @click="handleCloseExam(row)"
                  >
                    取消开通
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
      <div slot="footer">
        <el-button @click="examDialog.visible = false">取 消</el-button>
        <el-button
          type="primary"
          :loading="examDialog.submitting"
          :disabled="examDialog.selected.length === 0"
          @click="submitOpenExam"
        >
          确认开通({{ examDialog.selected.length }})
        </el-button>
      </div>
    </el-dialog>

    <!-- 新增学生弹窗 -->
    <el-dialog
      title="新增学生"
      :visible.sync="addDialog.visible"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="addForm" :model="addDialog.form" :rules="addDialog.rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="addDialog.form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="addDialog.form.studentNo" placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="addDialog.form.phone" placeholder="选填" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="addDialog.form.password" placeholder="默认123456" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="专业" prop="professionIds">
              <el-select
                v-model="addDialog.form.professionIds"
                placeholder="请选择或输入新专业"
                multiple
                filterable
                allow-create
                default-first-option
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="item in professionOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input
                v-model="addDialog.form.idCard"
                placeholder="请输入身份证号"
                @blur="onIdCardBlur('add')"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="昵称">
              <el-input v-model="addDialog.form.nickname" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证书类型" prop="certType">
              <el-select v-model="addDialog.form.certType" placeholder="请选择证书类型" filterable style="width: 100%">
                <el-option v-for="item in certTypeOptions" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer">
        <el-button @click="addDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="addDialog.submitting" @click="submitAdd">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog
      title="批量导入学生"
      :visible.sync="importDialog.visible"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-alert
        :title="importTipTitle"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <div style="margin-bottom: 12px">
        <el-button type="text" icon="el-icon-download" @click="handleDownloadTemplate">
          下载导入模板
        </el-button>
      </div>
      <el-upload
        ref="importUpload"
        :show-file-list="true"
        :limit="1"
        :auto-upload="false"
        :on-change="handleImportChange"
        :on-remove="handleImportRemove"
        accept=".xlsx,.xls"
        drag
        action="#"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">仅支持 .xlsx / .xls 文件</div>
      </el-upload>

      <!-- 导入结果 -->
      <div v-if="importDialog.result" class="import-result">
        <el-alert
          :title="`导入完成:成功 ${importDialog.result.success || 0} 条,重复 ${importDialog.result.duplicateCount || 0} 条(已跳过),失败 ${importDialog.result.failCount || 0} 条`"
          :type="importDialog.result.failList && importDialog.result.failList.length ? 'warning' : 'success'"
          :closable="false"
          show-icon
          style="margin-top: 12px"
        />
        <div
          v-if="importDialog.result.duplicateList && importDialog.result.duplicateList.length"
          style="margin-top: 8px"
        >
          <div style="margin-bottom: 4px; color: #e6a23c; font-size: 13px">
            重复列表 ({{ importDialog.result.duplicateCount || importDialog.result.duplicateList.length }})
          </div>
          <el-table :data="importDialog.result.duplicateList" border size="mini" max-height="200">
            <el-table-column prop="rowIndex" label="行号" width="70" align="center" />
            <el-table-column prop="name" label="姓名" min-width="100" show-overflow-tooltip />
            <el-table-column prop="idCard" label="身份证号" min-width="180" show-overflow-tooltip />
            <el-table-column prop="profession" label="专业" min-width="120" show-overflow-tooltip />
            <el-table-column prop="skillLevel" label="级别" min-width="100" show-overflow-tooltip />
            <el-table-column prop="phone" label="手机号" min-width="120" show-overflow-tooltip />
            <el-table-column prop="certType" label="证书类型" min-width="120" show-overflow-tooltip />
          </el-table>
        </div>
        <div
          v-if="importDialog.result.failList && importDialog.result.failList.length"
          style="margin-top: 8px"
        >
          <div style="margin-bottom: 4px; color: #f56c6c; font-size: 13px">
            失败列表 ({{ importDialog.result.failCount || importDialog.result.failList.length }})
          </div>
          <el-table :data="importDialog.result.failList" border size="mini" max-height="200">
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="name" label="姓名" min-width="100" show-overflow-tooltip />
            <el-table-column prop="idCard" label="证件号码" min-width="180" show-overflow-tooltip />
            <el-table-column prop="reason" label="失败原因" min-width="180" show-overflow-tooltip />
          </el-table>
        </div>
      </div>

      <div slot="footer">
        <el-button @click="importDialog.visible = false">取 消</el-button>
        <el-button
          type="primary"
          :loading="importDialog.submitting"
          @click="submitImport"
      >
        开始导入
      </el-button>
    </div>
  </el-dialog>

  <!-- 编辑学生(含改密码) -->
  <el-dialog
    title="编辑学生"
    :visible.sync="editDialog.visible"
    width="560px"
    @closed="onEditDialogClosed"
  >
    <el-form ref="editForm" :model="editDialog.form" :rules="editDialog.rules" label-width="100px" size="small">
      <el-form-item label="学号">
        <el-input v-model="editDialog.form.studentNo" placeholder="留空自动分配" />
      </el-form-item>
      <el-form-item label="姓名" prop="name">
        <el-input v-model="editDialog.form.name" placeholder="请输入姓名" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="editDialog.form.nickname" placeholder="可选" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="editDialog.form.phone" placeholder="选填" />
      </el-form-item>
      <el-form-item label="身份证号" prop="idCard">
        <el-input
          v-model="editDialog.form.idCard"
          placeholder="选填,请输入身份证号"
        />
      </el-form-item>
      <el-form-item label="专业">
        <el-select v-model="editDialog.form.professionIds" placeholder="请选择或输入新专业" multiple filterable allow-create default-first-option clearable style="width: 100%">
          <el-option
            v-for="p in professionOptions"
            :key="p.id"
            :label="p.name"
            :value="p.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="证书类型" prop="certType">
        <el-select v-model="editDialog.form.certType" placeholder="请选择证书类型" filterable style="width: 100%">
          <el-option v-for="item in certTypeOptions" :key="item.id" :label="item.name" :value="item.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="editDialog.form.status">
          <el-radio :label="1">正常</el-radio>
          <el-radio :label="0">冻结</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="新密码" prop="password">
        <el-input
          v-model="editDialog.form.password"
          type="password"
          show-password
          placeholder="留空则不修改密码"
          autocomplete="new-password"
        />
      </el-form-item>
    </el-form>
    <div slot="footer">
      <el-button @click="editDialog.visible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="editDialog.submitting"
        @click="submitEdit"
      >保存</el-button>
    </div>
  </el-dialog>
  </div>
</template>

<script>
import {
  studentPage,
  addStudent,
  updateStudent,
  deleteStudent,
  batchDeleteStudents,
  freezeStudent,
  getStudentCourses,
  openCourses,
  getStudentExams,
  openExams,
  importStudents,
  downloadTemplate
} from '@/api/student'
import { closeCourseStudent } from '@/api/course'
import { closeExamStudent } from '@/api/exam'
import { professions, addProfession } from '@/api/setting'
import { certificateTypeList } from '@/api/certificateType'
import { downloadBlob } from '@/utils'

export default {
  name: 'StudentList',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      selection: [],
      professionOptions: [],
      certTypeOptions: [],
      query: {
        page: 1,
        size: 10,
        professionId: undefined,
        keyword: '',
        status: undefined,
        dateRange: [],
        exactCount: undefined
      },
      courseDialog: {
        visible: false,
        loading: false,
        submitting: false,
        studentId: null,
        studentPhone: '',
        activeTab: 'unopened',
        opened: [],
        unopened: [],
        selected: [],
        selectedRows: []
      },
      examDialog: {
        visible: false,
        loading: false,
        submitting: false,
        studentId: null,
        studentPhone: '',
        activeTab: 'unopened',
        opened: [],
        unopened: [],
        selected: [],
        selectedRows: []
      },
      importDialog: {
        visible: false,
        submitting: false,
        file: null,
        result: null
      },
      // 编辑学生(含改密码):仅在前端维护一个 dialog 实例
      editDialog: {
        visible: false,
        submitting: false,
        form: {
          id: null,
          studentNo: '',
          name: '',
          nickname: '',
          phone: '',
          idCard: '',
          professionIds: [],
          professionId: null,
          certType: '',
          status: 1,
          password: ''
        },
        rules: {
          name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
          certType: [{ required: true, message: '请选择证书类型', trigger: 'change' }],
          phone: [
            { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
          ],
          idCard: [
            { required: true, message: '请输入身份证号', trigger: 'blur' },
            {
              validator: (rule, value, cb) => {
                if (!value) return cb(new Error('请输入身份证号'))
                // 格式校验:不合法仍允许提交(前端只做提示,不阻止)
                // 校验位不正确也不拦截,后台会正常保存,列表页用浅红色背景标注
                return cb()
              },
              trigger: 'blur'
            }
          ],
          status: [{ required: true, message: '请选择状态', trigger: 'change' }],
          password: [
            {
              validator: (rule, value, cb) => {
                // 留空 = 不改密码,合法
                if (!value) return cb()
                if (value.length < 6 || value.length > 32) {
                  return cb(new Error('密码长度 6-32 位'))
                }
                return cb()
              },
              trigger: 'blur'
            }
          ]
        }
      },
      addDialog: {
        visible: false,
        submitting: false,
        form: {
          name: '',
          studentNo: '',
          phone: '',
          password: '',
          professionIds: [],
          professionId: undefined,
          nickname: '',
          idCard: '',
          certType: ''
        },
        rules: {
          name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
          certType: [{ required: true, message: '请选择证书类型', trigger: 'change' }],
          phone: [
            { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
          ],
          idCard: [
            {
              validator: (rule, value, cb) => {
                // 校验位不正确也不拦截,后台会正常保存,列表页用浅红色背景标注
                return cb()
              },
              trigger: 'blur'
            }
          ]
        }
      }
    }
  },
  computed: {
    importTipTitle() {
      return '请上传 Excel 文件，按统一导入模板填写。其中「职业名称」对应学生的「专业」'
    }
  },
  created() {
    this.fetchProfessions()
    this.fetchCertTypes()
    this.fetchList()
  },
  methods: {
    fetchProfessions() {
      return professions()
        .then((res) => {
          this.professionOptions = res.data || []
        })
        .catch(() => {
          this.professionOptions = []
        })
    },
    fetchCertTypes() {
      certificateTypeList()
        .then((res) => {
          this.certTypeOptions = res.data || []
        })
        .catch(() => {
          this.certTypeOptions = []
        })
    },
    /**
     * 身份证号脱敏: 110101********1234
     */
    maskIdCard(idCard) {
      if (!idCard) return ''
      if (idCard.length <= 8) return idCard
      return idCard.substring(0, 6) + '********' + idCard.substring(14)
    },
    /**
     * 身份证号 18 位校验(含校验位算法 GB 11643)
     */
    validateIdCard(idCard) {
      if (!idCard) return true
      if (!/^[1-9]\d{5}(?:18|19|20)\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/.test(idCard)) {
        return false
      }
      // 校验位算法
      var weight = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
      var checkCodes = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']
      var sum = 0
      for (var i = 0; i < 17; i++) {
        sum += parseInt(idCard.charAt(i)) * weight[i]
      }
      var expectedCheck = checkCodes[sum % 11]
      return expectedCheck === idCard.charAt(17).toUpperCase()
    },
    onIdCardBlur(type) {
      // 身份证格式校验已去除,不再拦截或警告
    },
    fetchList() {
      this.loading = true
      const params = {
        page: this.query.page,
        size: this.query.size,
        professionId: this.query.professionId,
        keyword: this.query.keyword,
        status: this.query.status
      }
      if (this.query.dateRange && this.query.dateRange.length === 2) {
        params.registerTimeStart = this.query.dateRange[0]
        params.registerTimeEnd = this.query.dateRange[1]
      }
      if (this.query.exactCount && this.query.exactCount > 0) {
        params.exactCount = this.query.exactCount
      }
      studentPage(params)
        .then((res) => {
          const data = res.data || {}
          this.list = data.records || data.list || data.rows || []
          this.total = data.total || 0
        })
        .catch(() => {
          this.list = []
          this.total = 0
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleSearch() {
      this.query.page = 1
      this.fetchList()
    },
    handleReset() {
      this.query = {
        page: 1,
        size: 10,
        professionId: undefined,
        keyword: '',
        status: undefined,
        dateRange: [],
        exactCount: undefined
      }
      this.fetchList()
    },
    handleSizeChange(size) {
      this.query.size = size
      this.query.page = 1
      this.fetchList()
    },
    handleCurrentChange(page) {
      this.query.page = page
      this.fetchList()
    },
    handleDetail(row) {
      this.$router.push(`/student/detail/${row.id}`).catch(() => {})
    },
    openAddDialog() {
      this.addDialog.form = {
        name: '',
        studentNo: '',
        phone: '',
        password: '',
        professionIds: [],
        professionId: undefined,
        nickname: '',
        idCard: '',
        certType: ''
      }
      this.addDialog.visible = true
      this.$nextTick(() => {
        this.$refs.addForm && this.$refs.addForm.clearValidate()
      })
    },
    async submitAdd() {
      this.$refs.addForm.validate(async (valid) => {
        if (!valid) return
        this.addDialog.submitting = true
        try {
          const data = { ...this.addDialog.form }
          if (!data.password) delete data.password
          if (!data.studentNo) delete data.studentNo
          // 处理专业: allow-create 产生的是字符串(新专业名),需先创建拿ID
          if (data.professionIds && data.professionIds.length > 0) {
            const existingIds = this.professionOptions.map(p => p.id)
            const newNames = data.professionIds.filter(v => !existingIds.includes(v))
            // 先创建新专业,拿到ID后替换
            for (const name of newNames) {
              try {
                const res = await addProfession({ name: String(name), sort: 0, status: 1 })
                // 后端返回创建的专业对象(含自增ID)
                if (res && res.data && res.data.id) {
                  const idx = data.professionIds.indexOf(name)
                  if (idx >= 0) data.professionIds[idx] = res.data.id
                } else {
                  // 兜底:重新拉取专业列表获取新ID
                  await this.fetchProfessions()
                  const created = this.professionOptions.find(p => p.name === String(name))
                  if (created) {
                    const idx = data.professionIds.indexOf(name)
                    if (idx >= 0) data.professionIds[idx] = created.id
                  }
                }
              } catch (e) {
                // 专业可能已存在(并发),尝试从列表中找
                await this.fetchProfessions()
                const found = this.professionOptions.find(p => p.name === String(name))
                if (found) {
                  const idx = data.professionIds.indexOf(name)
                  if (idx >= 0) data.professionIds[idx] = found.id
                }
              }
            }
            // 安全过滤:移除仍未解析为数字ID的专业名,避免后端反序列化失败
            data.professionIds = data.professionIds.filter(v => typeof v === 'number')
          }
          if (!data.professionIds || data.professionIds.length === 0) {
            delete data.professionIds
          }
          delete data.professionId
          await addStudent(data)
          this.$message.success('新增成功')
          this.addDialog.visible = false
          this.query.page = 1
          this.fetchList()
        } catch (err) {
          this.$message.error((err && err.message) || '新增失败')
        } finally {
          this.addDialog.submitting = false
        }
      })
    },
    handleFreeze(row) {
      const target = row.status === 1 ? 0 : 1
      const text = target === 1 ? '解冻' : '冻结'
      this.$confirm(`确定要${text}学生 "${row.phone}" 吗?`, '提示', { type: 'warning' })
        .then(() => {
          freezeStudent(row.id).then(() => {
            this.$message.success(`${text}成功`)
            this.fetchList()
          })
        })
        .catch(() => {})
    },
    // ====== 编辑学生(含改密码) ======
    handleEdit(row) {
      // 关键:打开 dialog 前先回填(从列表行复制,避免依赖后端 detail 接口)
      this.editDialog.form = {
        id: row.id,
        studentNo: row.studentNo || '',
        name: row.name || row.realName || '',
        nickname: row.nickname || '',
        phone: row.phone || '',
        idCard: row.idCard || '',
        professionIds: row.professionIds || [],
        professionId: row.professionId || null,
        certType: row.certType || '',
        status: row.status === undefined ? 1 : row.status,
        password: ''
      }
      this.editDialog.visible = true
    },
    async submitEdit() {
      this.$refs.editForm.validate(async (valid) => {
        if (!valid) return
        this.editDialog.submitting = true
        try {
          const payload = { ...this.editDialog.form }
          if (!payload.studentNo) delete payload.studentNo
          if (!payload.nickname) delete payload.nickname
          if (!payload.password) delete payload.password
          // 处理专业: allow-create 产生的是字符串(新专业名),需先创建拿ID
          if (payload.professionIds && payload.professionIds.length > 0) {
            const existingIds = this.professionOptions.map(p => p.id)
            const newNames = payload.professionIds.filter(v => !existingIds.includes(v))
            for (const name of newNames) {
              try {
                const res = await addProfession({ name: String(name), sort: 0, status: 1 })
                if (res && res.data && res.data.id) {
                  const idx = payload.professionIds.indexOf(name)
                  if (idx >= 0) payload.professionIds[idx] = res.data.id
                } else {
                  await this.fetchProfessions()
                  const created = this.professionOptions.find(p => p.name === String(name))
                  if (created) {
                    const idx = payload.professionIds.indexOf(name)
                    if (idx >= 0) payload.professionIds[idx] = created.id
                  }
                }
              } catch (e) {
                await this.fetchProfessions()
                const found = this.professionOptions.find(p => p.name === String(name))
                if (found) {
                  const idx = payload.professionIds.indexOf(name)
                  if (idx >= 0) payload.professionIds[idx] = found.id
                }
              }
            }
            // 安全过滤:移除仍未解析为数字ID的专业名
            payload.professionIds = payload.professionIds.filter(v => typeof v === 'number')
          }
          if (!payload.professionIds || payload.professionIds.length === 0) {
            delete payload.professionIds
          }
          delete payload.professionId
          if (payload.idCard == null) delete payload.idCard
          await updateStudent(this.editDialog.form.id, payload)
          this.$message.success('保存成功')
          this.editDialog.visible = false
          this.fetchList()
        } catch (err) {
          this.$message.error((err && err.message) || '保存失败')
        } finally {
          this.editDialog.submitting = false
        }
      })
    },
    onEditDialogClosed() {
      // dialog 关闭时清空密码字段(防止下次打开看到旧密码)
      this.editDialog.form.password = ''
      this.editDialog.form.id = null
      if (this.$refs.editForm) {
        this.$refs.editForm.clearValidate()
      }
    },
    // ====== 批量删除学生 ======
    handleBatchDelete() {
      if (this.selection.length === 0) {
        this.$message.warning('请先选择要删除的学生')
        return
      }
      const count = this.selection.length
      this.$confirm(
        `确定要删除选中的 ${count} 名学生吗?\n将同时删除其开通的课程/考试/学习记录,且不可恢复!`,
        '批量删除确认',
        { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
      )
        .then(() => {
          this.loading = true
          const ids = this.selection.map(s => s.id)
          batchDeleteStudents(ids)
            .then(() => {
              this.$message.success('批量删除成功')
              this.selection = []
              this.fetchList()
            })
            .catch((err) => {
              this.$message.error((err && err.message) || '批量删除失败')
            })
            .finally(() => {
              this.loading = false
            })
        })
        .catch(() => {})
    },
    // ====== 删除学生 ======
    handleDelete(row) {
      // 删除是不可恢复操作,要求用户二次确认 + 输入手机号确认
      this.$prompt(
        `确定要删除学生 "${row.name || row.phone}" 吗?\n将同时删除其开通的课程/考试/学习记录,且不可恢复!\n请输入手机号 "${row.phone}" 以确认:`,
        '危险操作',
        {
          confirmButtonText: '确认删除',
          cancelButtonText: '取消',
          type: 'warning',
          inputPattern: new RegExp('^' + this.escapeRegExp(row.phone) + '$'),
          inputErrorMessage: '手机号输入不一致,已取消',
          inputValue: ''
        }
      )
        .then(({ value }) => {
          if (value !== row.phone) {
            this.$message.error('手机号不匹配,已取消')
            return
          }
          deleteStudent(row.id)
            .then(() => {
              this.$message.success('删除成功')
              this.fetchList()
            })
            .catch((err) => {
              this.$message.error((err && err.message) || '删除失败')
            })
        })
        .catch(() => {})
    },
    escapeRegExp(s) {
      return String(s).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    },
    handleOpenCourse(row) {
      this.courseDialog.studentId = row.id
      this.courseDialog.studentPhone = row.phone
      this.courseDialog.visible = true
      this.courseDialog.activeTab = 'unopened'
      this.courseDialog.selected = []
      this.courseDialog.selectedRows = []
      this.courseDialog.opened = []
      this.courseDialog.unopened = []
      this.fetchStudentCourses()
    },
    fetchStudentCourses() {
      this.courseDialog.loading = true
      getStudentCourses(this.courseDialog.studentId)
        .then((res) => {
          const data = res.data || {}
          this.courseDialog.opened = data.opened || data.openedList || []
          this.courseDialog.unopened = data.notOpened || data.unopened || data.unopenedList || []
        })
        .catch(() => {
          this.courseDialog.opened = []
          this.courseDialog.unopened = []
        })
        .finally(() => {
          this.courseDialog.loading = false
        })
    },
    handleCourseSelectionChange(rows) {
      this.courseDialog.selectedRows = rows
      this.courseDialog.selected = rows.map((r) => r.id)
    },
    submitOpenCourse() {
      if (this.courseDialog.selected.length === 0) {
        this.$message.warning('请选择要开通的课程')
        return
      }
      this.courseDialog.submitting = true
      openCourses({ studentId: this.courseDialog.studentId, courseIds: this.courseDialog.selected })
        .then(() => {
          this.$message.success('开通成功')
          this.courseDialog.submitting = false
          this.fetchStudentCourses()
          this.courseDialog.selected = []
        })
        .catch(() => {
          this.courseDialog.submitting = false
        })
    },
    handleCloseCourse(row) {
      const courseId = row.courseId || row.id
      this.$confirm(`确定取消开通课程 "${row.name}" 吗?`, '提示', { type: 'warning' })
        .then(() => {
          closeCourseStudent(courseId, this.courseDialog.studentId).then(() => {
            this.$message.success('已取消开通')
            this.fetchStudentCourses()
          })
        })
        .catch(() => {})
    },
    handleOpenExam(row) {
      this.examDialog.studentId = row.id
      this.examDialog.studentPhone = row.phone
      this.examDialog.visible = true
      this.examDialog.activeTab = 'unopened'
      this.examDialog.selected = []
      this.examDialog.selectedRows = []
      this.examDialog.opened = []
      this.examDialog.unopened = []
      this.fetchStudentExams()
    },
    fetchStudentExams() {
      this.examDialog.loading = true
      getStudentExams(this.examDialog.studentId)
        .then((res) => {
          const data = res.data || {}
          this.examDialog.opened = data.opened || data.openedList || []
          this.examDialog.unopened = data.notOpened || data.unopened || data.unopenedList || []
        })
        .catch(() => {
          this.examDialog.opened = []
          this.examDialog.unopened = []
        })
        .finally(() => {
          this.examDialog.loading = false
        })
    },
    handleExamSelectionChange(rows) {
      this.examDialog.selectedRows = rows
      this.examDialog.selected = rows.map((r) => r.id)
    },
    submitOpenExam() {
      if (this.examDialog.selected.length === 0) {
        this.$message.warning('请选择要开通的考试')
        return
      }
      this.examDialog.submitting = true
      openExams({ studentId: this.examDialog.studentId, examIds: this.examDialog.selected })
        .then(() => {
          this.$message.success('开通成功')
          this.examDialog.submitting = false
          this.fetchStudentExams()
          this.examDialog.selected = []
        })
        .catch(() => {
          this.examDialog.submitting = false
        })
    },
    handleCloseExam(row) {
      const examId = row.examId || row.id
      this.$confirm(`确定取消开通考试 "${row.name}" 吗?`, '提示', { type: 'warning' })
        .then(() => {
          closeExamStudent(examId, this.examDialog.studentId).then(() => {
            this.$message.success('已取消开通')
            this.fetchStudentExams()
          })
        })
        .catch(() => {})
    },
    openImportDialog() {
      this.importDialog.file = null
      this.importDialog.result = null
      this.importDialog.visible = true
      this.$nextTick(() => {
        this.$refs.importUpload && this.$refs.importUpload.clearFiles()
      })
    },
    handleImportChange(file) {
      if (file.status === 'ready') {
        if (!/\.(xlsx|xls)$/i.test(file.name)) {
          this.$message.error('只能上传 Excel 文件')
          this.$refs.importUpload && this.$refs.importUpload.clearFiles()
          this.importDialog.file = null
          return
        }
        this.importDialog.file = file.raw
        this.importDialog.result = null
      }
    },
    handleImportRemove() {
      this.importDialog.file = null
    },
    submitImport() {
      if (!this.importDialog.file) {
        this.$message.warning('请先选择 Excel 文件')
        return
      }
      this.importDialog.submitting = true
      this.importDialog.result = null
      const formData = new FormData()
      formData.append('file', this.importDialog.file)
      importStudents(formData)
        .then((res) => {
          const data = (res && res.data) || {}
          const failList = data.failList || data.failures || data.errors || data.failedRows || []
          const dupCount = data.duplicateCount || 0
          const dupList = data.duplicateList || []
          this.importDialog.result = {
            // 优先使用后端返回的 successCount(新接口),兼容旧字段 success/count
            success: data.successCount != null ? data.successCount : (data.success != null ? data.success : (data.count || 0)),
            // 优先使用后端返回的 failCount,兜底用 failList 长度
            failCount: data.failCount != null ? data.failCount : failList.length,
            failList: failList,
            duplicateCount: dupCount,
            duplicateList: dupList
          }
          const ok = this.importDialog.result.success
          const fail = this.importDialog.result.failCount
          // 有重复数据时弹框提示
          if (dupCount > 0 && fail > 0 && ok === 0) {
            this.$alert('全部数据未通过: 重复 ' + dupCount + ' 条(姓名+身份证+专业+级别完全相同,已跳过),失败 ' + fail + ' 条。请查看下方"重复列表"和"失败明细"。', '导入未通过', { type: 'error' })
          } else if (dupCount > 0 && fail > 0) {
            this.$alert('导入完成: 成功 ' + ok + ' 条,重复 ' + dupCount + ' 条(已自动跳过),失败 ' + fail + ' 条。请查看下方"重复列表"和"失败明细"。', '导入完成(有重复和失败)', { type: 'warning' })
          } else if (dupCount > 0 && fail === 0) {
            this.$alert('导入完成: 成功 ' + ok + ' 条,重复 ' + dupCount + ' 条(姓名+身份证+专业+级别完全相同,已自动跳过未重复导入)。请查看下方"重复列表"了解详情。', '导入完成(有重复)', { type: 'warning' })
          } else if (fail > 0 && ok === 0) {
            this.$alert('全部 ' + fail + ' 条数据未通过校验,请查看下方"失败明细"修正后重传。', '校验未通过', { type: 'error' })
          } else if (fail > 0) {
            this.$alert('成功 ' + ok + ' 条,失败 ' + fail + ' 条,请查看下方"失败明细"。', '导入完成(有失败)', { type: 'warning' })
          } else {
            this.$message.success('导入成功 ' + ok + ' 条')
          }
          this.fetchList()
        })
        .catch((err) => {
          // 显示后端返回的真实错误信息
          const msg = (err && err.message) || '导入失败,请检查文件格式或网络后重试'
          this.$alert(msg, '导入异常', { type: 'error' })
        })
        .finally(() => {
          this.importDialog.submitting = false
        })
    },
    handleDownloadTemplate() {
      downloadTemplate()
        .then((response) => {
          downloadBlob(response, '学生导入模板.xlsx')
        })
        .catch(() => {
          this.$message.error('模板下载失败，请稍后重试')
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.danger-text {
  color: #f56c6c;
}
.id-card {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: #606266;
  letter-spacing: 0.5px;
  padding: 2px 6px;
  border-radius: 3px;
}
.id-card-invalid {
  background-color: #fef0f0;
  color: #f56c6c;
}
.text-muted {
  color: #c0c4cc;
}
</style>
