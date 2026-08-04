<template>
  <div class="live-room-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <!-- 未开通提示 -->
        <van-notice-bar
          v-if="detail.loaded && !detail.opened"
          left-icon="warning-o"
          color="#ff976a"
          background="#fdf6ec"
          :text="'您尚未开通课程「' + (detail.courseName || '') + '」，请联系管理员开通后再观看直播'"
        />

        <div class="live-main">
          <!-- 播放器区域 -->
          <div class="player-section">
            <div class="player-wrapper">
              <video
                v-if="playUrl"
                ref="videoPlayer"
                class="player-video"
                :poster="resolveImg(detail.coverUrl || '')"
                controls
                autoplay
                playsinline
                webkit-playsinline
              ></video>
              <div v-else class="player-placeholder">
                <van-icon :name="placeholderIcon" size="64" color="#c8c9cc" />
                <p>{{ placeholderText }}</p>
                <div v-if="detail.status === 2 && !detail.replayUrl" class="placeholder-sub">
                  直播已结束，回放地址由管理员发布后即可观看
                </div>
                <div v-if="!detail.opened" class="placeholder-sub">
                  开通课程后可观看直播与回放
                </div>
              </div>
            </div>

            <!-- 直播信息 -->
            <div class="live-info">
              <div class="live-title-row">
                <h2 class="live-title">{{ detail.title }}</h2>
                <van-tag :type="statusTagType" size="medium">{{ statusText }}</van-tag>
              </div>
              <div class="live-meta">
                <span><van-icon name="manager-o" /> {{ detail.anchorName || '主讲老师' }}</span>
                <span><van-icon name="clock-o" /> {{ detail.startTime || '-' }}</span>
                <span><van-icon name="eye-o" /> {{ detail.viewCount || 0 }} 观看</span>
                <span class="online-count"><van-icon name="friends-o" /> {{ onlineCount }} 在线</span>
              </div>
              <div v-if="detail.intro" class="live-intro">{{ detail.intro }}</div>
            </div>
          </div>

          <!-- 聊天区域 -->
          <div class="chat-section">
            <div class="chat-header">
              <van-icon name="chat-o" color="#1989fa" />
              <span>互动聊天</span>
              <span class="chat-count">{{ messages.length }} 条</span>
            </div>

            <div ref="chatList" class="chat-list">
              <div v-if="messages.length === 0" class="chat-empty">
                暂无消息，来说点什么吧~
              </div>
              <div v-for="(m, i) in messages" :key="m.id || i" class="chat-item">
                <span class="chat-nick">{{ m.nickname || '游客' }}</span>
                <span class="chat-content">{{ m.content }}</span>
                <span class="chat-time">{{ formatTime(m.createTime) }}</span>
              </div>
            </div>

            <div class="chat-input-row">
              <van-field
                v-model="inputText"
                placeholder="说点什么..."
                maxlength="500"
                :disabled="!canChat"
                @keyup.enter.native="sendChat"
              />
              <van-button
                type="primary"
                size="small"
                :disabled="!canChat || !inputText.trim()"
                :loading="sending"
                @click="sendChat"
              >
                发送
              </van-button>
            </div>
            <div v-if="!canChat" class="chat-tip">登录并开通课程后即可参与互动</div>
          </div>
        </div>

        <!-- 课程其他直播场次 -->
        <div v-if="courseLives.length > 1" class="section-block">
          <div class="block-title">本课程其他直播</div>
          <div class="other-live-list">
            <div
              v-for="item in courseLives"
              :key="item.id"
              class="other-live-item"
              :class="{ active: item.id === liveId }"
              @click="switchLive(item.id)"
            >
              <div class="other-live-info">
                <div class="other-live-title">{{ item.title }}</div>
                <div class="other-live-time">
                  <van-tag :type="statusType(item.status)" size="small" style="margin-right: 6px">
                    {{ statusTextOf(item.status) }}
                  </van-tag>
                  {{ item.startTime || '' }}
                </div>
              </div>
              <van-icon name="arrow" color="#999" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import store from '@/store'
import { Toast } from 'vant'
import { getLiveDetail, enterLive, getCourseLives, getLiveMessages, sendLiveMessage } from '@/api/live'
import { apiBase, resolveImg } from '@/utils/apiBase'

