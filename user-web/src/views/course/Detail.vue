<template>
  <div class="course-detail-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <!-- 未开通课程: 显示占位 + 提示信息 -->
        <van-notice-bar
          v-if="course && course.purchased === false"
          :left-icon="course.needLogin ? 'info-o' : 'warning-o'"
          :color="course.needLogin ? '#1989fa' : '#ff976a'"
          :background="course.needLogin ? '#ecf5ff' : '#fdf6ec'"
          :text="course.needLogin ? '请先登录后再学习' : (canBuy ? '该课程需要购买后学习' : '您尚未开通该课程,请联系管理员开通')"
          style="margin-bottom: 16px"
        >
          <template #right-icon>
            <div style="display: flex; gap: 8px;">
              <template v-if="course.needLogin">
                <van-button size="small" type="primary" @click="goLogin">去登录</van-button>
                <van-button size="small" plain @click="goRegister">注册账号</van-button>
              </template>
              <van-button v-else-if="canBuy" size="small" type="danger" @click="openPayPopup">
                立即购买 ¥{{ course.price }}
              </van-button>
            </div>
          </template>
        </van-notice-bar>

        <!-- 课程信息 -->
        <div class="course-info-card">
          <div class="course-cover">
            <img :src="resolveImg(course.coverUrl || course.coverImage || defaultCover)" :alt="course.name" />
          </div>
          <div class="course-detail-info">
            <h2 class="course-name">{{ course.name }}</h2>
            <div class="course-tags">
              <van-tag
                v-for="(tag, i) in (course.tags || []).slice(0, 3)"
                :key="i"
                plain
                type="primary"
              >
                {{ tag }}
              </van-tag>
            </div>
            <div class="course-meta">
              <span class="meta-item">
                <van-icon name="clock-o" /> 总时长 {{ formatDuration(course.totalDuration) }}
              </span>
              <span class="meta-item">
                <van-icon name="bookmark-o" /> {{ totalSections }}小节
              </span>
            </div>
            <div class="course-price-row">
              <span class="price" v-if="course.price > 0">¥{{ course.price }}</span>
              <span class="price free" v-else>免费</span>
            </div>
          </div>
        </div>

        <!-- 左右布局：视频 + 目录 -->
        <div class="detail-layout">
          <!-- 左侧视频播放区域 -->
          <div class="video-section">
            <div class="video-wrapper">
              <video
                ref="videoPlayer"
                class="video-player"
                :src="resolveImg(currentVideo.url || '')"
                :poster="resolveImg(course.coverUrl || course.coverImage || '')"
                controls
                playsinline
                webkit-playsinline
                @timeupdate="onTimeUpdate"
                @loadedmetadata="onLoadedMetadata"
                @ended="onVideoEnded"
                @play="onPlay"
                @pause="onPause"
              ></video>

              <!-- 未选择视频时的占位 -->
              <div class="video-placeholder" v-if="!currentVideo.url">
                <van-icon name="play-circle-o" size="60" color="#fff" @click="playFirstVideo" />
                <p>请从右侧目录中选择视频开始学习</p>
              </div>
            </div>

            <!-- 课程视频列表(在播放器下方,用户可直接点击切换,无需依赖右侧目录) -->
            <div class="section-block" v-if="course.chapters && course.chapters.length > 0">
              <div class="block-title">课程视频</div>
              <div class="left-video-list">
                <div
                  v-for="chapter in course.chapters"
                  :key="chapter.id || chapter.name"
                  class="left-chapter"
                >
                  <div class="left-chapter-name">{{ chapter.name }}</div>
                  <div
                    v-for="video in chapter.videos"
                    :key="video.id"
                    class="left-video-item"
                    :class="{ active: currentVideo.id === video.id }"
                    @click="playVideo(video)"
                  >
                    <van-icon
                      :name="currentVideo.id === video.id && isPlaying ? 'pause-circle-o' : 'play-circle-o'"
                      :color="currentVideo.id === video.id ? '#1989fa' : '#999'"
                      size="18"
                      @click.stop="handleLeftIconClick(video)"
                    />
                    <span class="left-video-name">{{ video.name }}</span>
                    <span class="left-video-duration">{{ formatDuration(video.duration) }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 直播课程(后台配置,点击进入直播间,直播结束后可看回放) -->
            <div v-if="courseLives.length" class="section-block">
              <div class="block-title">直播课程</div>
              <div class="live-course-list">
                <div v-for="live in courseLives" :key="live.id" class="live-course-item" @click="goLive(live.id)">
                  <div class="live-course-left">
                    <img
                      v-if="live.coverUrl"
                      :src="resolveImg(live.coverUrl)"
                      class="live-course-cover"
                      alt=""
                    />
                    <div v-else class="live-course-cover cover-placeholder">
                      <van-icon name="video-o" color="#c8c9cc" size="24" />
                    </div>
                    <div class="live-course-info">
                      <div class="live-course-title">{{ live.title }}</div>
                      <div class="live-course-meta">
                        <span v-if="live.anchorName">{{ live.anchorName }}</span>
                        <span>{{ live.startTime || '' }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="live-course-right">
                    <van-tag :type="liveStatusType(live.status)" size="small">
                      {{ liveStatusText(live.status) }}
                    </van-tag>
                    <van-button
                      v-if="live.status === 2"
                      size="small"
                      plain
                      type="primary"
                      icon="replay"
                    >看回放</van-button>
                    <van-button
                      v-else
                      size="small"
                      plain
                      type="danger"
                      icon="video-o"
                    >进入直播</van-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 测评服务平台(后台配置,点击跳转) -->
            <div v-if="threeImages.length" class="section-block evaluate-section">
              <div class="section-header">
                <span class="section-title">测评服务平台</span>
              </div>
              <div class="evaluate-row">
                <div
                  v-for="img in threeImages"
                  :key="img.id"
                  class="evaluate-item"
                  @click="onThreeImageClick(img)"
                >
                  <!-- 有图片时只显示图片，不展示标题 -->
                  <template v-if="img.imageUrl">
                    <div class="evaluate-image-wrapper">
                      <img :src="resolveImg(img.imageUrl)" :alt="img.title || ''" class="evaluate-image" />
                    </div>
                  </template>
                  <!-- 无图片时只显示标题，居中显示，位置与图片一致 -->
                  <template v-else>
                    <div class="evaluate-text-content">
                      <div class="evaluate-title">{{ img.title || '' }}</div>
                    </div>
                  </template>
                </div>
              </div>
            </div>

            <!-- 课程介绍 -->
            <div class="section-block">
              <div class="block-title">课程介绍</div>
              <div class="course-intro">{{ course.description || course.intro || '暂无介绍' }}</div>
            </div>
          </div>

          <!-- 右侧课程目录 -->
          <div class="catalog-section">
            <div class="catalog-header">
              <van-icon name="orders-o" size="18" color="#1989fa" />
              <span>课程目录</span>
            </div>
            <div class="catalog-list">
              <div v-if="!course.chapters || course.chapters.length === 0" class="catalog-empty">
                暂无课程目录
              </div>
              <div
                v-for="(chapter, ci) in course.chapters"
                :key="ci"
                class="chapter-item"
              >
                <div class="chapter-header" @click="toggleChapter(ci)">
                  <div class="chapter-left">
                    <van-icon :name="expandedChapters.includes(ci) ? 'arrow-down' : 'arrow'" />
                    <span class="chapter-name">{{ ci + 1 }}. {{ chapter.name }}</span>
                  </div>
                  <span class="chapter-count">{{ chapter.videos.length }}个视频</span>
                </div>
                <div class="video-list" v-show="expandedChapters.includes(ci)">
                  <div
                    v-for="(video, vi) in chapter.videos"
                    :key="video.id"
                    class="video-item"
                    :class="{ active: currentVideo.id === video.id }"
                    @click="playVideo(video, ci, vi)"
                  >
                    <div class="video-left">
                      <van-icon
                        :name="currentVideo.id === video.id && isPlaying ? 'pause' : 'play-circle-o'"
                        :color="currentVideo.id === video.id ? '#1989fa' : '#999'"
                        size="20"
                      />
                      <div class="video-info">
                        <div class="video-name">{{ video.name }}</div>
                        <div class="video-meta">
                          <span>{{ video.updateTime || '' }}</span>
                          <span>{{ formatDuration(video.duration) }}</span>
                        </div>
                        <div v-if="video.remark" class="remark">{{ video.remark }}</div>
                      </div>
                    </div>
                    <van-icon
                      v-if="video.progress && video.progress > 0"
                      name="clock-o"
                      color="#ff976a"
                      size="14"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 清晰度选择 -->
    <van-action-sheet
      v-model="showQualityPicker"
      title="选择清晰度"
      :actions="qualityActions"
      @select="onQualitySelect"
    />

    <!-- 课程购买支付弹窗 -->
    <van-popup
      v-model="showPayPopup"
      round
      :close-on-click-overlay="false"
      :style="{ width: '340px', borderRadius: '12px' }"
    >
      <div class="pay-popup">
        <div class="pay-popup-header">
          <span class="pay-popup-title">购买课程</span>
          <van-icon name="cross" size="18" color="#999" @click="closePayPopup" />
        </div>
        <div class="pay-popup-body">
          <div class="pay-course-name">{{ course.name }}</div>
          <div class="pay-amount-row">
            <span class="pay-label">应付金额</span>
            <span class="pay-amount">¥{{ fenToYuan(payData.amount) }}</span>
          </div>

          <!-- 渠道切换(仅双通道时显示) -->
          <div v-if="payChannels.length > 1" class="pay-channels">
            <div
              v-for="ch in payChannels"
              :key="ch.key"
              class="pay-channel-tab"
              :class="{ active: payChannel === ch.key }"
              @click="switchChannel(ch.key)"
            >
              {{ ch.name }}
            </div>
          </div>

          <!-- 二维码 -->
          <div class="pay-qrcode-wrap">
            <img v-if="payData.qrImage" :src="payData.qrImage" alt="支付二维码" class="pay-qrcode-img" />
            <div v-else class="pay-qrcode-loading">
              <van-loading size="28" color="#1989fa" vertical>正在生成二维码...</van-loading>
            </div>
          </div>
          <div class="pay-tip">
            请使用{{ payChannelName }}扫码支付<br />支付成功后课程自动开通
          </div>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getCourseDetail, reportVideoProgress, getVideoInfo } from '@/api/course'
import { createOrder, getOrderByNo } from '@/api/order'
import { getCourseLives } from '@/api/live'
import { resolveImg } from '@/utils/apiBase'
import { Toast, Dialog } from 'vant'

export default {
  name: 'CourseDetail',
  components: { Header },
  data() {
    return {
      courseId: this.$route.params.id,
      course: {
        id: null,
        name: '',
        coverImage: '',
        description: '',
        price: 0,
        totalDuration: 0,
        tags: [],
        chapters: []
      },
      currentVideo: {
        id: null,
        name: '',
        url: '',
        duration: 0
      },
      isPlaying: false,
      currentTime: 0,
      duration: 0,
      expandedChapters: [0],
      showQualityPicker: false,
      currentQuality: '标清',
      qualityList: ['高清', '标清', '流畅'],
      reportTimer: null,
      lastReportTime: null,
      threeImages: [],
      courseLives: [],
      // 支付相关
      showPayPopup: false,
      payData: {},
      payChannel: 'wechat',
      pollTimer: null,
      defaultCover: 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="120" height="80"><rect fill="#e8e8e8" width="120" height="80"/><text x="50%" y="50%" text-anchor="middle" dy=".3em" fill="#999" font-size="12">课程封面</text></svg>')
    }
  },
  computed: {
    playedPercent() {
      if (!this.duration) return 0
      return Math.min(100, (this.currentTime / this.duration) * 100)
    },
    totalSections() {
      return this.course.chapters.reduce((sum, ch) => sum + (ch.videos ? ch.videos.length : 0), 0)
    },
    qualityActions() {
      return this.qualityList.map(q => ({
        name: q,
        color: q === this.currentQuality ? '#1989fa' : '#333'
      }))
    },
    /** 是否可在线购买: 已登录 + 未开通 + 价格>0 */
    canBuy() {
      return !this.course.needLogin && this.course.purchased === false && Number(this.course.price) > 0
    },
    /** 可用支付渠道 */
    payChannels() {
      return [{ key: 'wechat', name: '微信支付' }, { key: 'alipay', name: '支付宝' }]
    },
    payChannelName() {
      return this.payChannel === 'alipay' ? '支付宝' : '微信'
    }
  },
  created() {
    this.fetchCourseDetail()
    this.loadThreeImages()
    this.fetchCourseLives()
  },
  beforeDestroy() {
    this.stopReportTimer()
    this.stopPoll()
  },
  methods: {
    resolveImg,
    goLogin() {
      this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
    },
    goRegister() {
      this.$router.push({ path: '/register', query: { redirect: this.$route.fullPath } })
    },
    // ===== 课程购买支付 =====
    fenToYuan(fen) {
      if (fen === null || fen === undefined) return '0.00'
      return (fen / 100).toFixed(2)
    },
    openPayPopup() {
      this.payData = {}
      this.payChannel = 'wechat'
      this.showPayPopup = true
      this.doCreateOrder('wechat')
    },
    closePayPopup() {
      this.stopPoll()
      this.showPayPopup = false
    },
    switchChannel(channel) {
      if (this.payChannel === channel) return
      this.payChannel = channel
      this.doCreateOrder(channel)
    },
    async doCreateOrder(channel) {
      this.payData = { amount: Math.round(Number(this.course.price) * 100) }
      try {
        const res = await createOrder(this.courseId, channel)
        const data = res.data || res
        this.payData = data
        if (data.opened) {
          // 免费/已开通: 直接提示成功
          Toast.success('课程已开通')
          this.closePayPopup()
          this.fetchCourseDetail()
          return
        }
        this.startPoll()
      } catch (e) {
        Toast.fail(e.message || '下单失败,请稍后重试')
      }
    },
    startPoll() {
      this.stopPoll()
      this.pollTimer = setInterval(async () => {
        const orderNo = this.payData.orderNo
        if (!orderNo) return
        try {
          const res = await getOrderByNo(orderNo)
          const order = res.data || res
          if (order && order.status === 1) {
            Toast.success('支付成功,课程已开通')
            this.closePayPopup()
            this.fetchCourseDetail()
          } else if (order && order.status === 2) {
            this.stopPoll()
            Toast('订单已关闭,请重新购买')
            this.showPayPopup = false
          }
        } catch (e) {
          // 轮询失败忽略,继续等待
        }
      }, 3000)
    },
    stopPoll() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    fetchCourseLives() {
      if (!this.courseId) return
      getCourseLives(this.courseId)
        .then((res) => {
          this.courseLives = res.data || []
        })
        .catch(() => {
          this.courseLives = []
        })
    },
    goLive(liveId) {
      this.$router.push({ path: '/live/' + liveId }).catch(() => {})
    },
    liveStatusText(status) {
      return ['未开始', '直播中', '已结束', '已取消'][status] || ''
    },
    liveStatusType(status) {
      return ['info', 'danger', 'success', 'info'][status] || 'info'
    },
    onThreeImageClick(img) {
      const t = Number(img.linkType)
      if (t === 1 && img.linkUrl) {
        // 跳转外链
        window.open(img.linkUrl, '_blank')
      } else if (t === 2 && img.linkId) {
        // 跳转试卷 -> /exam/intro/:id
        this.$router.push('/exam/intro/' + img.linkId).catch(() => {})
      } else if (t === 3 && img.linkId) {
        // 跳转课程 -> /course/detail/:id
        this.$router.push('/course/detail/' + img.linkId).catch(() => {})
      } else {
        Toast('该图片未配置跳转')
      }
    },
    async loadThreeImages() {
      try {
        const res = await getCourseThreeImages(this.courseId)
        const data = res.data || res || []
        this.threeImages = Array.isArray(data) ? data : []
      } catch (e) {
        this.threeImages = []
      }
    },
    async fetchCourseDetail() {
      try {
        const res = await getCourseDetail(this.courseId)
        const data = res.data || res
        this.course = {
          id: data.id,
          name: data.name,
          coverImage: data.coverUrl || data.coverImage || data.thumbnail || '',
          description: data.description || data.intro || '',
          price: data.price || 0,
          totalDuration: data.totalDuration || 0,
          tags: data.tags || [],
          chapters: data.chapters || data.sections || []
        }
        // 不再做兜底：后端没有返回章节就保持空数组，由 UI 自行提示"暂无目录"
      } catch (error) {
        // 接口失败时只记录日志，不写入任何假数据；UI 展示空状态
        console.error('获取课程详情失败:', error)
        this.course = {
          id: this.courseId,
          name: '',
          coverImage: '',
          description: '',
          price: 0,
          totalDuration: 0,
          tags: [],
          chapters: []
        }
      }
    },
    async playVideo(video, chapterIndex, videoIndex) {
      // 未开通课程时不允许播放视频
      if (this.course && this.course.purchased === false) {
        if (this.canBuy) {
          // 付费课程: 弹出购买提示
          Dialog.alert({
            title: '提示',
            message: '该课程需要购买后才能学习',
            confirmButtonText: '去购买',
            showCancelButton: true,
            cancelButtonText: '取消'
          }).then(() => {
            this.openPayPopup()
          }).catch(() => {})
        } else {
          Dialog.alert({
            title: '提示',
            message: this.course.needLogin ? '请先登录后再学习' : '您尚未开通该课程,请联系管理员先开通',
            confirmButtonText: '我知道了'
          })
        }
        return
      }
      // 初始化当前播放视频，后端没有 url 时就保持空字符串，不做假数据兜底
      this.currentVideo = {
        id: video.id,
        name: video.name,
        url: video.url || '',
        duration: video.duration
      }

      // 如果后端没有返回 url，再请求一次 video-info；拿不到就保持空，不播放任何视频
      if (!video.url) {
        try {
          const res = await getVideoInfo(video.id, this.courseId)
          const data = res.data || res
          this.currentVideo.url = data.url || data.playUrl || ''
          this.currentVideo.duration = data.duration || video.duration
        } catch (error) {
          console.error('获取视频播放信息失败:', error)
          this.currentVideo.url = ''
        }
      }

      // 没有真实视频源时不自动播放，避免播放器加载空 url
      if (!this.currentVideo.url) {
        this.isPlaying = false
        return
      }

      // 自动播放
      this.$nextTick(() => {
        const player = this.$refs.videoPlayer
        if (player) {
          player.load()
          player.play().then(() => {
            this.isPlaying = true
            this.startReportTimer()
          }).catch(() => {
            this.isPlaying = false
          })
        }
      })
    },
    getSampleVideoUrl() {
      return ''
    },
    handleLeftIconClick(video) {
      if (this.currentVideo.id === video.id && this.isPlaying) {
        this.togglePlay()
      } else {
        this.playVideo(video)
      }
    },
    playFirstVideo() {
      if (this.course.chapters && this.course.chapters.length > 0) {
        const firstChapter = this.course.chapters[0]
        if (firstChapter.videos && firstChapter.videos.length > 0) {
          this.playVideo(firstChapter.videos[0])
        }
      }
    },
    togglePlay() {
      const player = this.$refs.videoPlayer
      if (!player) return
      if (player.paused) {
        player.play()
        this.isPlaying = true
        this.startReportTimer()
      } else {
        player.pause()
        this.isPlaying = false
      }
    },
    onPlay() {
      this.isPlaying = true
      this.startReportTimer()
    },
    onPause() {
      this.isPlaying = false
    },
    onTimeUpdate() {
      const player = this.$refs.videoPlayer
      if (player) {
        this.currentTime = player.currentTime
        this.duration = player.duration || 0
      }
    },
    onLoadedMetadata() {
      const player = this.$refs.videoPlayer
      if (player) {
        this.duration = player.duration || 0
      }
    },
    onVideoEnded() {
      this.isPlaying = false
      this.stopReportTimer()
      // 视频结束,上报100%进度,并把上次上报到结束之间的观看时长也算上
      const now = Date.now()
      const interval = this.lastReportTime ? Math.round((now - this.lastReportTime) / 1000) : 0
      this.lastReportTime = now
      reportVideoProgress({
        videoId: this.currentVideo.id,
        courseId: this.courseId,
        progress: 100,
        watchedDuration: interval
      }).catch(() => {})
      Toast('视频播放完毕')
    },
    seek(event) {
      const player = this.$refs.videoPlayer
      if (!player || !player.duration) return
      const rect = event.currentTarget.getBoundingClientRect()
      const percent = (event.clientX - rect.left) / rect.width
      player.currentTime = percent * player.duration
      this.currentTime = player.currentTime
    },
    seekForward() {
      const player = this.$refs.videoPlayer
      if (!player) return
      player.currentTime = Math.min(player.currentTime + 15, player.duration || 0)
      Toast('快进15秒')
    },
    toggleChapter(index) {
      const idx = this.expandedChapters.indexOf(index)
      if (idx > -1) {
        this.expandedChapters.splice(idx, 1)
      } else {
        this.expandedChapters.push(index)
      }
    },
    onQualitySelect(item) {
      const quality = item.name
      const player = this.$refs.videoPlayer
      this.currentQuality = quality
      this.showQualityPicker = false

      // 没有视频源时仅切换标签
      if (!player || !this.currentVideo.url) {
        Toast('已切换到' + quality)
        return
      }

      // 记录当前播放时间
      const currentTime = player.currentTime
      // 模拟清晰度切换：同一URL加参数
      const baseUrl = this.currentVideo.url.split('?')[0]
      this.currentVideo.url = baseUrl + '?quality=' + encodeURIComponent(quality)

      // 切换 src 后恢复进度并继续播放
      this.$nextTick(() => {
        const p = this.$refs.videoPlayer
        if (!p) return
        const onMeta = () => {
          try {
            p.currentTime = currentTime
          } catch (e) {
            // 忽略设置进度异常
          }
          p.play().then(() => {
            this.isPlaying = true
            this.startReportTimer()
          }).catch(() => {
            this.isPlaying = false
          })
          p.removeEventListener('loadedmetadata', onMeta)
        }
        p.addEventListener('loadedmetadata', onMeta)
        p.load()
      })
      Toast('已切换到' + quality)
    },
    startReportTimer() {
      this.stopReportTimer()
      this.lastReportTime = Date.now()
      this.reportTimer = setInterval(() => {
        this.reportProgress()
      }, 30000) // 每30秒上报一次
    },
    stopReportTimer() {
      if (this.reportTimer) {
        clearInterval(this.reportTimer)
        this.reportTimer = null
      }
    },
    reportProgress(forcePercent) {
      if (!this.currentVideo.id) return
      const player = this.$refs.videoPlayer
      if (!player) return

      const progress = forcePercent !== undefined
        ? forcePercent
        : (player.duration ? Math.round((player.currentTime / player.duration) * 100) : 0)

      // 计算本次观看时长（秒）：距上次上报的时间差
      const now = Date.now()
      const interval = this.lastReportTime ? Math.round((now - this.lastReportTime) / 1000) : 30
      this.lastReportTime = now

      reportVideoProgress({
        videoId: this.currentVideo.id,
        courseId: this.courseId,
        progress: progress,
        watchedDuration: interval
      }).catch(() => {})
    },
    formatTime(seconds) {
      if (!seconds || isNaN(seconds)) return '00:00'
      const min = Math.floor(seconds / 60)
      const sec = Math.floor(seconds % 60)
      return `${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
    },
    formatDuration(seconds) {
      if (!seconds) return '0分钟'
      const hours = Math.floor(seconds / 3600)
      const min = Math.floor((seconds % 3600) / 60)
      if (hours > 0) {
        return `${hours}小时${min}分钟`
      }
      return `${min}分钟`
    },
    resolveImg
  }
}
</script>

<style lang="scss" scoped>
.course-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-body {
  padding-top: var(--header-height, 170px);
}

.container {
  width: 80%;
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px 20px;
}

.course-info-card {
  display: flex;
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  .course-cover {
    width: 200px;
    height: 130px;
    border-radius: 8px;
    overflow: hidden;
    flex-shrink: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .course-detail-info {
    flex: 1;
    margin-left: 20px;
    display: flex;
    flex-direction: column;

    .course-name {
      font-size: 22px;
      font-weight: bold;
      color: #333;
      margin-bottom: 10px;
      line-height: 1.4;
    }

    .course-tags {
      display: flex;
      gap: 6px;
      margin-bottom: 10px;
    }

    .course-meta {
      display: flex;
      gap: 20px;
      margin-bottom: 12px;

      .meta-item {
        font-size: 13px;
        color: #999;
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }

    .course-price-row {
      margin-top: auto;

      .price {
        font-size: 24px;
        color: #ee0a24;
        font-weight: bold;

        &.free {
          color: #07c160;
        }
      }
    }
  }
}

.detail-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.video-section {
  flex: 0 0 65%;
  min-width: 0;
}

.catalog-section {
  flex: 0 0 33%;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  position: sticky;
  top: 80px;
  max-height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
}

.catalog-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 16px 20px;
  font-size: 16px;
  font-weight: bold;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.catalog-list {
  overflow-y: auto;
  flex: 1;
}

.catalog-empty {
  padding: 40px 20px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.video-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);

  .video-player {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.video-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;

  p {
    margin-top: 12px;
    font-size: 14px;
    opacity: 0.8;
  }
}

.custom-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  pointer-events: none;

  .controls-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: linear-gradient(180deg, rgba(0,0,0,0.5), transparent);
    pointer-events: auto;

    .video-title {
      color: #fff;
      font-size: 13px;
      max-width: 60%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .controls-bottom {
    display: flex;
    align-items: center;
    padding: 8px 12px;
    background: linear-gradient(0deg, rgba(0,0,0,0.6), transparent);
    pointer-events: auto;
    gap: 10px;

    .time-display {
      color: #fff;
      font-size: 12px;
      white-space: nowrap;
    }

    .progress-bar {
      flex: 1;
      height: 4px;
      background: rgba(255, 255, 255, 0.3);
      border-radius: 2px;
      cursor: pointer;
      position: relative;

      .progress-played {
        height: 100%;
        background: #1989fa;
        border-radius: 2px;
      }
    }

    .quality-selector {
      display: flex;
      align-items: center;
      color: #fff;
      font-size: 12px;
      cursor: pointer;
    }
  }
}

.section-block {
  background: #fff;
  margin-top: 20px;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  .block-title {
    font-size: 18px;
    font-weight: bold;
    color: #333;
    margin-bottom: 14px;
    padding-left: 10px;
    border-left: 4px solid #1989fa;
  }

  .course-intro {
    font-size: 14px;
    color: #666;
    line-height: 1.8;
  }
}

.left-video-list {
  .left-chapter {
    margin-bottom: 12px;

    &:last-child {
      margin-bottom: 0;
    }

    .left-chapter-name {
      font-size: 14px;
      font-weight: 600;
      color: #333;
      padding: 8px 0;
      border-bottom: 1px dashed #eee;
      margin-bottom: 6px;
    }

    .left-video-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 8px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;
      color: #555;
      transition: background 0.15s;

      &:hover {
        background: #f0f8ff;
      }

      &.active {
        background: #e6f4ff;
        color: #1989fa;
        font-weight: 500;
      }

      .left-video-name {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .left-video-duration {
        color: #999;
        font-size: 12px;
        flex-shrink: 0;
      }
    }
  }
}

.catalog-list {
  .chapter-item {
    border-bottom: 1px solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .chapter-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      background: #f7f8fa;
      cursor: pointer;

      .chapter-left {
        display: flex;
        align-items: center;

        .chapter-name {
          margin-left: 6px;
          font-size: 14px;
          font-weight: 500;
          color: #333;
        }
      }

      .chapter-count {
        font-size: 12px;
        color: #999;
      }
    }

    .video-list {
      .video-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 16px 12px 36px;
        border-bottom: 1px solid #f5f5f5;
        cursor: pointer;

        &:last-child {
          border-bottom: none;
        }

        &.active {
          background: #f0f8ff;
        }

        .video-left {
          display: flex;
          align-items: center;
          flex: 1;
          min-width: 0;

          .video-info {
            margin-left: 8px;
            min-width: 0;

            .video-name {
              font-size: 13px;
              color: #333;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .video-meta {
              display: flex;
              gap: 10px;
              margin-top: 2px;

              span {
                font-size: 11px;
                color: #999;
              }
            }

            .remark {
              margin-top: 4px;
              font-size: 11px;
              color: #ff976a;
              line-height: 1.4;
            }
          }
        }
      }
    }
  }
}

@media (max-width: 992px) {
  .detail-layout {
    flex-direction: column;
  }

  .video-section,
  .catalog-section {
    flex: 1 1 100%;
    width: 100%;
  }

  .catalog-section {
    position: static;
    max-height: none;
  }
}

/* 测评服务平台 - 风格与推荐课程一致 */
.evaluate-section {
  padding: 20px !important;
  background: #fff !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04) !important;
  border: none !important;
  margin-top: 16px !important;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;

    .section-title {
      font-size: 22px;
      font-weight: bold;
      color: #333;
      position: relative;
      padding-left: 14px;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 4px;
        height: 20px;
        background: #1989fa;
        border-radius: 2px;
      }
    }
  }
}

.evaluate-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.evaluate-item {
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }
}

.evaluate-image-wrapper {
  position: relative;
  width: 100%;
  height: 180px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s;
  }
}

.evaluate-content {
  padding: 14px 16px 16px;
}

.evaluate-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.evaluate-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  margin-top: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 纯文字模式 */
.evaluate-text-content {
  width: 100%;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.evaluate-text-content .evaluate-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  line-height: 1.5;
}

/* 直播课程 */
.live-course-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.live-course-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }
}

.live-course-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.live-course-cover {
  width: 96px;
  height: 56px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6f8;
}

.live-course-info {
  min-width: 0;
}

.live-course-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.live-course-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #969799;
  display: flex;
  gap: 10px;
}

.live-course-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 10px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .container { width: 100%; padding: 12px; }
  .course-info-card {
    flex-direction: column;
    .course-cover { width: 100%; height: 160px; }
    .course-detail-info {
      margin-left: 0;
      margin-top: 12px;
      .course-name { font-size: 16px; }
      .course-price-row .price { font-size: 18px; }
    }
  }
  .evaluate-row { grid-template-columns: 1fr; }
  .detail-layout { gap: 12px; }
  .section-block { padding: 12px; }
  .evaluate-section { padding: 12px !important; }
}

/* ===== 课程购买支付弹窗 ===== */
.pay-popup {
  .pay-popup-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 16px;
    border-bottom: 1px solid #f0f0f0;

    .pay-popup-title {
      font-size: 15px;
      font-weight: 600;
      color: #333;
    }
  }

  .pay-popup-body {
    padding: 16px;
    text-align: center;

    .pay-course-name {
      font-size: 14px;
      color: #333;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .pay-amount-row {
      margin-top: 8px;
      display: flex;
      justify-content: center;
      align-items: baseline;
      gap: 8px;

      .pay-label {
        font-size: 13px;
        color: #999;
      }

      .pay-amount {
        font-size: 26px;
        font-weight: bold;
        color: #ee0a24;
      }
    }

    .pay-channels {
      margin: 14px auto 0;
      display: inline-flex;
      border-radius: 6px;
      overflow: hidden;

      .pay-channel-tab {
        padding: 6px 18px;
        font-size: 13px;
        background: #f5f6f8;
        color: #666;
        cursor: pointer;
        transition: all 0.2s;

        &.active {
          background: #1989fa;
          color: #fff;
        }

        &.active-alipay.active {
          background: #1677ff;
        }
      }
    }

    .pay-qrcode-wrap {
      margin: 16px auto 0;
      width: 220px;
      height: 220px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 1px solid #f0f0f0;
      border-radius: 8px;
      background: #fff;

      .pay-qrcode-img {
        width: 200px;
        height: 200px;
      }

      .pay-qrcode-loading {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 100%;
      }
    }

    .pay-tip {
      margin-top: 12px;
      font-size: 12px;
      color: #999;
      line-height: 1.6;
    }
  }
}
</style>
