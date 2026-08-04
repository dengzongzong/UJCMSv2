<template>
  <div class="live-center-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <!-- 标题栏 -->
        <div class="page-header">
          <h1 class="page-title">直播中心</h1>
          <p class="page-subtitle">直播课程与回放，尽收眼底</p>
        </div>

        <!-- 状态筛选 -->
        <div class="filter-tabs">
          <div
            v-for="tab in tabs"
            :key="tab.value"
            :class="['filter-tab', { active: activeTab === tab.value }]"
            @click="activeTab = tab.value"
          >
            {{ tab.label }}
            <span v-if="tab.value !== 'all'" class="tab-count">{{ countBy(tab.value) }}</span>
          </div>
        </div>

        <!-- 直播列表 -->
        <div v-if="loading" class="loading-wrap">
          <van-loading size="28" color="#c8102e">加载中...</van-loading>
        </div>
        <div v-else-if="filteredList.length" class="live-grid">
          <div
            v-for="item in filteredList"
            :key="item.id"
            class="live-card"
            @click="goLive(item.id)"
          >
            <div class="live-cover">
              <img v-if="item.coverUrl" :src="resolveImg(item.coverUrl)" :alt="item.title" />
              <div v-else class="cover-placeholder">
                <van-icon name="video-o" size="36" color="#c8c9cc" />
              </div>
              <div class="cover-status">
                <van-tag :type="statusType(item.status)" size="medium" color="rgba(0,0,0,0.55)">
                  {{ statusText(item.status) }}
                </van-tag>
              </div>
              <div v-if="item.status === 2 && item.replayUrl" class="replay-badge">
                <van-icon name="replay" /> 回放
              </div>
            </div>
            <div class="live-info">
              <div class="live-title">{{ item.title }}</div>
              <div class="live-course">{{ item.courseName || '未绑定课程' }}</div>
              <div class="live-meta">
                <span v-if="item.anchorName"><van-icon name="manager-o" /> {{ item.anchorName }}</span>
                <span><van-icon name="clock-o" /> {{ item.startTime || '-' }}</span>
              </div>
              <div class="live-stats">
                <span><van-icon name="eye-o" /> {{ item.viewCount || 0 }} 观看</span>
                <span class="online"><van-icon name="friends-o" /> {{ item.onlineCount || 0 }} 在线</span>
              </div>
            </div>
          </div>
        </div>

        <van-empty v-if="!loading && filteredList.length === 0" description="暂无相关直播场次" />
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getLiveList } from '@/api/live'
import { resolveImg } from '@/utils/apiBase'

export default {
  name: 'LiveCenter',
  components: { Header },
  data() {
    return {
      loading: false,
      activeTab: 'all',
      list: [],
      tabs: [
        { label: '全部', value: 'all' },
        { label: '直播中', value: 1 },
        { label: '即将开始', value: 0 },
        { label: '已结束', value: 2 }
      ]
    }
  },
  computed: {
    filteredList() {
      if (this.activeTab === 'all') return this.list
      return this.list.filter((it) => it.status === this.activeTab)
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    resolveImg,
    countBy(status) {
      return this.list.filter((it) => it.status === status).length
    },
    statusText(status) {
      return ['未开始', '直播中', '已结束', '已取消'][status] || ''
    },
    statusType(status) {
      return ['default', 'danger', 'success', 'default'][status] || 'default'
    },
    fetchList() {
      this.loading = true
      getLiveList()
        .then((res) => {
          this.list = res.data || []
        })
        .catch(() => {
          this.list = []
        })
        .finally(() => {
          this.loading = false
        })
    },
    goLive(id) {
      this.$router.push({ path: '/live/' + id }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.live-center-page {
  min-height: 100vh;
  background: #f5f6f8;
}

.page-body {
  padding: 24px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 20px;

  .page-title {
    font-size: 24px;
    font-weight: 600;
    color: #333;
  }

  .page-subtitle {
    margin-top: 6px;
    font-size: 14px;
    color: #969799;
  }
}

.filter-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  .filter-tab {
    padding: 7px 18px;
    border-radius: 18px;
    background: #fff;
    border: 1px solid #ebedf0;
    font-size: 14px;
    color: #666;
    cursor: pointer;
    transition: all 0.2s;

    &.active {
      background: #c8102e;
      border-color: #c8102e;
      color: #fff;
    }

    .tab-count {
      margin-left: 4px;
      font-size: 12px;
      opacity: 0.8;
    }
  }
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.live-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.live-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }
}

.live-cover {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #f0f0f0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .cover-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .cover-status {
    position: absolute;
    top: 8px;
    left: 8px;
  }

  .replay-badge {
    position: absolute;
    bottom: 8px;
    right: 8px;
    background: rgba(0, 0, 0, 0.55);
    color: #fff;
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 10px;
  }
}

.live-info {
  padding: 12px;

  .live-title {
    font-size: 15px;
    font-weight: 500;
    color: #333;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .live-course {
    margin-top: 4px;
    font-size: 13px;
    color: #1989fa;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .live-meta {
    margin-top: 8px;
    font-size: 12px;
    color: #969799;
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }

  .live-stats {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid #f5f5f5;
    font-size: 12px;
    color: #969799;
    display: flex;
    justify-content: space-between;

    .online {
      color: #c8102e;
    }
  }
}

@media (max-width: 768px) {
  .container {
    padding: 0 12px;
  }

  .filter-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;

    .filter-tab {
      white-space: nowrap;
    }
  }
}
</style>
