<template>
  <div class="floating-qrcode" v-if="showQrcode" @click="handleClick">
    <a :href="linkUrl" target="_blank" rel="noopener" v-if="linkUrl">
      <img :src="qrcodeSrc" alt="客服二维码" @error="onError" />
    </a>
    <img v-else :src="qrcodeSrc" alt="客服二维码" @error="onError" />
    <p class="floating-qrcode-text">扫码咨询</p>
  </div>
</template>

<script>
import { getAboutUs } from '@/api/profile'
import { resolveImg } from '@/utils/apiBase'

export default {
  name: 'FloatingQrcode',
  data() {
    return {
      aboutData: {},
      showQrcode: false,
      hasError: false
    }
  },
  computed: {
    qrcodeSrc() {
      if (this.hasError) return ''
      // 优先使用上传的二维码图片
      if (this.aboutData.serviceQrcode) {
        return resolveImg(this.aboutData.serviceQrcode)
      }
      // 其次使用根据url生成的二维码
      if (this.aboutData.qrcodeLink) {
        return '/api/public/about/qrcode'
      }
      return ''
    },
    linkUrl() {
      return this.aboutData.qrcodeLink || ''
    }
  },
  created() {
    this.fetchAbout()
  },
  methods: {
    async fetchAbout() {
      try {
        const res = await getAboutUs()
        this.aboutData = (res && res.data) || {}
        // 有上传图片或配置了链接才显示
        this.showQrcode = !!(this.aboutData.serviceQrcode || this.aboutData.qrcodeLink)
      } catch (e) {
        this.showQrcode = false
      }
    },
    onError() {
      this.hasError = true
      this.showQrcode = false
    },
    handleClick() {
      // 无链接时不跳转
    }
  }
}
</script>

<style lang="scss" scoped>
.floating-qrcode {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 999;
  background: #fff;
  border-radius: 10px;
  padding: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  text-align: center;
  transition: transform 0.2s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  }

  img {
    display: block;
    width: 84px;
    height: 84px;
    border-radius: 6px;
  }

  .floating-qrcode-text {
    margin: 5px 0 0;
    font-size: 11px;
    color: #666;
    line-height: 1.4;
  }
}

@media (max-width: 600px) {
  .floating-qrcode {
    bottom: 14px;
    right: 14px;
    padding: 8px;

    img {
      width: 60px;
      height: 60px;
    }

    .floating-qrcode-text {
      font-size: 10px;
    }
  }
}
</style>
