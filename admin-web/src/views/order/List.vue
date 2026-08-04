<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="filter-container">
        <div class="pay-switch-box">
          <span class="pay-switch-label">在线支付</span>
          <el-switch
            v-model="payEnabled"
            active-text="已开启"
            inactive-text="已关闭"
            @change="handlePaySwitchChange"
          />
          <span class="pay-switch-tip">关闭后学员端不显示支付入口,已生成的待支付订单也无法继续支付</span>
        </div>
        <el-input
          v-model="query.keyword"
          placeholder="订单号 / 课程名"
          clearable
          class="filter-item"
          style="width: 220px"
          @keyup.enter.native="handleSearch"
        />
        <el-select
          v-model="query.status"
          placeholder="订单状态"
          clearable
          class="filter-item"
          style="width: 140px"
        >
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已关闭" :value="2" />
        </el-select>
        <el-select
          v-model="query.channel"
          placeholder="支付渠道"
          clearable
          class="filter-item"
          style="width: 140px"
        >
          <el-option label="微信支付" value="wechat" />
          <el-option label="支付宝" value="alipay" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" class="filter-item" @click="handleSearch">
          搜索
        </el-button>
        <el-button icon="el-icon-refresh" class="filter-item" @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="orderNo" label="订单号" min-width="190" align="center" show-overflow-tooltip />
        <el-table-column prop="studentName" label="学生" width="120" align="center" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.studentName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="courseName" label="课程" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.courseName || '-' }}</template>
        </el-table-column>
        <el-table-column label="金额" width="100" align="center">
          <template slot-scope="{ row }">
            <span class="amount-text">¥{{ fenToYuan(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="渠道" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.channel === 'alipay' ? 'warning' : 'success'" size="small">
              {{ row.channel === 'alipay' ? '支付宝' : '微信' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="160" align="center" />
        <el-table-column prop="payTime" label="支付时间" width="160" align="center">
          <template slot-scope="{ row }">{{ row.payTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="transactionId" label="交易号" min-width="200" align="center" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.transactionId || '-' }}</template>
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
  </div>
</template>

<script>
import { orderPage, getPaySwitch, setPaySwitch } from '@/api/order'
import { Message } from 'element-ui'

export default {
  name: 'OrderList',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      payEnabled: false,
      query: {
        page: 1,
        size: 10,
        keyword: '',
        status: undefined,
        channel: undefined
      }
    }
  },
  created() {
    this.fetchList()
    this.fetchPaySwitch()
  },
  methods: {
    fetchPaySwitch() {
      getPaySwitch()
        .then((res) => {
          const data = res.data || res
          this.payEnabled = !!data.enabled
        })
        .catch(() => {})
    },
    handlePaySwitchChange(val) {
      setPaySwitch(val)
        .then(() => {
          Message.success(val ? '在线支付已开启' : '在线支付已关闭')
        })
        .catch(() => {
          // 失败时回滚开关状态
          this.payEnabled = !val
        })
    },
    fetchList() {
      this.loading = true
      orderPage(this.query)
        .then((res) => {
          const data = res.data || res
          this.list = data.records || data.list || []
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
      this.query = { page: 1, size: this.query.size, keyword: '', status: undefined, channel: undefined }
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
    statusText(status) {
      return { 0: '待支付', 1: '已支付', 2: '已关闭' }[status] || '未知'
    },
    statusType(status) {
      return { 0: 'warning', 1: 'success', 2: 'info' }[status] || 'info'
    },
    fenToYuan(fen) {
      if (fen === null || fen === undefined) return '0.00'
      return (fen / 100).toFixed(2)
    }
  }
}
</script>

<style lang="scss" scoped>
.amount-text {
  color: #ee0a24;
  font-weight: 600;
}
</style>
