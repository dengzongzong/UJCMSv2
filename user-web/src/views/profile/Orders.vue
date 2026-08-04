<template>
  <div class="orders-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <div class="orders-card">
          <div class="card-header">
            <span class="card-title">我的订单</span>
          </div>

          <!-- 加载中 -->
          <div v-if="loading" class="orders-empty">
            <van-loading size="24" color="#1989fa" />
          </div>

          <!-- 空列表 -->
          <div v-else-if="orders.length === 0" class="orders-empty">
            <van-icon name="orders-o" size="56" color="#c8c9cc" />
            <p>暂无订单</p>
            <van-button size="small" type="primary" @click="$router.push('/course/my')">去选课</van-button>
          </div>

          <!-- 订单列表 -->
          <div v-else class="order-list">
            <div v-for="order in orders" :key="order.id" class="order-item">
              <div class="order-top">
                <span class="order-no">订单号:{{ order.orderNo }}</span>
                <van-tag :type="statusType(order.status)" size="small">{{ statusText(order.status) }}</van-tag>
              </div>
              <div class="order-main">
                <div class="order-course">
                  <div class="course-name">{{ order.courseName }}</div>
                  <div class="course-meta">
                    <span>渠道:{{ order.channel === 'alipay' ? '支付宝' : '微信支付' }}</span>
                    <span>{{ order.createTime }}</span>
                  </div>
                </div>
                <div class="order-amount">¥{{ fenToYuan(order.amount) }}</div>
              </div>
              <div class="order-bottom">
                <span v-if="order.status === 0" class="pay-tip">待支付,请尽快完成支付</span>
                <span v-else-if="order.payTime" class="pay-time">支付时间:{{ order.payTime }}</span>
                <van-button
                  v-if="order.status === 0"
                  size="small"
                  type="primary"
                  @click="continuePay(order)"
                >继续支付</van-button>
                <van-button
                  v-if="order.status === 1"
                  size="small"
                  plain
                  type="success"
                  @click="$router.push('/course/detail/' + order.courseId)"
                >去学习</van-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 继续支付弹窗 -->
    <van-popup v-model="showPayPopup" round :style="{ width: '360px', borderRadius: '12px' }">
      <div class="pay-popup">
        <div class="pay-title">继续支付</div>
        <div class="pay-course">{{ payingCourseName }}</div>
        <div class="pay-amount">¥{{ fenToYuan(payingAmount) }}</div>
        <div class="pay-qrcode">
          <img v-if="payData.qrImage" :src="payData.qrImage" alt="支付二维码" />
          <van-loading v-else size="32" color="#1989fa" vertical>正在生成二维码...</van-loading>
        </div>
        <div class="pay-channel">请使用{{ payData.channel === 'alipay' ? '支付宝' : '微信' }}扫码支付</div>
      </div>
    </van-popup>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getMyOrders, createOrder, getOrderByNo } from '@/api/order'
import { Toast } from 'vant'

export default {
  name: 'OrderList',
  components: { Header },
  data() {
    return {
      loading: false,
      orders: [],
      showPayPopup: false,
      payingCourseName: '',
      payingAmount: 0,
      payingOrderNo: '',
      payData: {},
      pollTimer: null
    }
  },
  created() {
    this.fetchOrders()
  },
  beforeDestroy() {
    this.stopPoll()
  },
  methods: {
    async fetchOrders() {
      this.loading = true
      try {
        const res = await getMyOrders()
        this.orders = res.data || res || []
      } catch (e) {
        this.orders = []
      } finally {
        this.loading = false
      }
    },
    statusText(status) {
      return { 0: '待支付', 1: '已支付', 2: '已关闭' }[status] || '未知'
    },
    statusType(status) {
      return { 0: 'warning', 1: 'success', 2: 'default' }[status] || 'default'
    },
    fenToYuan(fen) {
      if (fen === null || fen === undefined) return '0.00'
      return (fen / 100).toFixed(2)
    },
    async continuePay(order) {
      this.payingOrderNo = order.orderNo
      this.payingCourseName = order.courseName
      this.payingAmount = order.amount
      this.showPayPopup = true
      this.payData = { channel: order.channel }
      try {
        const res = await createOrder(order.courseId, order.channel)
        const data = res.data || res
        this.payData = data
        if (data.opened) {
          // 复用订单时若已开通(可能已在别处支付成功),直接刷新
          Toast.success('课程已开通')
          this.showPayPopup = false
          this.fetchOrders()
          return
        }
        this.startPoll()
      } catch (e) {
        this.showPayPopup = false
        Toast.fail(e.message || '生成支付二维码失败')
      }
    },
    startPoll() {
      this.stopPoll()
      this.pollTimer = setInterval(async () => {
        try {
          const res = await getOrderByNo(this.payingOrderNo)
          const order = res.data || res
          if (order && order.status === 1) {
            Toast.success('支付成功,课程已开通')
            this.stopPoll()
            this.showPayPopup = false
            this.fetchOrders()
          } else if (order && order.status === 2) {
            this.stopPoll()
            this.showPayPopup = false
            Toast('订单已关闭')
          }
        } catch (e) {
          // 轮询失败忽略,继续
        }
      }, 3000)
    },
    stopPoll() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.orders-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-body {
  padding-top: var(--header-height, 170px);
}

.container {
  width: 80%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

.orders-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;

  .card-header {
    padding: 18px 24px;
    border-bottom: 1px solid #f0f0f0;

    .card-title {
      font-size: 18px;
      font-weight: bold;
      color: #333;
    }
  }
}

.orders-empty {
  padding: 60px 20px;
  text-align: center;
  color: #999;

  p {
    margin: 12px 0 16px;
    font-size: 14px;
  }
}

.order-list {
  .order-item {
    padding: 16px 24px;
    border-bottom: 1px solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .order-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;

      .order-no {
        font-size: 12px;
        color: #999;
      }
    }

    .order-main {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .course-name {
        font-size: 15px;
        font-weight: 500;
        color: #333;
      }

      .course-meta {
        margin-top: 4px;
        font-size: 12px;
        color: #999;
        display: flex;
        gap: 12px;
      }

      .order-amount {
        font-size: 18px;
        font-weight: bold;
        color: #ee0a24;
        flex-shrink: 0;
        margin-left: 16px;
      }
    }

    .order-bottom {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      gap: 12px;
      margin-top: 10px;

      .pay-tip {
        font-size: 12px;
        color: #ff976a;
      }

      .pay-time {
        font-size: 12px;
        color: #999;
      }
    }
  }
}

/* 支付弹窗 */
.pay-popup {
  padding: 20px;
  text-align: center;

  .pay-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }

  .pay-course {
    margin-top: 8px;
    font-size: 13px;
    color: #666;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .pay-amount {
    margin-top: 6px;
    font-size: 26px;
    font-weight: bold;
    color: #ee0a24;
  }

  .pay-qrcode {
    margin: 16px auto 0;
    width: 220px;
    height: 220px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 8px;

    img {
      width: 200px;
      height: 200px;
    }
  }

  .pay-channel {
    margin-top: 12px;
    font-size: 13px;
    color: #666;
  }
}

@media (max-width: 768px) {
  .container { width: 100%; padding: 12px; }
  .order-item { padding: 14px 16px; }
}
</style>
