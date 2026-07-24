<template>
  <div class="news-center-page">
    <Header />
    <div class="page-body">
      <div class="container">
        <!-- 面包屑 -->
        <div class="breadcrumb">
          <span>您当前所在位置：</span>
          <router-link to="/" class="crumb-link">主页</router-link>
          <span class="crumb-sep">&gt;</span>
          <span class="crumb-current">中心动态</span>
        </div>

        <div class="main-layout">
          <!-- 左侧分类菜单 -->
          <aside class="sidebar">
            <ul class="category-list">
              <li
                v-for="cat in categories"
                :key="cat.key"
                :class="['category-item', { active: activeCategory === cat.key }]"
                @click="switchCategory(cat.key)"
              >
                {{ cat.label }}
              </li>
            </ul>
          </aside>

          <!-- 右侧内容区 -->
          <main class="content-area">
            <div class="content-header">
              <span class="content-title">{{ currentCategoryLabel }}</span>
            </div>
            <div class="content-body">
              <ul v-if="list.length > 0" class="news-list">
                <li v-for="item in list" :key="item.id" class="news-item" @click="goDetail(item)">
                  <span class="news-marker"></span>
                  <span class="news-title">{{ item.title }}</span>
                  <span class="news-date">{{ formatDate(item.publishTime || item.createTime) }}</span>
                </li>
              </ul>
              <div v-else class="empty-state">暂无数据</div>
            </div>
          </main>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getNewsList, getEventsList, getAnnouncements, getHomepageSections } from '@/api/home'

export default {
  name: 'NewsCenter',
  components: { Header },
  data() {
    return {
      activeCategory: 'news',
      categories: [
        { key: 'news', label: '新闻动态' },
        { key: 'events', label: '重大活动' },
        { key: 'announcement', label: '通知公告' },
        { key: 'policy', label: '政策法规' }
      ],
      newsList: [],
      eventsList: [],
      announcementList: [],
      policyList: []
    }
  },
  computed: {
    currentCategoryLabel() {
      const cat = this.categories.find(c => c.key === this.activeCategory)
      return cat ? cat.label : '中心动态'
    },
    list() {
      const map = {
        news: this.newsList,
        events: this.eventsList,
        announcement: this.announcementList,
        policy: this.policyList
      }
      return map[this.activeCategory] || []
    }
  },
  mounted() {
    const tab = this.$route.query.tab
    if (tab && this.categories.some(c => c.key === tab)) {
      this.activeCategory = tab
    }
    this.fetchAll()
  },
  methods: {
    async fetchAll() {
      try {
        const [newsRes, eventsRes, annoRes, sectionsRes] = await Promise.all([
          getNewsList(),
          getEventsList(),
          getAnnouncements(),
          getHomepageSections()
        ])
        this.newsList = this.extractList(newsRes)
        this.eventsList = this.extractList(eventsRes)
        this.announcementList = this.extractList(annoRes)
        const sectionsData = sectionsRes.data || sectionsRes || []
        const sectionsList = Array.isArray(sectionsData) ? sectionsData : (sectionsData.list || [])
        this.policyList = sectionsList.filter(item => item.type === 1)
      } catch (e) {
        // ignore
      }
    },
    extractList(res) {
      const data = res.data || res
      if (Array.isArray(data)) return data
      return data.list || data.records || []
    },
    switchCategory(key) {
      this.activeCategory = key
    },
    goDetail(item) {
      const typeMap = {
        news: 'news',
        events: 'events',
        announcement: 'announcement',
        policy: 'policy'
      }
      const type = typeMap[this.activeCategory] || 'news'
      this.$router.push({ path: '/news/detail/' + item.id, query: { type } })
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      const d = new Date(dateStr)
      if (isNaN(d.getTime())) return dateStr
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    }
  }
}
</script>

<style lang="scss" scoped>
$primary-red: #c41e3a;
$primary-red-dark: #a01530;

.news-center-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.page-body {
  padding-top: var(--header-height, 198px);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px 40px;
}

/* 面包屑 */
.breadcrumb {
  padding: 16px 0;
  font-size: 13px;
  color: #999;
  .crumb-link {
    color: $primary-red;
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }
  .crumb-sep {
    margin: 0 6px;
    color: #ccc;
  }
  .crumb-current {
    color: #333;
  }
}

/* 两栏布局 */
.main-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

/* 左侧边栏 */
.sidebar {
  flex-shrink: 0;
  width: 200px;
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.category-list {
  list-style: none;
  margin: 0;
  padding: 0;
  .category-item {
    padding: 14px 20px;
    font-size: 15px;
    color: #333;
    cursor: pointer;
    border-bottom: 1px solid #f0f0f0;
    transition: all 0.2s;
    &:last-child { border-bottom: none; }
    &:hover {
      color: $primary-red;
      background: #fff5f6;
    }
    &.active {
      color: #fff;
      background: $primary-red;
      font-weight: 500;
      position: relative;
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        bottom: 0;
        width: 4px;
        background: $primary-red-dark;
      }
    }
  }
}

/* 右侧内容区 */
.content-area {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.content-header {
  padding: 16px 24px;
  border-bottom: 2px solid $primary-red;
  .content-title {
    font-size: 18px;
    font-weight: 600;
    color: #333;
  }
}

.content-body {
  padding: 8px 24px;
}

.news-list {
  list-style: none;
  margin: 0;
  padding: 0;
  .news-item {
    display: flex;
    align-items: center;
    padding: 14px 0;
    border-bottom: 1px dashed #eee;
    cursor: pointer;
    transition: background 0.2s;
    &:last-child { border-bottom: none; }
    &:hover {
      background: #fafafa;
      .news-title { color: $primary-red; }
    }
    .news-marker {
      flex-shrink: 0;
      width: 6px;
      height: 6px;
      background: $primary-red;
      margin-right: 10px;
    }
    .news-title {
      flex: 1;
      font-size: 14px;
      color: #333;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-right: 12px;
      transition: color 0.2s;
    }
    .news-date {
      flex-shrink: 0;
      font-size: 13px;
      color: #999;
    }
  }
}

.empty-state {
  padding: 60px 0;
  text-align: center;
  color: #ccc;
  font-size: 14px;
}

/* 响应式 */
@media (max-width: 768px) {
  .container { padding: 0 12px 20px; }
  .main-layout {
    flex-direction: column;
  }
  .sidebar {
    width: 100%;
    .category-list {
      display: flex;
      overflow-x: auto;
      .category-item {
        white-space: nowrap;
        border-bottom: none;
        border-right: 1px solid #f0f0f0;
        padding: 10px 16px;
        font-size: 14px;
        &.active::before { display: none; }
      }
    }
  }
}
</style>
