<template>
  <div class="ann-list-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <div class="page-title">通知公告</div>

        <div v-if="loading" class="loading">
          <van-loading size="24" color="#1989fa">加载中...</van-loading>
        </div>

        <div v-else-if="list.length === 0" class="empty">
          <van-empty description="暂无公告" />
        </div>

        <ul v-else class="ann-list">
          <li
            v-for="item in list"
            :key="item.id"
            class="ann-item"
            @click="goDetail(item)"
          >
            <div class="ann-text">
              <div class="ann-title">{{ item.title }}</div>
              <div class="ann-date">{{ formatDate(item.publishTime || item.createTime) }}</div>
            </div>
            <van-icon name="arrow" class="arrow-icon" />
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getAnnouncementList } from '@/api/home'

export default {
  name: 'AnnouncementList',
  components: { Header },
  data() {
    return {
      list: [],
      loading: true
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    formatDate(date) {
      if (!date) return ''
      const d = new Date(date)
      const yyyy = d.getFullYear()
      const mm = String(d.getMonth() + 1).padStart(2, '0')
      const dd = String(d.getDate()).padStart(2, '0')
      return `${yyyy}-${mm}-${dd}`
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await getAnnouncementList()
        console.log('公告接口返回：', res)
        // 兼容多种后端返回结构
        const payload = res.data || res
        const data = payload.data || payload
        console.log('解析后 data：', data)
        if (Array.isArray(data)) {
          this.list = data
        } else if (data && typeof data === 'object') {
          this.list = data.list || data.records || data.items || data.rows || []
        } else {
          this.list = []
        }
        console.log('最终 list 长度：', this.list.length)
      } catch (error) {
        console.error('公告接口失败：', error)
        this.list = []
      } finally {
        this.loading = false
      }
    },
    goDetail(item) {
      sessionStorage.setItem('news_detail_data', JSON.stringify(item))
      this.$router.push(`/news/detail/${item.id}?type=announcement`).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.ann-list-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-body {
  padding-top: var(--header-height, 170px);
  padding-bottom: 40px;
  min-height: 100vh;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  padding: 20px 0;
  border-bottom: 2px solid #c00;
  margin-bottom: 20px;
}

.loading, .empty {
  padding: 60px 0;
  text-align: center;
}

.ann-list {
  list-style: none;
  margin: 0;
  padding: 0;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.ann-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f9f9f9;
  }
}

.ann-text {
  flex: 1;
  min-width: 0;
}

.ann-title {
  font-size: 15px;
  color: #303133;
  line-height: 1.5;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ann-date {
  font-size: 13px;
  color: #909399;
}

.arrow-icon {
  color: #c0c4cc;
  margin-left: 12px;
  flex-shrink: 0;
}
</style>
