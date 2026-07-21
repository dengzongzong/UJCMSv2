<template>
  <div class="search-page">
    <Header />
    <div class="page-body">
      <div class="container">
        <!-- 搜索框 -->
        <div class="search-box">
          <input v-model="keyword" type="text" placeholder="请输入关键字" class="search-input" @keyup.enter="doSearch" />
          <button class="search-btn" @click="doSearch">搜索</button>
        </div>

        <!-- 搜索结果 -->
        <div v-if="searched" class="search-results">
          <div class="result-summary">
            搜索"<b>{{ searchedKeyword }}</b>"，共找到 {{ totalCount }} 条结果
          </div>

          <div v-if="loading" class="loading-state">
            <van-loading size="24" color="#1989fa">搜索中...</van-loading>
          </div>

          <div v-else>
            <!-- Tab 切换 -->
            <div class="result-tabs">
              <div
                v-for="tab in tabs"
                :key="tab.key"
                :class="['result-tab', { active: activeTab === tab.key }]"
                @click="activeTab = tab.key"
              >
                {{ tab.label }}
                <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
              </div>
            </div>

            <!-- 新闻动态 -->
            <div v-if="activeTab === 'news'" class="result-section">
              <div v-if="results.news && results.news.length > 0">
                <div v-for="item in results.news" :key="item.id" class="result-item" @click="goNewsDetail(item)">
                  <span class="result-marker"></span>
                  <span class="result-title">{{ item.title }}</span>
                  <span class="result-date">{{ formatDate(item.createTime) }}</span>
                </div>
              </div>
              <div v-else class="empty-state">暂无相关新闻</div>
            </div>

            <!-- 通知公告 -->
            <div v-if="activeTab === 'announcements'" class="result-section">
              <div v-if="results.announcements && results.announcements.length > 0">
                <div v-for="item in results.announcements" :key="item.id" class="result-item" @click="goAnnouncementDetail(item)">
                  <span class="result-marker"></span>
                  <span class="result-title">{{ item.title }}</span>
                  <span class="result-date">{{ formatDate(item.createTime) }}</span>
                </div>
              </div>
              <div v-else class="empty-state">暂无相关公告</div>
            </div>

            <!-- 课程 -->
            <div v-if="activeTab === 'courses'" class="result-section">
              <div v-if="results.courses && results.courses.length > 0">
                <div v-for="item in results.courses" :key="item.id" class="result-item" @click="goCourseDetail(item)">
                  <span class="result-marker"></span>
                  <span class="result-title">{{ item.name || item.title }}</span>
                  <span class="result-date">课程</span>
                </div>
              </div>
              <div v-else class="empty-state">暂无相关课程</div>
            </div>

            <!-- 考试 -->
            <div v-if="activeTab === 'exams'" class="result-section">
              <div v-if="results.exams && results.exams.length > 0">
                <div v-for="item in results.exams" :key="item.id" class="result-item" @click="goExamDetail(item)">
                  <span class="result-marker"></span>
                  <span class="result-title">{{ item.name || item.title }}</span>
                  <span class="result-date">考试</span>
                </div>
              </div>
              <div v-else class="empty-state">暂无相关考试</div>
            </div>

            <!-- 政策法规 -->
            <div v-if="activeTab === 'policies'" class="result-section">
              <div v-if="results.policies && results.policies.length > 0">
                <div v-for="item in results.policies" :key="item.id" class="result-item" @click="goPolicyDetail(item)">
                  <span class="result-marker"></span>
                  <span class="result-title">{{ item.title }}</span>
                  <span class="result-date">{{ formatDate(item.createTime) }}</span>
                </div>
              </div>
              <div v-else class="empty-state">暂无相关政策法规</div>
            </div>
          </div>
        </div>

        <div v-else class="search-hint">
          请输入关键词进行搜索
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { searchAll } from '@/api/home'

