<template>
  <div class="about-page">
    <Header />

    <div class="page-body">
      <div class="container">
        <div class="about-card">
          <!-- 标题 -->
          <div class="about-header">
            <h1 class="app-name">中国人力资源专业技能人才评价中心</h1>
          </div>

          <!-- 关于我们(后台数据) -->
          <div class="section" v-if="aboutData.content">
            <div class="section-title">关于我们</div>
            <div class="section-text rich-content" v-html="aboutData.content"></div>
          </div>

          <!-- 联系我们 -->
          <div class="section" v-if="aboutData.servicePhone || aboutData.serviceQrcode">
            <div class="section-title">联系我们</div>
            <div class="contact-list">
              <div class="contact-item" v-if="aboutData.servicePhone">
                <van-icon name="phone-o" size="18" color="#1989fa" />
                <span class="label">客服电话：</span>
                <span class="value">{{ aboutData.servicePhone }}</span>
              </div>
              <div class="contact-item" v-if="aboutData.serviceEmail">
                <van-icon name="envelop-o" size="18" color="#1989fa" />
                <span class="label">邮箱：</span>
                <span class="value">{{ aboutData.serviceEmail }}</span>
              </div>
              <div class="contact-item" v-if="aboutData.serviceAddress">
                <van-icon name="location-o" size="18" color="#1989fa" />
                <span class="label">地址：</span>
                <span class="value">{{ aboutData.serviceAddress }}</span>
              </div>
              <div class="contact-item" v-if="aboutData.serviceQrcode">
                <div class="qrcode-img">
                  <img :src="resolveImg(aboutData.serviceQrcode)" alt="客服二维码" />
                </div>
              </div>
            </div>
          </div>

          <!-- 无数据时显示默认内容 -->
          <template v-if="!aboutData.content && !aboutData.servicePhone">
            <div class="section">
              <div class="section-title">平台介绍</div>
              <p class="section-text">
                中国人力资源专业技能人才评价中心是面向职业能力标准评价与人才评测的综合性在线平台，提供课程学习、在线考试、证书查询与下载、错题练习等一站式服务。
              </p>
            </div>
            <div class="section">
              <div class="section-title">联系我们</div>
              <div class="contact-list">
                <div class="contact-item">
                  <van-icon name="phone-o" size="18" color="#1989fa" />
                  <span class="label">客服电话：</span>
                  <span class="value">400-888-8888</span>
                </div>
                <div class="contact-item">
                  <van-icon name="envelop-o" size="18" color="#1989fa" />
                  <span class="label">邮箱：</span>
                  <span class="value">support@gjzynlts.com</span>
                </div>
              </div>
            </div>
          </template>

          <!-- 证书说明(后台配置) -->
          <div v-if="aboutData.disclaimer" class="section">
            <div class="section-title">证书说明</div>
            <div class="section-text" v-html="aboutData.disclaimer"></div>
          </div>

          <!-- 版权信息 -->
          <div class="copyright">
            <p>Copyright &copy; {{ year }} 中国人力资源专业技能人才评价中心</p>
            <!-- 备案信息 -->
            <div class="beian">
              <span>香港政府注册登记号：78503955-000-07-25-4</span>
              <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener">
                冀ICP备2025108945号-2
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import { getAboutUs } from '@/api/profile'
import { resolveImg } from '@/utils/apiBase'

export default {
  name: 'About',
  components: { Header },
  data() {
    return {
      version: '1.0.0',
      year: new Date().getFullYear(),
      aboutData: {}
    }
  },
  created() {
    this.fetchAbout()
  },
  methods: {
    resolveImg,
    async fetchAbout() {
      try {
        const res = await getAboutUs()
        this.aboutData = (res && res.data) || {}
      } catch (e) {
        this.aboutData = {}
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.about-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-body {
  padding-top: var(--header-height, 170px);
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 20px;
}

.about-card {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.about-header {
  text-align: center;
  padding-bottom: 32px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 32px;

  .app-name {
    font-size: 26px;
    font-weight: bold;
    color: #333;
    margin-bottom: 8px;
  }

  .app-version {
    font-size: 14px;
    color: #999;
  }
}

.section {
  margin-bottom: 32px;

  .section-title {
    font-size: 18px;
    font-weight: bold;
    color: #333;
    margin-bottom: 14px;
    padding-left: 12px;
    border-left: 4px solid #1989fa;
  }

  .section-text {
    font-size: 14px;
    color: #666;
    line-height: 1.9;
  }

  .section-text.rich-content {
    word-break: break-word;
    line-height: 1.85;
    color: #333;

    ::v-deep p { margin: 8px 0; }
    ::v-deep img { max-width: 100%; border-radius: 4px; }
    ::v-deep ol, ::v-deep ul { padding-left: 20px; }
  }
}

.contact-list {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .contact-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;

    .label {
      color: #999;
    }

    .value {
      color: #333;
    }
  }

  .qrcode-img {
    margin-top: 8px;

    img {
      width: 72px;
      height: 72px;
      border-radius: 6px;
      border: 1px solid #eee;
    }
  }
}

.copyright {
  text-align: center;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;

  p {
    font-size: 13px;
    color: #999;
    line-height: 1.8;
  }

  .beian {
    margin-top: 12px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #999;

    a {
      color: #999;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 4px;

      &:hover {
        color: #1989fa;
      }
    }

    .beian-icon {
      width: 16px;
      height: 16px;
      vertical-align: middle;
    }
  }
}

@media (max-width: 600px) {
  .about-card {
    padding: 24px 20px;
  }
}
</style>
