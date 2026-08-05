<template>
  <div class="face-verify-page">
    <Header />

    <div class="verify-container">
      <div class="header-section">
        <van-icon name="contact" size="48" color="#1989fa" />
        <h2 class="title">考前人脸识别</h2>
        <p class="subtitle">请确保光线充足，面部正对摄像头</p>
      </div>

      <div class="camera-section">
        <div class="camera-wrapper">
          <video
            ref="video"
            autoplay
            playsinline
            webkit-playsinline
            x5-playsinline
            x5-video-player-type="h5"
            muted
            :class="{ 'camera-error': cameraError }"
          ></video>
          <canvas ref="canvas" style="display:none"></canvas>

          <!-- 静态人脸框引导(不需要前端模型检测) -->
          <div class="face-overlay" v-if="!cameraError && !loading">
            <div class="face-frame">
              <div class="corner top-left"></div>
              <div class="corner top-right"></div>
              <div class="corner bottom-left"></div>
              <div class="corner bottom-right"></div>
            </div>
          </div>

          <div class="loading-overlay" v-if="loading">
            <van-loading type="spinner" size="24px" />
            <span>{{ loadingMessage }}</span>
          </div>

          <div class="error-overlay" v-if="cameraError">
            <van-icon name="warning-o" size="32px" color="#ee0a24" />
            <span>{{ cameraErrorMessage }}</span>
            <van-button size="small" type="primary" @click="retryInit" style="margin-top:12px">重新加载</van-button>
            <van-button v-if="allowSkip" size="small" type="warning" @click="skipToExam" style="margin-top:8px">跳过验证，进入考试</van-button>
          </div>

          <!-- 拍照闪光效果 -->
          <div class="flash-overlay" v-if="flash"></div>
        </div>

        <div class="status-bar" :class="statusClass">
          <van-icon :name="statusIcon" size="16" />
          <span>{{ statusMessage }}</span>
        </div>
      </div>

      <div class="result-section" v-if="verifyResult !== null">
        <div class="result-card" :class="verifyResult ? 'success' : 'fail'">
          <van-icon :name="verifyResult ? 'success' : 'cross'" size="32" />
          <span class="result-text">
            {{ verifyResult ? '验证通过' : '验证失败' }}
          </span>
          <span class="similarity-text" v-if="similarity !== null">
            相似度：{{ similarityPercent }}%
          </span>
        </div>
      </div>

      <div class="action-section">
        <van-button
          round
          block
          type="primary"
          size="large"
          :loading="verifying"
          :disabled="cameraError || loading"
          @click="handleVerify"
        >
          {{ verifying ? '比对中...' : '拍照验证' }}
        </van-button>

        <van-button
          v-if="verifyResult === false && retryCount < maxRetries"
          round
          block
          plain
          size="large"
          @click="handleRetry"
        >
          重新验证
        </van-button>

        <p class="retry-tip" v-if="retryCount > 0">
          已重试 {{ retryCount }}/{{ maxRetries }} 次
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getFaceConfig, compareFace, submitVerifyResult, getFaceStatus } from '@/api/face'

