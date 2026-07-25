<template>
  <div class="news-list-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <div class="page-title">新闻动态</div>

        <div v-if="loading" class="loading">
          <van-loading size="24" color="#1989fa">加载中...</van-loading>
        </div>

        <div v-else-if="list.length === 0" class="empty">
          <van-empty description="暂无新闻" />
        </div>

        <ul v-else class="news-list">
          <li
            v-for="item in list"
            :key="item.id"
            class="news-item"
            @click="goDetail(item)"
          >
            <div class="news-text">
              <div class="news-title">{{ item.title }}</div>
              <div class="news-date">{{ formatDate(item.publishTime || item.createTime) }}</div>
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
import { getNewsList } from '@/api/home'

export default {
  name: 'NewsList',
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
      const hh = String(d.getHours()).padStart(2, '0')
      const mi = String(d.getMinutes()).padStart(2, '0')
      return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await getNewsList()
        console.log('新闻接口返回：', res)
        const payload = res.data || res
        const data = payload.data || payload
        if (Array.isArray(data)) {
          this.list = data
        } else if (data && typeof data === 'object') {
          this.list = data.list || data.records || data.items || data.rows || []
        } else {
          this.list = []
        }
        console.log('新闻 list 长度：', this.list.length)
      } catch (error) {
        console.error('新闻接口失败：', error)
        this.list = []
      } finally {
        this.loading = false
      }
    },
    goDetail(item) {
      sessionStorage.setItem('news_detail_data', JSON.stringify(item))
      this.$router.push(`/news/detail/${item.id}?type=news`).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.news-list-page {
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

.news-list {
  list-style: none;
  margin: 0;
  padding: 0;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.news-item {
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

.news-text {
  flex: 1;
  min-width: 0;
}

.news-title {
  font-size: 15px;
  color: #303133;
  line-height: 1.5;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.news-date {
  font-size: 13px;
  color: #909399;
}

.arrow-icon {
  color: #c0c4cc;
  margin-left: 12px;
  flex-shrink: 0;
}

/* 移动端适配:768px 以下 */
@media (max-width: 768px) {
  .container { width: 100%; max-width: 100%; padding: 12px; }
  .page-title { font-size: 18px; }
  .news-title {
    white-space: normal;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .news-item { padding: 12px 8px; }
}
</style>
