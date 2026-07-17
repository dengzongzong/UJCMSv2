<template>
  <div class="news-center-page">
    <Header />
    <div class="page-body">
      <div class="container">
        <div class="placeholder-card">
          <van-icon name="info-o" size="48" color="#1989fa" />
          <p class="placeholder-title">中心动态</p>
          <p class="placeholder-desc">页面建设中，敬请期待...</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'

export default {
  name: 'NewsCenter',
  components: { Header }
}
</script>

<style lang="scss" scoped>
.news-center-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-body {
  padding-top: var(--header-height, 168px);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.placeholder-card {
  background: #fff;
  border-radius: 8px;
  padding: 80px 20px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.placeholder-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 16px 0 8px;
}

.placeholder-desc {
  font-size: 14px;
  color: #999;
}

@media (max-width: 768px) {
  .container {
    padding: 12px;
  }
  .placeholder-card {
    padding: 48px 16px;
  }
  .placeholder-title {
    font-size: 18px;
  }
}
</style>