export default {
  name: 'LiveRoom',
  components: { Header },
  data() {
    return {
      liveId: Number(this.$route.params.id),
      detail: {
        loaded: false,
        id: null,
        title: '',
        courseId: null,
        courseName: '',
        coverUrl: '',
        anchorName: '',
        intro: '',
        startTime: '',
        status: 0,
        viewCount: 0,
        onlineCount: 0,
        replayUrl: '',
        playUrl: '',
        opened: false
      },
      courseLives: [],
      messages: [],
      inputText: '',
      sending: false,
      onlineCount: 0,
      ws: null,
      wsConnected: false,
      reconnectTimer: null,
      heartbeatTimer: null,
      fallbackTimer: null,
      hls: null,
      destroyed: false
    }
  },
  computed: {
    token() {
      return store.getters.token
    },
    statusText() {
      return ['直播未开始', '直播中', '直播已结束', '直播已取消'][this.detail.status] || ''
    },
    statusTagType() {
      return ['info', 'danger', 'success', 'info'][this.detail.status] || 'info'
    },
    playUrl() {
      if (!this.detail.opened) return ''
      if (this.detail.status === 1) return this.detail.playUrl || ''
      if (this.detail.status === 2) return this.detail.replayUrl || ''
      return ''
    },
    canChat() {
      return !!this.token && !!this.detail.opened && this.detail.status === 1
    },
    placeholderIcon() {
      if (this.detail.status === 1) return 'video-o'
      if (this.detail.status === 2) return 'clock-o'
      return 'video-play-o'
    },
    placeholderText() {
      if (this.detail.status === 0) return '直播尚未开始，敬请期待'
      if (this.detail.status === 1) {
        return this.detail.opened ? '正在加载直播流...' : '开通课程后即可观看直播'
      }
      if (this.detail.status === 2) {
        return this.detail.replayUrl ? '' : '直播已结束'
      }
      return '直播已取消'
    }
  },
  created() {
    this.fetchDetail()
  },
  beforeDestroy() {
    this.destroyed = true
    this.closeAll()
  },
  methods: {
    resolveImg,
    statusType(s) {
      return ['info', 'danger', 'success', 'info'][s] || 'info'
    },
    statusTextOf(s) {
      return ['未开始', '直播中', '已结束', '已取消'][s] || ''
    },
    formatTime(t) {
      if (!t) return ''
      const s = String(t).replace('T', ' ')
      if (s.length >= 16) return s.slice(5, 16)
      return s
    },
    async fetchDetail() {
      try {
        const res = await getLiveDetail(this.liveId)
        this.detail = Object.assign(this.detail, res.data || {})
        this.detail.loaded = true
        this.onlineCount = this.detail.onlineCount || 0
        // 已开始/已结束 且 已开通: 累计观看人次 + 播放
        if (this.detail.opened && this.detail.status !== 0) {
          this.enterLive(this.liveId)
        }
        this.initPlayer()
        this.initChat()
        // 详情加载完再拉同课程其他直播场次
        if (this.detail.courseId) {
          this.fetchCourseLives()
        }
      } catch (e) {
        this.detail.loaded = true
        this.detail.status = 3
      }
    },
    enterLive(id) {
      enterLive(id).then((res) => {
        this.detail.viewCount = (res.data && res.data) || this.detail.viewCount
      }).catch(() => {})
    },
    fetchCourseLives() {
      getCourseLives(this.detail.courseId)
        .then((res) => {
          this.courseLives = (res.data || []).filter((it) => it.id !== this.liveId)
        })
        .catch(() => {
          this.courseLives = []
        })
    },
    switchLive(id) {
      this.closeAll()
      this.messages = []
      this.liveId = id
      this.detail = {
        loaded: false,
        id: null,
        title: '',
        courseId: null,
        courseName: '',
        coverUrl: '',
        anchorName: '',
        intro: '',
        startTime: '',
        status: 0,
        viewCount: 0,
        onlineCount: 0,
        replayUrl: '',
        playUrl: '',
        opened: false
      }
      this.$router.replace(`/live/${id}`).catch(() => {})
      this.fetchDetail()
    },
    // ============ 播放器 ============
    initPlayer() {
      const url = this.playUrl
      if (!url) return
      this.$nextTick(() => {
        const video = this.$refs.videoPlayer
        if (!video) return
        // HLS(m3u8) 用 hls.js, 其余(mp4/flv 直链)用原生播放
        if (/\.m3u8($|\?)|m3u8/i.test(url)) {
          if (window.Hls && window.Hls.isSupported()) {
            if (this.hls) {
              this.hls.destroy()
            }
            this.hls = new window.Hls({
              liveDurationInfinity: true,
              maxLiveSyncPlaybackRate: 1.5
            })
            this.hls.loadSource(url)
            this.hls.attachMedia(video)
            this.hls.on(window.Hls.Events.ERROR, (event, data) => {
              if (data && data.fatal) {
                Toast('直播流加载失败，请刷新重试')
              }
            })
          } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
            // iOS Safari 原生支持 HLS
            video.src = url
          }
        } else {
          video.src = url
        }
        video.play().catch(() => {})
      })
    },
    // ============ 聊天 ============
    initChat() {
      // 兜底: 先拉一次历史消息(WebSocket 不可用时也能看到)
      this.fetchFallbackMessages()
      if (this.canChat) {
        this.connectWs()
      }
    },
    connectWs() {
      if (typeof WebSocket === 'undefined') return
      const url = this.buildWsUrl()
      if (!url) return
      try {
        this.ws = new WebSocket(url)
      } catch (e) {
        this.startFallback()
        return
      }
      this.ws.onopen = () => {
        this.wsConnected = true
        this.stopFallback()
        this.startHeartbeat()
      }
      this.ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data)
          if (msg.action === 'history' && msg.data && msg.data.list) {
            this.messages = msg.data.list || []
            this.scrollToBottom()
          } else if (msg.action === 'chat') {
            this.messages.push({
              id: msg.id,
              nickname: msg.nickname,
              content: msg.content,
              createTime: msg.createTime
            })
            this.scrollToBottom()
          } else if (msg.action === 'online') {
            this.onlineCount = msg.count || 0
          }
        } catch (e) { /* ignore */ }
      }
      this.ws.onclose = () => {
        this.wsConnected = false
        this.stopHeartbeat()
        if (!this.destroyed) {
          this.reconnectTimer = setTimeout(() => this.connectWs(), 5000)
          this.startFallback()
        }
      }
      this.ws.onerror = () => { /* ignore */ }
    },
    buildWsUrl() {
      const base = apiBase()
      let url
      if (base) {
        // 开发环境: apiBase 是绝对地址, 直接连后端
        url = base.replace(/^http/, 'ws') + '/api/ws/live/' + this.liveId
      } else {
        // 生产环境: 同源走 nginx /api 反代
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
        url = protocol + '//' + window.location.host + '/api/ws/live/' + this.liveId
      }
      if (this.token) {
        url += '?token=' + encodeURIComponent(this.token)
      }
      return url
    },
    sendChat() {
      const content = this.inputText.trim()
      if (!content) return
      if (this.ws && this.ws.readyState === 1) {
        this.ws.send(JSON.stringify({ action: 'chat', content }))
        this.inputText = ''
        return
      }
      // HTTP 兜底
      this.sending = true
      sendLiveMessage(this.liveId, content)
        .then((res) => {
          this.messages.push((res && res.data) || { nickname: '我', content, createTime: this.nowStr() })
          this.scrollToBottom()
          this.inputText = ''
        })
        .catch(() => {})
        .finally(() => {
          this.sending = false
        })
    },
    fetchFallbackMessages() {
      getLiveMessages(this.liveId, 50)
        .then((res) => {
          if (this.messages.length === 0) {
            this.messages = res.data || []
            this.scrollToBottom()
          }
        })
        .catch(() => {})
    },
    startFallback() {
      if (this.fallbackTimer) return
      this.fallbackTimer = setInterval(() => {
        if (this.wsConnected) return
        this.fetchFallbackMessages()
      }, 10000)
    },
    stopFallback() {
      if (this.fallbackTimer) {
        clearInterval(this.fallbackTimer)
        this.fallbackTimer = null
      }
    },
    startHeartbeat() {
      if (this.heartbeatTimer) return
      this.heartbeatTimer = setInterval(() => {
        if (this.ws && this.ws.readyState === 1) {
          this.ws.send(JSON.stringify({ action: 'ping' }))
        }
      }, 30000)
    },
    stopHeartbeat() {
      if (this.heartbeatTimer) {
        clearInterval(this.heartbeatTimer)
        this.heartbeatTimer = null
      }
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.chatList
        if (el) {
          el.scrollTop = el.scrollHeight
        }
      })
    },
    nowStr() {
      const d = new Date()
      const pad = (n) => (n < 10 ? '0' + n : '' + n)
      return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    },
    closeAll() {
      if (this.hls) {
        this.hls.destroy()
        this.hls = null
      }
      if (this.ws) {
        try { this.ws.close() } catch (e) { /* ignore */ }
        this.ws = null
      }
      this.stopHeartbeat()
      this.stopFallback()
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer)
        this.reconnectTimer = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.live-room-page {
  min-height: 100vh;
  background: #f5f6f8;
}

