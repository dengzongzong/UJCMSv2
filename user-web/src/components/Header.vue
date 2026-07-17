<template>
  <div class="site-header">
    <!-- 返回按钮 -->
    <div v-if="showBack" class="back-button" @click="handleBack">
      <van-icon name="arrow-left" size="20" />
      <span>返回</span>
    </div>

    <!-- 第一行：透明背景图 + Logo + 搜索框 + 用户信息 -->
    <div class="header-top">
      <div class="header-bg-overlay"></div>
      <div class="header-container">
        <div class="logo" @click="$router.push('/home')">
          <img src="/images/log.jpg" alt="logo" class="logo-img" />
          <div class="logo-text">
            <div class="logo-title">国际职业能力标准人才评测网</div>
            <div class="logo-subtitle">International Professional Competency Standard Talent Evaluation Network</div>
          </div>
        </div>
        <div class="header-right">
          <!-- 搜索框 -->
          <div class="header-search">
            <input v-model="searchKeyword" type="text" placeholder="请输入关键字" class="search-input" @keyup.enter="onSearch" />
            <button class="search-btn" @click="onSearch">搜索</button>
          </div>
          <!-- 已登录:显示用户信息 + 专业切换 + 退出 -->
          <template v-if="isLoggedIn">
            <span class="welcome">你好，{{ nickname }}</span>
            <div class="subject-switch" @click="showSubjectPicker = true">
              <van-icon name="location-o" size="14" color="#c41e3a" />
              <span class="subject-name">{{ currentSubjectName }}</span>
              <van-icon name="arrow-down" size="12" color="#999" />
            </div>
            <span class="logout" @click="handleLogout">退出</span>
          </template>
          <!-- 未登录:显示登录/注册按钮 -->
          <template v-else>
            <router-link to="/login" class="login-btn">登录</router-link>
            <router-link to="/register" class="register-btn">注册</router-link>
          </template>
        </div>
      </div>
    </div>

    <!-- 第二行：菜单导航（红色背景，不铺满） -->
    <div class="header-nav">
      <div class="nav-container">
        <nav class="nav-menu">
          <router-link to="/" class="nav-item">网站首页</router-link>
          <router-link to="/about" class="nav-item">关于我们</router-link>
          <router-link to="/news/center" class="nav-item">中心动态</router-link>
          <router-link to="/course/my" class="nav-item">学习中心</router-link>
          <router-link to="/exam" class="nav-item">考试中心</router-link>
          <router-link to="/cooperation" class="nav-item">合作单位</router-link>
          <router-link to="/certificate" class="nav-item">证书查询</router-link>
          <router-link to="/profile" class="nav-item">个人中心</router-link>
        </nav>
      </div>
    </div>

    <!-- 专业选择弹窗 -->
    <van-popup v-model="showSubjectPicker" position="bottom" round :style="{ maxHeight: '70%' }">
      <div class="subject-picker">
        <div class="picker-header">
          <span class="picker-title">选择专业</span>
          <van-icon name="cross" size="18" color="#999" @click="showSubjectPicker = false" />
        </div>
        <van-search
          v-model="subjectKeyword"
          placeholder="输入专业名称搜索"
          shape="round"
          show-action
          @clear="subjectKeyword = ''"
        />
        <div class="subject-list">
          <div
            v-for="item in filteredSubjects"
            :key="item.id"
            class="subject-option"
            :class="{ active: item.id === currentSubjectId }"
            @click="handleSelectSubject(item)"
          >
            <van-icon :name="item.icon || 'bookmark-o'" size="20" />
            <span class="option-name">{{ item.name }}</span>
            <van-icon v-if="item.id === currentSubjectId" name="success" color="#1989fa" />
          </div>
          <van-empty v-if="filteredSubjects.length === 0" description="未找到匹配专业" />
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script>
import { Toast, Dialog } from 'vant'
import { getProfessions } from '@/api/home'
import { chooseSubject } from '@/api/auth'

