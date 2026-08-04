<template>
  <div class="app-container">
    <el-card shadow="never" v-loading="loading">
      <div slot="header">
        <span>考试安全设置</span>
      </div>

      <el-form
        ref="form"
        :model="form"
        :rules="rules"
        label-width="140px"
        style="max-width: 600px"
      >
        <el-form-item label="考前人脸识别" prop="enabled">
          <el-switch
            v-model="form.enabled"
            active-value="1"
            inactive-value="0"
            active-text="开启"
            inactive-text="关闭"
          />
          <div class="form-tip">
            开启后，学生进入考试前需要拍摄实时照片，与证书照片进行比对
          </div>
        </el-form-item>

        <el-form-item label="人脸比对阈值" prop="threshold">
          <el-slider
            v-model="form.threshold"
            :min="0.3"
            :max="0.8"
            :step="0.05"
            show-input
          />
          <div class="form-tip">
            范围 0.3-0.8，值越小越严格。建议值 0.6
          </div>
        </el-form-item>

        <el-form-item label="最大重试次数" prop="maxRetries">
          <el-input-number
            v-model="form.maxRetries"
            :min="1"
            :max="5"
          />
          <div class="form-tip">
            验证失败后的最大重试次数，超过后需联系管理员
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitForm">
            保存设置
          </el-button>
          <el-button @click="fetchConfig">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 20px">
      <div slot="header">
        <span>今日验证统计</span>
        <el-date-picker
          v-model="statsDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="选择日期"
          size="mini"
          style="float: right; width: 150px"
          @change="fetchStats"
        />
      </div>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-number">{{ stats.total || 0 }}</div>
            <div class="stat-label">验证总次数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-number success">{{ stats.success || 0 }}</div>
            <div class="stat-label">验证通过</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-number danger">{{ stats.fail || 0 }}</div>
            <div class="stat-label">验证失败</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-number">{{ stats.passRate || 0 }}%</div>
            <div class="stat-label">通过率</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" style="margin-top: 20px">
      <div slot="header">
        <span>验证记录</span>
      </div>
      <el-form :inline="true" :model="query" size="small" class="filter-form">
        <el-form-item label="学生姓名">
          <el-input v-model="query.studentName" clearable placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="考试名称">
          <el-input v-model="query.examName" clearable placeholder="请输入考试名称" />
        </el-form-item>
        <el-form-item label="验证结果">
          <el-select v-model="query.verifyResult" clearable placeholder="全部">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="loadLogs">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="logLoading" :data="logList" border size="small">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="studentName" label="学生姓名" width="100" align="center" />
        <el-table-column prop="studentPhone" label="手机号" width="120" align="center" />
        <el-table-column prop="examName" label="考试名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="验证结果" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.verifyResult === 1 ? 'success' : 'danger'" size="small">
              {{ row.verifyResult === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="相似度" width="100" align="center">
          <template slot-scope="{ row }">
            <span v-if="row.similarity !== null">
              {{ ((1 - row.similarity) * 100).toFixed(1) }}%
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="deviceInfo" label="设备信息" min-width="150" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="120" align="center" />
        <el-table-column prop="createTime" label="验证时间" width="180" align="center" />
      </el-table>
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :current-page.sync="query.page"
        :page-size.sync="query.size"
        :total="logTotal"
        :page-sizes="[10, 20, 50]"
        @current-change="loadLogs"
        @size-change="loadLogs"
        style="margin-top: 16px; text-align: right"
      />
    </el-card>
  </div>
</template>

<script>
import {
  getFaceVerifyConfig,
  saveFaceVerifyConfig,
  getFaceVerifyStats,
  getFaceVerifyLogs
} from '@/api/setting'

export default {
  name: 'SettingFaceVerify',
  data() {
    return {
      loading: false,
      submitting: false,
      form: {
        enabled: '0',
        threshold: 0.6,
        maxRetries: 3
      },
      rules: {},
      statsDate: '',
      stats: {
        total: 0,
        success: 0,
        fail: 0,
        passRate: 0
      },
      query: {
        page: 1,
        size: 10,
        studentName: '',
        examName: '',
        verifyResult: null
      },
      logList: [],
      logTotal: 0,
      logLoading: false
    }
  },
  created() {
    this.fetchConfig()
    this.fetchStats()
    this.loadLogs()
  },
  methods: {
    fetchConfig() {
      this.loading = true
      getFaceVerifyConfig()
        .then(res => {
          const data = res.data || {}
          this.form = {
            enabled: data.enabled ? '1' : '0',
            threshold: data.threshold || 0.6,
            maxRetries: data.maxRetries || 3
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    fetchStats() {
      getFaceVerifyStats(this.statsDate)
        .then(res => {
          this.stats = res.data || {}
        })
    },
    loadLogs() {
      this.logLoading = true
      getFaceVerifyLogs({
        page: this.query.page,
        size: this.query.size,
        studentName: this.query.studentName,
        examName: this.query.examName,
        verifyResult: this.query.verifyResult
      })
        .then(res => {
          const data = res.data || {}
          this.logList = data.list || []
          this.logTotal = data.total || 0
        })
        .finally(() => {
          this.logLoading = false
        })
    },
    submitForm() {
      this.submitting = true
      saveFaceVerifyConfig({
        enabled: this.form.enabled,
        threshold: this.form.threshold.toString(),
        maxRetries: this.form.maxRetries.toString()
      })
        .then(() => {
          this.$message.success('保存成功')
        })
        .finally(() => {
          this.submitting = false
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}

.stat-card {
  text-align: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;

  .stat-number {
    font-size: 28px;
    font-weight: bold;
    color: #409eff;

    &.success { color: #67c23a; }
    &.danger { color: #f56c6c; }
  }

  .stat-label {
    font-size: 13px;
    color: #909399;
    margin-top: 8px;
  }
}

.filter-form {
  margin-bottom: 16px;
}
</style>