.page-body {
  padding: 16px;
}

.container {
  max-width: 1100px;
  margin: 0 auto;
}

.live-main {
  display: flex;
  gap: 16px;
  align-items: flex-start;

  @media (max-width: 900px) {
    flex-direction: column;
  }
}

.player-section {
  flex: 1;
  min-width: 0;
}

.player-wrapper {
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  display: flex;
  align-items: center;
  justify-content: center;

  .player-video {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }

  .player-placeholder {
    text-align: center;
    color: #969799;

    p {
      margin: 12px 0 4px;
      font-size: 15px;
    }

    .placeholder-sub {
      font-size: 12px;
      color: #7a7b7c;
    }
  }
}

.live-info {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-top: 12px;

  .live-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;

    .live-title {
      font-size: 18px;
      margin: 0;
      flex: 1;
      min-width: 0;
      word-break: break-all;
    }
  }

  .live-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 14px;
    margin-top: 10px;
    font-size: 13px;
    color: #969799;

    .online-count {
      color: #1989fa;
    }
  }

  .live-intro {
    margin-top: 10px;
    font-size: 13px;
    color: #646566;
    line-height: 1.6;
  }
}

.chat-section {
  width: 320px;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  max-height: 560px;

  @media (max-width: 900px) {
    width: 100%;
    max-height: 420px;
  }

  .chat-header {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 12px 14px;
    border-bottom: 1px solid #f2f3f5;
    font-size: 15px;
    font-weight: 600;

    .chat-count {
      margin-left: auto;
      font-size: 12px;
      color: #969799;
      font-weight: 400;
    }
  }

  .chat-list {
    flex: 1;
    overflow-y: auto;
    padding: 12px 14px;
    min-height: 200px;
    max-height: 400px;

    .chat-empty {
      text-align: center;
      color: #c8c9cc;
      font-size: 13px;
      padding: 40px 0;
    }

    .chat-item {
      display: flex;
      align-items: baseline;
      gap: 6px;
      margin-bottom: 10px;
      font-size: 14px;

      .chat-nick {
        color: #1989fa;
        white-space: nowrap;
        max-width: 90px;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .chat-content {
        flex: 1;
        word-break: break-all;
        line-height: 1.4;
      }

      .chat-time {
        font-size: 11px;
        color: #c8c9cc;
        white-space: nowrap;
      }
    }
  }

  .chat-input-row {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 12px;
    border-top: 1px solid #f2f3f5;
  }

  .chat-tip {
    padding: 0 14px 10px;
    font-size: 12px;
    color: #969799;
  }
}

.section-block {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-top: 16px;

  .block-title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
  }
}

.other-live-list {
  display: flex;
  flex-direction: column;
  gap: 10px;

  .other-live-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px;
    border: 1px solid #f2f3f5;
    border-radius: 8px;
    cursor: pointer;

    &.active {
      border-color: #1989fa;
      background: #ecf5ff;
    }

    .other-live-info {
      .other-live-title {
        font-size: 15px;
        font-weight: 500;
      }

      .other-live-time {
        margin-top: 6px;
        font-size: 12px;
        color: #969799;
      }
    }
  }
}
</style>
