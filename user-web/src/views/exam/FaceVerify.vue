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
            muted
            :class="{ 'camera-error': cameraError }"
          ></video>
          <canvas ref="canvas" style="display:none"></canvas>

          <div class="face-overlay" v-if="!cameraError">
            <div class="face-frame" :class="{
              'face-detected': faceDetected,
              'face-mismatch': faceMismatch
            }">
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
          :disabled="!faceDetected || cameraError || loading"
          @click="handleVerify"
        >
          {{ verifying ? '比对中...' : '开始验证' }}
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
import { getFaceConfig, getIdPhoto, submitVerifyResult, getFaceStatus } from '@/api/face'

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
      loadingMessage: '正在加载AI模型...',
      cameraError: false,
      cameraErrorMessage: '',
      verifying: false,
      faceDetected: false,
      faceMismatch: false,

      faceDescriptor: null,
      idPhotoDescriptor: null,
      idPhotoLoaded: false,

      verifyResult: null,
      similarity: null,
      retryCount: 0,

      stream: null,
      detectInterval: null,

      // 证件照缺失时允许跳过(记录为验证失败)
      allowSkip: false
    }
  },

  computed: {
    statusMessage() {
      if (this.loading) return this.loadingMessage
      if (this.cameraError) return this.cameraErrorMessage
      if (this.verifying) return '正在比对人脸特征...'
      if (this.faceDetected) return '人脸已定位，点击验证'
      if (this.faceMismatch) return '请正对摄像头，确保面部清晰'
      return '请正对摄像头'
    },
    statusIcon() {
      if (this.faceDetected) return 'success'
      if (this.cameraError || this.faceMismatch) return 'warning-o'
      return 'info-o'
    },
    statusClass() {
      if (this.faceDetected) return 'status-success'
      if (this.cameraError || this.faceMismatch) return 'status-warning'
      return 'status-normal'
    },
    similarityPercent() {
      if (this.similarity === null) return 0
      return Math.max(0, Math.round((1 - this.similarity) * 100))
    }
  },

  async mounted() {
    await this.initFaceVerify()
  },

  beforeDestroy() {
    this.stopCamera()
    if (this.detectInterval) {
      clearInterval(this.detectInterval)
    }
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
      // 证件照缺失时跳过验证,向后端提交一条"验证失败"记录
      try {
        await submitVerifyResult({
          examId: this.examId,
          similarity: null,
          passed: false,
          deviceInfo: navigator.userAgent
        })
      } catch (e) {
        // 提交失败不阻塞考试
        console.warn('跳过验证记录提交失败:', e)
      }
      this.$toast('已跳过人脸验证，正在进入考试...')
      setTimeout(() => this.goToExam(), 1000)
    },

    async initFaceVerify() {
      try {
        const configRes = await getFaceConfig()
        this.config = configRes.data || this.config

        // 如果人脸识别未启用，直接进入考试
        if (!this.config.enabled) {
          this.goToExam()
          return
        }

        const statusRes = await getFaceStatus(this.examId)
        if (statusRes.data && statusRes.data.verified) {
          this.goToExam()
          return
        }

        await this.loadModels()
        await this.loadIdPhoto()
        await this.initCamera()
      } catch (err) {
        console.error('初始化失败:', err)
        this.cameraError = true
        let msg = err.message || '初始化失败'
        // 后端返回的通用错误给用户更友好的提示
        if (msg.includes('系统异常')) {
          msg = '服务暂时不可用，请稍后刷新重试。如反复出现请联系管理员检查系统配置'
        }
        this.cameraErrorMessage = msg
      } finally {
        this.loading = false
      }
    },

    /**
     * 动态加载 face-api.js 脚本(带重试)
     * 不依赖 index.html 的 <script> 标签,避免 CDN 被墙或加载顺序问题
     */
    async ensureFaceApiLoaded() {
      // 已加载则直接返回
      if (window.faceapi && window.faceapi.nets) {
        return window.faceapi
      }

      // 查找是否已有 script 标签(可能正在加载中)
      const existing = document.querySelector('script[data-face-api]')
      if (existing) {
        // 等待已有标签加载完成
        await new Promise((resolve, reject) => {
          existing.addEventListener('load', resolve)
          existing.addEventListener('error', () => reject(new Error('人脸识别脚本加载失败')))
        })
        if (window.faceapi) return window.faceapi
        throw new Error('人脸识别组件初始化失败')
      }

      // 动态创建 script 标签加载本地文件
      const script = document.createElement('script')
      script.src = (process.env.BASE_URL || '/') + 'js/face-api.min.js'
      script.setAttribute('data-face-api', '1')
      script.async = false

      await new Promise((resolve, reject) => {
        script.addEventListener('load', resolve)
        script.addEventListener('error', () => reject(new Error('人脸识别脚本文件加载失败，请检查网络后刷新重试')))
        document.head.appendChild(script)
      })

      // 等待 faceapi 对象可用(脚本加载后可能需要一帧时间初始化)
      let retries = 0
      while (!window.faceapi && retries < 20) {
        await new Promise(r => setTimeout(r, 50))
        retries++
      }

      if (!window.faceapi) {
        throw new Error('人脸识别组件初始化超时，请刷新页面重试')
      }
      return window.faceapi
    },

    async loadModels() {
      this.loadingMessage = '正在加载人脸识别组件...'
      const faceapi = await this.ensureFaceApiLoaded()

      this.loadingMessage = '正在加载AI模型...'
      const MODEL_URL = (process.env.BASE_URL || '/') + 'models'

      // 逐个加载模型,便于定位哪个模型加载失败
      const modelNames = [
        { net: faceapi.nets.ssdMobilenetv1, label: '人脸检测模型' },
        { net: faceapi.nets.faceLandmark68Net, label: '人脸特征点模型' },
        { net: faceapi.nets.faceRecognitionNet, label: '人脸识别模型' }
      ]
      for (const m of modelNames) {
        this.loadingMessage = '正在加载' + m.label + '...'
        try {
          // 30秒超时,防止网络问题导致无限等待
          await Promise.race([
            m.net.loadFromUri(MODEL_URL),
            new Promise((_, reject) =>
              setTimeout(() => reject(new Error(m.label + '加载超时，请检查网络后刷新重试')), 30000)
            )
          ])
        } catch (err) {
          console.error(m.label + '加载失败:', err)
          throw new Error(m.label + '加载失败，请检查网络后刷新重试')
        }
      }
    },

    async loadIdPhoto() {
      this.loadingMessage = '正在加载证件照...'
      const res = await getIdPhoto()
      const data = res.data || {}

      if (data.hasPhoto !== 'true') {
        // 证件照缺失:允许跳过验证(记录为失败),不卡死学生
        this.allowSkip = true
        throw new Error(data.message || '未找到证件照，可点击下方跳过验证直接进入考试')
      }

      const faceapi = await this.ensureFaceApiLoaded()
      const img = await faceapi.fetchImage(data.photoUrl)
      const detection = await faceapi
        .detectSingleFace(img)
        .withFaceLandmarks()
        .withFaceDescriptor()

      if (!detection) {
        throw new Error('证件照中未检测到人脸，请联系管理员')
      }

      this.idPhotoDescriptor = detection.descriptor
      this.idPhotoLoaded = true
    },

    async initCamera() {
      this.loadingMessage = '正在启动摄像头...'

      try {
        this.stream = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: 'user',
            width: { ideal: 640 },
            height: { ideal: 480 }
          },
          audio: false
        })

        this.$refs.video.srcObject = this.stream

        await new Promise((resolve) => {
          this.$refs.video.onloadedmetadata = resolve
        })

        this.startFaceDetection()
      } catch (err) {
        this.cameraError = true
        this.cameraErrorMessage = '无法访问摄像头，请检查权限设置'
        throw err
      }
    },

    startFaceDetection() {
      const video = this.$refs.video
      const faceapi = window.faceapi

      this.detectInterval = setInterval(async () => {
        if (video.readyState !== video.HAVE_ENOUGH_DATA) return

        try {
          const detection = await faceapi
            .detectSingleFace(video)
            .withFaceLandmarks()
            .withFaceDescriptor()

          if (detection) {
            this.faceDetected = true
            this.faceMismatch = false
            this.faceDescriptor = detection.descriptor
          } else {
            this.faceDetected = false
          }
        } catch (e) {
          // ignore
        }
      }, 200)
    },

    async handleVerify() {
      if (!this.faceDescriptor || !this.idPhotoLoaded) {
        this.$toast('请等待人脸加载完成')
        return
      }

      this.verifying = true

      try {
        const faceapi = window.faceapi
        const distance = faceapi.euclideanDistance(
          this.faceDescriptor,
          this.idPhotoDescriptor
        )

        this.similarity = distance
        const passed = distance < this.config.threshold
        this.verifyResult = passed

        await submitVerifyResult({
          examId: this.examId,
          similarity: distance,
          passed: passed,
          deviceInfo: navigator.userAgent
        })

        if (passed) {
          this.$toast('验证通过，即将进入考试...')
          setTimeout(() => this.goToExam(), 1500)
        } else {
          this.retryCount++
          if (this.retryCount >= this.config.maxRetries) {
            this.$toast.fail('验证失败次数过多，请联系管理员')
          } else {
            this.$toast.fail('验证失败，请重试')
          }
        }
      } catch (err) {
        this.$toast.fail('验证出错：' + (err.message || '未知错误'))
      } finally {
        this.verifying = false
      }
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
  aspect-ratio: 4/3;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);

  video {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transform: scaleX(-1);
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
    width: 30px;
    height: 30px;
    border-color: rgba(255, 255, 255, 0.5);
    border-style: solid;
    border-width: 0;

    &.top-left {
      top: 0; left: 0;
      border-top-width: 3px;
      border-left-width: 3px;
    }
    &.top-right {
      top: 0; right: 0;
      border-top-width: 3px;
      border-right-width: 3px;
    }
    &.bottom-left {
      bottom: 0; left: 0;
      border-bottom-width: 3px;
      border-left-width: 3px;
    }
    &.bottom-right {
      bottom: 0; right: 0;
      border-bottom-width: 3px;
      border-right-width: 3px;
    }
  }

  &.face-detected .corner {
    border-color: #1989fa;
    box-shadow: 0 0 8px rgba(25, 137, 250, 0.5);
  }

  &.face-mismatch .corner {
    border-color: #ee0a24;
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
</style>
