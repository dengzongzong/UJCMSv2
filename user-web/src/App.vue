<template>
  <div id="app" :class="{ 'is-mobile': isMobile }">
    <keep-alive :include="cachedViews">
      <router-view v-if="$route.meta.keepAlive" />
    </keep-alive>
    <router-view v-if="!$route.meta.keepAlive" />
    <FloatingQrcode v-if="!isMobile" />
    <TabBar v-if="isMobile" />
  </div>
</template>

<script>
import FloatingQrcode from '@/components/FloatingQrcode.vue'
import TabBar from '@/components/TabBar.vue'

export default {
  name: 'App',
  components: { FloatingQrcode, TabBar },
  data() {
    return {
      isMobile: window.innerWidth <= 768,
      cachedViews: ['HomeIndex', 'CourseCenter', 'ExamCenter', 'CertificatePortal', 'ProfileIndex']
    }
  },
  mounted() {
    this.setHeaderHeight()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    handleResize() {
      this.isMobile = window.innerWidth <= 768
      this.setHeaderHeight()
    },
    setHeaderHeight() {
      // 移动端Header高度约100px，桌面端 header-top(150)+nav(48)=198px，再加20px间距
      const height = window.innerWidth <= 768 ? '120px' : '218px'
      document.documentElement.style.setProperty('--header-height', height)
    }
  }
}
</script>

<style lang="scss">
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body {
  width: 100%;
  height: 100%;
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica,
    Segoe UI, Arial, Roboto, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft Yahei', sans-serif;
  -webkit-font-smoothing: antialiased;
  color: #333;
}

#app {
  width: 100%;
  min-height: 100vh;
  background: #f5f5f5;
}

a {
  text-decoration: none;
  color: inherit;
}

img {
  display: block;
  max-width: 100%;
}

ul,
li {
  list-style: none;
}

/* 清除浮动 */
.clearfix::after {
  content: '';
  display: block;
  clear: both;
}

/* 统一为 fixed Header 预留空间 */
.page-body {
  padding-top: var(--header-height, 170px) !important;
}

/* 通用页面容器：最大宽度 1600px 居中 */
.page-container {
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px 20px;
}

/* 通用卡片样式 */
.card {
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

/* ========== 移动端全局适配 ========== */
@media (max-width: 768px) {
  /* 移动端底部留出 TabBar 空间 */
  #app.is-mobile {
    padding-bottom: 50px;
  }

  .page-container {
    padding: 12px 12px;
  }

  /* 移动端通用标题缩小 */
  .page-title {
    font-size: 18px !important;
  }
}
</style>