export default {
  name: 'SiteHeader',
  props: {
    showBack: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      showSubjectPicker: false,
      subjectList: [],
      subjectKeyword: '',
      switching: false,
      searchKeyword: ''
    }
  },
  computed: {
    nickname() {
      const info = this.$store.getters.userInfo || {}
      if (info.nickname) return info.nickname
      if (info.username) return info.username
      if (info.phone) return '学员' + String(info.phone).slice(-4)
      return '学员'
    },
    isLoggedIn() {
      return this.$store.getters.isLoggedIn
    },
    currentSubjectName() {
      return this.$store.getters.professionName || '选择专业'
    },
    currentSubjectId() {
      return this.$store.getters.professionId
    },
    filteredSubjects() {
      if (!this.subjectKeyword) return this.subjectList
      var kw = this.subjectKeyword.toLowerCase()
      return this.subjectList.filter(function (s) {
        return s.name && s.name.toLowerCase().indexOf(kw) !== -1
      })
    }
  },
  mounted() {
    this.fetchSubjects()
    this.$nextTick(() => {
      this.updateHeaderHeight()
      // 使用 ResizeObserver 监听 header 元素尺寸变化
      this.observer = new ResizeObserver(() => {
        this.updateHeaderHeight()
      })
      const header = this.$el.querySelector('.site-header')
      if (header) {
        this.observer.observe(header)
      }
    })
    // 同时监听 window resize，确保浏览器缩放时也能更新
    window.addEventListener('resize', this.updateHeaderHeight)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.updateHeaderHeight)
    if (this.observer) {
      this.observer.disconnect()
    }
  },
  methods: {
    updateHeaderHeight() {
      const header = this.$el.querySelector('.site-header')
      if (header) {
        const height = header.offsetHeight
        document.documentElement.style.setProperty('--header-height', height + 'px')
      }
    },
    async fetchSubjects() {
      try {
        const res = await getProfessions()
        const data = res.data || res
        this.subjectList = Array.isArray(data) ? data : (data.list || [])
        // 后端无数据:留空,提示用户去联系管理员
        this.$nextTick(() => this.updateHeaderHeight())
      } catch (error) {
        this.subjectList = []
      }
    },
    async handleSelectSubject(subject) {
      if (this.switching) return
      this.switching = true
      try {
        // 调用后端接口更新学生表的专业字段
        await chooseSubject({ professionId: subject.id })
        const profession = {
          id: subject.id,
          name: subject.name,
          professionId: subject.id,
          professionName: subject.name
        }
        this.$store.dispatch('setCurrentSubject', profession)
        this.showSubjectPicker = false
        this.subjectKeyword = ''
        Toast.success('已切换到：' + subject.name)
        // 通知首页刷新课程
        this.$emit('subject-change', profession)
      } catch (e) {
        Toast.fail('切换专业失败，请重试')
      } finally {
        this.switching = false
      }
    },
    handleLogout() {
      Dialog.confirm({
        title: '提示',
        message: '确认退出登录吗？',
        confirmButtonText: '退出',
        confirmButtonColor: '#ee0a24'
      })
        .then(() => {
          this.$store.dispatch('logout')
          Toast.success('已退出登录')
          this.$router.replace({ name: 'Login' }).catch(() => {})
        })
        .catch(() => {})
    },
    handleBack() {
      this.$router.back()
    },
    onSearch() {
      if (this.searchKeyword && this.searchKeyword.trim()) {
        this.$router.push({ path: '/news/list', query: { keyword: this.searchKeyword.trim() } }).catch(() => {})
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.site-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  z-index: 1000;
}

.back-button {
  position: fixed;
  top: 10px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #1989fa;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 14px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 3000;
  transition: color 0.2s, background 0.2s;

  &:hover {
    color: #409eff;
    background: #f5f7fa;
  }
}

.header-container {
  max-width: 1600px;
  height: 100%;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
}

/* 第一行：透明背景图 + Logo + 搜索框（白底） */
.header-top {
  height: 120px;
  border-bottom: 1px solid #f0f0f0;
  position: relative;
  overflow: hidden;

  .header-bg-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(196, 30, 58, 0.03) 0%, rgba(25, 137, 250, 0.03) 100%);
    pointer-events: none;
  }

  .header-container {
    justify-content: space-between;
    position: relative;
    z-index: 1;
  }
}

/* 搜索框 */
.header-search {
  display: flex;
  align-items: center;
  margin-right: 16px;

  .search-input {
    width: 180px;
    height: 32px;
    padding: 0 12px;
    border: 1px solid #ddd;
    border-radius: 4px 0 0 4px;
    font-size: 13px;
    color: #333;
    outline: none;
    background: #fafafa;
    transition: border-color 0.2s;

    &:focus {
      border-color: #c41e3a;
      background: #fff;
    }
  }

  .search-btn {
    height: 32px;
    padding: 0 16px;
    border: none;
    border-radius: 0 4px 4px 0;
    background: #c41e3a;
    color: #fff;
    font-size: 13px;
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: #a01530;
    }
  }
}

