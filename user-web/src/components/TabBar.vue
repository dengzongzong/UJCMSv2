<template>
  <van-tabbar v-model="active" route class="tabbar" active-color="#c8102e" inactive-color="#666">
    <van-tabbar-item replace to="/" icon="home-o">首页</van-tabbar-item>
    <van-tabbar-item replace to="/course/my" icon="bookmark-o">课程</van-tabbar-item>
    <van-tabbar-item replace to="/live" icon="video-o">直播</van-tabbar-item>
    <van-tabbar-item replace to="/exam" icon="notes-o">考试</van-tabbar-item>
    <van-tabbar-item replace to="/certificate" icon="award-o">证书</van-tabbar-item>
    <van-tabbar-item replace to="/profile" icon="user-o">我的</van-tabbar-item>
  </van-tabbar>
</template>

<script>
export default {
  name: 'TabBar',
  data() {
    return {
      active: 0
    }
  }
}
</script>

<style lang="scss" scoped>
.tabbar {
  max-width: 500px;
  left: 50%;
  transform: translateX(-50%);
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}
</style>
