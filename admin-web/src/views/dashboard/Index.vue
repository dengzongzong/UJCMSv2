<template>
  <div class="app-container dashboard-container">
    <el-card class="welcome-card" shadow="never">
      <div class="welcome">
        <el-avatar :size="56" icon="el-icon-user-solid" class="welcome-avatar"></el-avatar>
        <div class="welcome-text">
          <h2>{{ greeting }}，{{ adminName }}！</h2>
          <p>欢迎使用国际职业能力标准人才评测网管理后台，祝您工作顺利。</p>
        </div>
        <div class="welcome-date">
          <i class="el-icon-date"></i>
          {{ today }}
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="stat-row">
      <el-col v-for="item in stats" :key="item.key" :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card-body">
            <div class="stat-icon" :style="{ background: item.color }">
              <i :class="item.icon"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :md="16">
        <el-card shadow="never" class="panel-card">
          <div slot="header" class="panel-header">
            <span>快捷入口</span>
          </div>
          <div class="quick-entry">
            <div
              v-for="entry in quickEntries"
              :key="entry.path"
              class="entry-item"
              @click="$router.push(entry.path)"
            >
              <i :class="entry.icon" :style="{ color: entry.color }"></i>
              <span>{{ entry.label }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="panel-card">
          <div slot="header" class="panel-header">
            <span>系统公告</span>
          </div>
          <ul class="notice-list">
            <li v-for="(notice, idx) in notices" :key="idx">
              <el-tag size="mini" :type="notice.type">{{ notice.tag }}</el-tag>
              <span class="notice-text">{{ notice.text }}</span>
            </li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { dashboardStats, dashboardAnnouncements } from '@/api/dashboard'

export default {
  name: 'DashboardIndex',
  data() {
    return {
      stats: [
        { key: 'student', label: '学生总数', value: '--', icon: 'el-icon-s-custom', color: '#409eff' },
        { key: 'course', label: '课程总数', value: '--', icon: 'el-icon-reading', color: '#67c23a' },
        { key: 'exam', label: '考试总数', value: '--', icon: 'el-icon-document', color: '#e6a23c' },
        { key: 'question', label: '题目总数', value: '--', icon: 'el-icon-edit', color: '#f56c6c' }
      ],
      quickEntries: [
        { label: '学生管理', path: '/student/list', icon: 'el-icon-s-custom', color: '#409eff' },
        { label: '课程管理', path: '/course/list', icon: 'el-icon-reading', color: '#67c23a' },
        { label: '视频管理', path: '/video/list', icon: 'el-icon-video-camera', color: '#909399' },
        { label: '题库管理', path: '/question/list', icon: 'el-icon-edit', color: '#f56c6c' },
        { label: '考试管理', path: '/exam/list', icon: 'el-icon-document', color: '#e6a23c' },
        { label: '系统设置', path: '/setting/profession', icon: 'el-icon-s-tools', color: '#9254de' }
      ],
      notices: []
    }
  },
  computed: {
    ...mapGetters(['adminInfo']),
    adminName() {
      if (this.adminInfo) {
        return this.adminInfo.username || this.adminInfo.account || '管理员'
      }
      return '管理员'
    },
    greeting() {
      const h = new Date().getHours()
      if (h < 6) return '凌晨好'
      if (h < 9) return '早上好'
      if (h < 12) return '上午好'
      if (h < 14) return '中午好'
      if (h < 18) return '下午好'
      return '晚上好'
    },
    today() {
      const d = new Date()
      const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
      return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${week}`
    }
  },
  created() {
    this.fetchStats()
    this.fetchNotices()
  },
  methods: {
    fetchStats() {
      dashboardStats()
        .then((res) => {
          const data = res.data || res || {}
          this.stats.forEach((s) => {
            if (data[s.key] !== undefined) {
              s.value = data[s.key]
            }
          })
        })
        .catch(() => {
          // 接口未就绪时显示静态数据
          this.stats.forEach((s) => {
            s.value = 0
          })
        })
    },
    fetchNotices() {
      dashboardAnnouncements()
        .then((res) => {
          const data = res.data || {}
          const records = data.records || data.list || data.rows || []
          this.notices = records.map((item, idx) => ({
            tag: '公告',
            type: idx === 0 ? 'success' : (idx === 1 ? 'info' : ''),
            text: item.title + (item.content ? '：' + item.content : '')
          }))
        })
        .catch(() => {
          this.notices = []
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  .welcome-card {
    margin-bottom: 16px;

    .welcome {
      display: flex;
      align-items: center;

      .welcome-avatar {
        background-color: #409eff;
      }

      .welcome-text {
        margin-left: 16px;
        flex: 1;

        h2 {
          font-size: 18px;
          color: #303133;
          margin-bottom: 6px;
        }

        p {
          font-size: 13px;
          color: #909399;
        }
      }

      .welcome-date {
        color: #909399;
        font-size: 13px;

        i {
          margin-right: 4px;
        }
      }
    }
  }

  .stat-row {
    margin-bottom: 16px;

    .stat-card {
      margin-bottom: 16px;

      .stat-card-body {
        display: flex;
        align-items: center;
      }

      .stat-icon {
        width: 56px;
        height: 56px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        font-size: 28px;
        margin-right: 16px;
      }

      .stat-info {
        .stat-value {
          font-size: 26px;
          font-weight: 600;
          color: #303133;
          line-height: 1.2;
        }

        .stat-label {
          font-size: 13px;
          color: #909399;
          margin-top: 4px;
        }
      }
    }
  }

  .content-row {
    .panel-card {
      margin-bottom: 16px;

      .panel-header {
        font-weight: 600;
      }

      .quick-entry {
        display: flex;
        flex-wrap: wrap;

        .entry-item {
          width: 33.33%;
          padding: 18px 0;
          text-align: center;
          cursor: pointer;
          transition: all 0.2s;
          border-radius: 6px;

          &:hover {
            background: #f5f7fa;
          }

          i {
            font-size: 30px;
            display: block;
            margin-bottom: 8px;
          }

          span {
            font-size: 13px;
            color: #606266;
          }
        }
      }

      .notice-list {
        li {
          padding: 10px 0;
          border-bottom: 1px dashed #ebeef5;
          display: flex;
          align-items: center;

          &:last-child {
            border-bottom: none;
          }

          .notice-text {
            margin-left: 8px;
            font-size: 13px;
            color: #606266;
          }
        }
      }
    }
  }
}
</style>