export default {
  name: 'FaceVerify',
  components: { Header },
  data() {
    return {
      examId: this.$route.params.id,
      recordId: this.$route.query.recordId,

      config: {
        enabled: false,
        threshold: 0.6,
        maxRetries: 3
      },

      loading: true,
      loadingMessage: '正在初始化...',
      cameraError: false,
      cameraErrorMessage: '',
      verifying: false,
      flash: false,

      verifyResult: null,
      similarity: null,
      retryCount: 0,
      maxRetries: 3,

      stream: null,

      // 系统异常时允许跳过(记录为验证失败)
      allowSkip: false
    }
  },

  computed: {
    statusMessage() {
      if (this.loading) return this.loadingMessage
      if (this.cameraError) return this.cameraErrorMessage
      if (this.verifying) return '正在上传照片比对...'
      if (this.verifyResult === true) return '验证通过'
      if (this.verifyResult === false) return '验证失败，请重试'
      return '请将面部对准框内，点击拍照验证'
    },
    statusIcon() {
      if (this.verifyResult === true) return 'success'
      if (this.cameraError || this.verifyResult === false) return 'warning-o'
      return 'info-o'
    },
    statusClass() {
      if (this.verifyResult === true) return 'status-success'
      if (this.cameraError || this.verifyResult === false) return 'status-warning'
      return 'status-normal'
    },
    similarityPercent() {
      if (this.similarity === null) return 0
      // similarity 是 Bhattacharyya 距离 [0,1], 0=完全相同
      // 转为百分比: (1 - distance) * 100
      return Math.max(0, Math.round((1 - this.similarity) * 100))
    }
  },

  async mounted() {
    await this.initFaceVerify()
  },

  beforeDestroy() {
    this.stopCamera()
  },

  methods: {
    retryInit() {
      this.cameraError = false
      this.cameraErrorMessage = ''
      this.loading = true
      this.loadingMessage = '正在重新加载...'
      this.initFaceVerify()
    },

    async skipToExam() {
      try {
        await submitVerifyResult({
          examId: this.examId,
          similarity: null,
          passed: false,
          deviceInfo: navigator.userAgent
        })
      } catch (e) {
        console.warn('跳过验证记录提交失败:', e)
      }
      this.$toast('已跳过人脸验证，正在进入考试...')
      setTimeout(() => this.goToExam(), 1000)
    },

    async initFaceVerify() {
      try {
        // 1. 获取人脸验证配置
        const configRes = await getFaceConfig()
        this.config = configRes.data || this.config
        this.maxRetries = this.config.maxRetries || 3

        // 如果人脸识别未启用，直接进入考试
        if (!this.config.enabled) {
          this.goToExam()
          return
        }

        // 2. 检查是否已验证通过
        const statusRes = await getFaceStatus(this.examId)
        if (statusRes.data && statusRes.data.verified) {
          this.goToExam()
          return
        }

        // 3. 启动摄像头(不需要加载任何模型)
        await this.initCamera()
      } catch (err) {
        console.error('初始化失败:', err)
        this.cameraError = true
        let msg = err.message || '初始化失败'
        if (msg.includes('系统异常')) {
          msg = '服务暂时不可用，请稍后刷新重试'
        }
        if (msg.includes('证件照') || msg.includes('未找到')) {
          this.allowSkip = true
        }
        this.cameraErrorMessage = msg
      } finally {
        this.loading = false
      }
    },

    async initCamera() {
      this.loadingMessage = '正在启动摄像头...'

      try {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
          throw new Error('当前浏览器不支持摄像头功能，请使用微信内置浏览器或Chrome/Safari')
        }

        this.stream = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: 'user',
            width: { ideal: 640 },
            height: { ideal: 480 }
          },
          audio: false
        })

        this.$refs.video.srcObject = this.stream

        await new Promise((resolve, reject) => {
          this.$refs.video.onloadedmetadata = resolve
          setTimeout(() => reject(new Error('摄像头视频加载超时')), 5000)
        })

        // 摄像头就绪,不需要加载模型,直接显示
      } catch (err) {
        this.cameraError = true
        let msg = '无法访问摄像头'
        if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
          msg = '摄像头权限被拒绝，请在浏览器设置中允许访问摄像头后刷新重试'
        } else if (err.name === 'NotFoundError') {
          msg = '未检测到摄像头设备，请检查设备是否连接'
        } else if (err.name === 'NotReadableError') {
          msg = '摄像头被其他应用占用，请关闭其他使用摄像头的应用后重试'
        } else if (err.message) {
          msg = err.message
        }
        this.cameraErrorMessage = msg
        throw new Error(msg)
      }
    },

    /**
     * 拍照验证: 截取视频帧 -> 压缩为JPEG -> 上传后端比对
     */
    async handleVerify() {
      if (this.verifying) return

      this.verifying = true

      try {
        // 1. 闪光效果
        this.flash = true
        setTimeout(() => { this.flash = false }, 300)

        // 2. 截取视频帧并压缩
        const photoBase64 = this.takeSnapshot()

        // 3. 上传到后端比对
        const res = await compareFace({
          examId: this.examId,
          photo: photoBase64,
          deviceInfo: navigator.userAgent
        })

        const data = res.data || {}
        this.similarity = data.similarity
        this.verifyResult = data.passed

        if (data.passed) {
          this.$toast('验证通过，即将进入考试...')
          setTimeout(() => this.goToExam(), 1500)
        } else {
          this.retryCount++
          if (this.retryCount >= this.maxRetries) {
            this.$toast.fail('验证失败次数过多，请联系管理员')
          } else {
            this.$toast.fail(data.message || '验证失败，请重试')
          }
        }
      } catch (err) {
        console.error('人脸比对失败:', err)
        let msg = err.message || '未知错误'
        // 证件照/引擎/OpenCV相关错误允许跳过验证进入考试
        if (msg.includes('证件照') || msg.includes('未找到') || msg.includes('引擎')
            || msg.includes('OpenCV') || msg.includes('原生库') || msg.includes('初始化')
            || msg.includes('系统异常')) {
          this.allowSkip = true
          msg = '人脸比对服务暂时不可用(' + msg + ')，可点击跳过验证进入考试'
        }
        this.$toast.fail('验证失败：' + msg)
      } finally {
        this.verifying = false
      }
    },

    /**
     * 从视频流截取一帧，压缩为 JPEG base64
     * 尺寸 480x360, quality 0.8, 体积约 30-60KB
     */
    takeSnapshot() {
      const video = this.$refs.video
      const canvas = this.$refs.canvas
      const ctx = canvas.getContext('2d')

      // 截取中间区域(优先人脸部分), 缩小到 480x360
      var width = 480
      var height = 360
      canvas.width = width
      canvas.height = height

      // 镜像翻转(与预览一致)
      ctx.setTransform(-1, 0, 0, 1, width, 0)
      ctx.drawImage(video, 0, 0, width, height)
      ctx.setTransform(1, 0, 0, 1, 0, 0)

      return canvas.toDataURL('image/jpeg', 0.8)
    },

    handleRetry() {
      this.verifyResult = null
      this.similarity = null
    },

    goToExam() {
      this.$router.push({
        path: '/exam/take/' + this.examId,
        query: { recordId: this.recordId }
      })
    },

    stopCamera() {
      if (this.stream) {
        this.stream.getTracks().forEach(track => track.stop())
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.face-verify-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.verify-container {
  padding: 20px;
}

.header-section {
  text-align: center;
  padding: 30px 0;

  .title {
    font-size: 22px;
    color: #333;
    margin: 12px 0 8px;
  }

  .subtitle {
    font-size: 14px;
    color: #999;
  }
}

.camera-section {
  margin-bottom: 24px;
}

.camera-wrapper {
  position: relative;
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  aspect-ratio: 4/3;

  video {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transform: scaleX(-1);
    display: block;
    -webkit-transform: scaleX(-1);
  }
}

@supports not (aspect-ratio: 4/3) {
  .camera-wrapper {
    padding-bottom: 75%;
    height: 0;
  }
  .camera-wrapper video {
    position: absolute;
    top: 0;
    left: 0;
  }
}

.face-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.face-frame {
  width: 60%;
  aspect-ratio: 3/4;
  position: relative;

  .corner {
    position: absolute;
    width: 36px;
    height: 36px;
    border-color: rgba(255, 255, 255, 0.85);
    border-style: solid;
    border-width: 0;
    box-shadow: 0 0 6px rgba(255, 255, 255, 0.3);

    &.top-left {
      top: 0; left: 0;
      border-top-width: 4px;
      border-left-width: 4px;
      border-top-left-radius: 8px;
    }
    &.top-right {
      top: 0; right: 0;
      border-top-width: 4px;
      border-right-width: 4px;
      border-top-right-radius: 8px;
    }
    &.bottom-left {
      bottom: 0; left: 0;
      border-bottom-width: 4px;
      border-left-width: 4px;
      border-bottom-left-radius: 8px;
    }
    &.bottom-right {
      bottom: 0; right: 0;
      border-bottom-width: 4px;
      border-right-width: 4px;
      border-bottom-right-radius: 8px;
    }
  }
}

.status-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  margin-top: 12px;
  border-radius: 8px;
  font-size: 14px;

  &.status-normal { background: #f0f0f0; color: #666; }
  &.status-success { background: #e8f5e9; color: #4caf50; }
  &.status-warning { background: #fff3e0; color: #ff9800; }
}

.result-section {
  margin-bottom: 20px;
}

.result-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  gap: 8px;

  &.success { background: #e8f5e9; color: #4caf50; }
  &.fail { background: #ffebee; color: #f44336; }

  .result-text { font-size: 18px; font-weight: bold; }
  .similarity-text { font-size: 14px; }
}

.action-section {
  .van-button { margin-bottom: 12px; }
}

.retry-tip {
  text-align: center;
  font-size: 12px;
  color: #999;
}

.loading-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  gap: 12px;
}

.error-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.8);
  color: #fff;
  gap: 12px;
  padding: 20px;
  text-align: center;
}

.flash-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: #fff;
  opacity: 0.8;
  animation: flash 0.3s ease-out;
}

@keyframes flash {
  0% { opacity: 0.8; }
  100% { opacity: 0; }
}
</style>