export default {
  name: 'SearchPage',
  components: { Header },
  data() {
    return {
      keyword: '',
      searchedKeyword: '',
      searched: false,
      loading: false,
      activeTab: 'news',
      results: {
        news: [],
        announcements: [],
        courses: [],
        exams: [],
        policies: []
      }
    }
  },
  computed: {
    tabs() {
      return [
        { key: 'news', label: '新闻动态', count: (this.results.news || []).length },
        { key: 'announcements', label: '通知公告', count: (this.results.announcements || []).length },
        { key: 'courses', label: '课程', count: (this.results.courses || []).length },
        { key: 'exams', label: '考试', count: (this.results.exams || []).length },
        { key: 'policies', label: '政策法规', count: (this.results.policies || []).length }
      ]
    },
    totalCount() {
      return (this.results.news || []).length
        + (this.results.announcements || []).length
        + (this.results.courses || []).length
        + (this.results.exams || []).length
        + (this.results.policies || []).length
    }
  },
  mounted() {
    const kw = this.$route.query.keyword
    if (kw) {
      this.keyword = kw
      this.doSearch()
    }
  },
  watch: {
    '$route'(to) {
      if (to.query.keyword && to.query.keyword !== this.searchedKeyword) {
        this.keyword = to.query.keyword
        this.doSearch()
      }
    }
  },
  methods: {
    async doSearch() {
      const kw = (this.keyword || '').trim()
      if (!kw) return
      this.searchedKeyword = kw
      this.searched = true
      this.loading = true
      try {
        const res = await searchAll(kw)
        const data = res.data || res || {}
        this.results = {
          news: data.news || [],
          announcements: data.announcements || [],
          courses: data.courses || [],
          exams: data.exams || [],
          policies: data.policies || []
        }
        // 自动跳到第一个有结果的 tab
        const firstWithResult = this.tabs.find(t => t.count > 0)
        if (firstWithResult) {
          this.activeTab = firstWithResult.key
        }
      } catch (e) {
        this.results = { news: [], announcements: [], courses: [], exams: [], policies: [] }
      } finally {
        this.loading = false
      }
    },
    goNewsDetail(item) {
      this.$router.push({ path: '/news/detail/' + item.id, query: { type: 'news' } })
    },
    goAnnouncementDetail(item) {
      this.$router.push({ path: '/news/detail/' + item.id, query: { type: 'announcement' } })
    },
    goCourseDetail(item) {
      this.$router.push('/course/' + item.id)
    },
    goExamDetail(item) {
      this.$router.push('/exam/' + item.id)
    },
    goPolicyDetail(item) {
      this.$router.push({ path: '/news/detail/' + item.id, query: { type: 'policy' } })
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

.search-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.page-body {
  padding-top: var(--header-height, 218px);
  padding-bottom: 40px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* 搜索框 */
.search-box {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  .search-input {
    flex: 1;
    height: 40px;
    padding: 0 14px;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 14px;
    outline: none;
    &:focus { border-color: $primary-red; }
  }
  .search-btn {
    padding: 0 24px;
    height: 40px;
    background: $primary-red;
    color: #fff;
    border: none;
    border-radius: 4px;
    font-size: 14px;
    cursor: pointer;
    &:hover { opacity: 0.9; }
  }
}

.result-summary {
  padding: 12px 0;
  font-size: 14px;
  color: #666;
  b { color: $primary-red; }
}

/* Tab */
.result-tabs {
  display: flex;
  gap: 0;
  border-bottom: 2px solid #eee;
  margin-bottom: 16px;
  .result-tab {
    padding: 10px 20px;
    font-size: 15px;
    color: #666;
    cursor: pointer;
    border-bottom: 2px solid transparent;
    margin-bottom: -2px;
    transition: all 0.2s;
    &:hover { color: $primary-red; }
    &.active {
      color: $primary-red;
      border-bottom-color: $primary-red;
      font-weight: 600;
    }
    .tab-count {
      margin-left: 4px;
      font-size: 12px;
      color: #999;
    }
  }
}

.result-section {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  padding: 0 20px;
}

.result-item {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px dashed #eee;
  cursor: pointer;
  transition: background 0.2s;
  &:last-child { border-bottom: none; }
  &:hover {
    background: #fafafa;
    .result-title { color: $primary-red; }
  }
  .result-marker {
    flex-shrink: 0;
    width: 6px;
    height: 6px;
    background: $primary-red;
    margin-right: 10px;
  }
  .result-title {
    flex: 1;
    font-size: 14px;
    color: #333;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-right: 12px;
    transition: color 0.2s;
  }
  .result-date {
    flex-shrink: 0;
    font-size: 13px;
    color: #999;
  }
}

.loading-state {
  padding: 60px 0;
  text-align: center;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
  color: #ccc;
  font-size: 14px;
}

.search-hint {
  padding: 60px 0;
  text-align: center;
  color: #ccc;
  font-size: 15px;
}

@media (max-width: 768px) {
  .container { padding: 0 12px; }
  .result-tabs {
    overflow-x: auto;
    .result-tab { white-space: nowrap; padding: 8px 14px; font-size: 14px; }
  }
  .result-section { padding: 0 12px; }
}
</style>