/* 第二行：菜单导航（红色背景，不铺满） */
.header-nav {
  min-height: 48px;
  height: auto;
  background: linear-gradient(180deg, #c8102e 0%, #a30d24 100%);

  .nav-container {
    max-width: 1200px;
    height: 100%;
    margin: 0 auto;
    padding: 0 20px;
    display: flex;
    align-items: center;
  }
}

.logo {
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  flex-shrink: 0;

  .logo-img {
    height: 60px;
    width: auto;
    border-radius: 10px;
    object-fit: contain;
    display: block;
  }

  .logo-text {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
  }

  .logo-title {
    font-size: 36px;
    font-weight: bold;
    line-height: 1.3;
    letter-spacing: 3px;
    background: linear-gradient(135deg, #c8102e 0%, #f5222d 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    color: #c8102e;
  }

  .logo-subtitle {
    font-size: 11px;
    color: #999;
    letter-spacing: 1px;
    font-weight: 300;
  }
}

.nav-menu {
  display: flex;
  align-items: center;
  height: 100%;
  flex-wrap: nowrap;

  .nav-item {
    position: relative;
    padding: 0 30px;
    height: 48px;
    line-height: 48px;
    font-size: 15px;
    letter-spacing: 1px;
    color: #fff;
    white-space: nowrap;
    transition: all 0.2s;

    &:hover {
      background: rgba(255, 255, 255, 0.12);
    }

    &.router-link-active {
      background: rgba(255, 255, 255, 0.18);
      font-weight: 500;

      &::after {
        content: '';
        position: absolute;
        left: 50%;
        bottom: 0;
        transform: translateX(-50%);
        width: 30px;
        height: 3px;
        background: #fff;
        border-radius: 2px 2px 0 0;
      }
    }
  }
}

@media (max-width: 768px) {
  .header-top {
    height: 50px;
  }
  .header-container {
    padding: 0 12px;
  }
  .logo {
    gap: 8px;
    .logo-img {
      height: 36px;
      border-radius: 6px;
    }
    .logo-text {
      gap: 0;
    }
    .logo-title {
      font-size: 16px;
      letter-spacing: 0;
      line-height: 1.2;
    }
    .logo-subtitle {
      display: none;
    }
  }
  .header-right {
    gap: 8px;
    .header-search {
      display: none;
    }
    .welcome {
      display: none;
    }
    .subject-switch {
      max-width: 100px;
      padding: 3px 6px;
      .subject-name {
        font-size: 12px;
      }
    }
    .logout {
      padding: 3px 8px;
      font-size: 12px;
    }
    .login-btn,
    .register-btn {
      font-size: 12px;
      padding: 4px 10px;
    }
  }
  .nav-menu {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    &::-webkit-scrollbar {
      display: none;
    }
    .nav-item {
      padding: 0 14px;
      font-size: 13px;
    }
  }
  .header-nav {
    min-height: 40px;
  }
  .back-button {
    top: 6px;
    right: 8px;
    padding: 4px 10px;
    font-size: 12px;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;

  .welcome {
    font-size: 14px;
    color: #666;
  }

  .subject-switch {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    background: #f0f8ff;
    border-radius: 4px;
    cursor: pointer;
    max-width: 200px;
    transition: all 0.2s;

    &:hover {
      background: #e6f4ff;
    }

    .subject-name {
      font-size: 13px;
      color: #1989fa;
      font-weight: 500;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .logout {
    font-size: 14px;
    color: #ee0a24;
    cursor: pointer;
    padding: 4px 12px;
    border: 1px solid #ee0a24;
    border-radius: 4px;
    transition: all 0.2s;

    &:hover {
      background: #ee0a24;
      color: #fff;
    }
  }

  .login-btn,
  .register-btn {
    font-size: 14px;
    padding: 5px 14px;
    border-radius: 4px;
    text-decoration: none;
    transition: all 0.2s;
  }

  .login-btn {
    color: #ee0a24;
    border: 1px solid #ee0a24;

    &:hover {
      background: #ee0a24;
      color: #fff;
    }
  }

  .register-btn {
    color: #fff;
    background: #ee0a24;
    border: 1px solid #ee0a24;

    &:hover {
      background: #c8102e;
    }
  }
}

.subject-picker {
  padding: 0 0 24px;

  .picker-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;

    .picker-title {
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
  }

  .subject-list {
    max-height: 50vh;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }

  .subject-option {
    display: flex;
    align-items: center;
    padding: 14px 16px;
    border-bottom: 1px solid #f5f5f5;

    &.active {
      background: #f0f8ff;
    }

    .option-name {
      flex: 1;
      margin-left: 12px;
      font-size: 15px;
      color: #333;
    }
  }
}
</style>
