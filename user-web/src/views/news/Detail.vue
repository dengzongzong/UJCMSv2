<template>
  <div class="news-detail-page">
    <Header :show-back="true" />

    <div class="page-body">
      <div class="container">
        <!-- 面包屑 -->
        <div class="breadcrumb">
          <span>您当前所在位置：</span>
          <router-link to="/" class="crumb-link">主页</router-link>
          <span class="crumb-sep">&gt;</span>
          <router-link to="/news/center" class="crumb-link">中心动态</router-link>
          <span class="crumb-sep">&gt;</span>
          <router-link :to="crumbCategoryLink" class="crumb-link">{{ crumbCategoryName }}</router-link>
          <span class="crumb-sep">&gt;</span>
          <span class="crumb-current">{{ news ? news.title : '详情' }}</span>
        </div>

        <div v-if="loading" class="loading">
          <van-loading size="24" color="#1989fa">加载中...</van-loading>
        </div>

        <div v-else-if="news" class="news-card">
          <div class="news-header">
            <h1 class="news-title">{{ news.title }}</h1>
            <div class="news-meta">
              <span><van-icon name="clock-o" /> {{ formatDate(news.createTime) }}</span>
            </div>
          </div>
          <van-image
            v-if="news.coverUrl"
            :src="news.coverUrl"
            class="news-cover"
            fit="cover"
          />
          <div class="news-content" v-html="news.content || '暂无内容'"></div>
        </div>

        <van-empty v-else description="新闻不存在或已删除" />
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getNewsList, getEventsList, getAnnouncementList, getHomepageSections } from '@/api/home'

export default {
  name: 'NewsDetail',
  components: { Header },
  data() {
    return {
      news: null,
      loading: true
    }
  },
  computed: {
    newsId() {
      return this.$route.params.id
    },
    detailType() {
      return this.$route.query.type || 'news'
    },
    crumbCategoryName() {
      const map = {
        'announcement': '通知公告',
        'events': '重大活动',
        'policy': '政策法规',
        'news': '新闻动态'
      }
      return map[this.detailType] || '新闻动态'
    },
    crumbCategoryLink() {
      const map = {
        'announcement': '/news/announcements',
        'events': '/news/center?type=events',
        'policy': '/news/center?type=policy',
        'news': '/news/center'
      }
      return map[this.detailType] || '/news/center'
    }
  },
  created() {
    this.fetchNews()
  },
  watch: {
    '$route'(to, from) {
      if (to.params.id !== from.params.id || to.query.type !== from.query.type) {
        this.news = null
        this.fetchNews()
      }
    }
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
    async fetchNews() {
      this.loading = true
      // 优先从 sessionStorage 读取上次缓存的详情数据，立即展示
      const cached = sessionStorage.getItem('news_detail_data')
      if (cached) {
        try {
          this.news = JSON.parse(cached)
        } catch (e) {
          this.news = null
        }
      }
      // 再请求接口拿最新数据
      try {
        let apiCall
        if (this.detailType === 'announcement') {
          apiCall = getAnnouncementList()
        } else if (this.detailType === 'events') {
          apiCall = getEventsList()
        } else if (this.detailType === 'policy') {
          apiCall = getHomepageSections(1)
        } else {
          apiCall = getNewsList()
        }
        const res = await apiCall
        const data = res.data || res
        const list = Array.isArray(data) ? data : (data.list || data.records || [])
        const found = list.find((item) => String(item.id) === String(this.newsId))
        if (found) {
          this.news = found
        }
      } catch (error) {
        // 接口失败时保留缓存数据
      } finally {
        this.loading = false
        // 读取后清除缓存，避免残留
        sessionStorage.removeItem('news_detail_data')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.news-detail-page {
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

.breadcrumb {
  font-size: 13px;
  color: #909399;
  padding: 12px 0;
  margin-bottom: 8px;

  .crumb-link {
    color: #409eff;
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }
  .crumb-sep {
    margin: 0 6px;
    color: #c0c4cc;
  }
  .crumb-current {
    color: #303133;
  }
}

.loading {
  padding: 60px 0;
  text-align: center;
}

.news-card {
  border-radius: 8px;

  .news-header {
    border-bottom: 1px solid #ebeef5;
    padding-bottom: 16px;
    margin-bottom: 20px;
  }

  .news-title {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
    line-height: 1.4;
    margin: 0 0 12px;
  }

  .news-meta {
    font-size: 13px;
    color: #909399;

    span {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }

  .news-cover {
    width: 100%;
    max-height: 360px;
    border-radius: 6px;
    margin-bottom: 20px;
  }

  .news-content {
    font-size: 15px;
    line-height: 1.8;
    color: #303133;
    word-break: break-word;
  }
}

/* 移动端适配:768px 以下 */
@media (max-width: 768px) {
  .container { width: 100%; max-width: 100%; padding: 12px; }
  .news-card .news-title { font-size: 18px; }
  .news-card .news-cover { max-height: 200px; }
  .news-content ::v-deep table { max-width: 100%; display: block; overflow-x: auto; }
  .news-content ::v-deep iframe { max-width: 100%; }
}
</style>

<style>
/* 富文本内容样式(非 scoped,确保 v-html 渲染的图片居中生效) */
.news-content p { margin: 0 0 12px; }
.news-content img {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 0 auto;
}
.news-content p[style*="text-align: center"] img {
  display: inline-block;
  margin: 0 auto;
}
</style>
