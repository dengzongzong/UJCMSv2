<template>
  <div class="home-page">
    <Header @subject-change="onSubjectChange" />

    <div class="page-body">
      <!-- ========== Banner + 新闻侧栏 (左大图 + 右Tab) ========== -->
      <div class="banner-section">
        <div class="banner-wrapper">
          <!-- 左侧轮播图 -->
          <div class="banner-left">
            <van-swipe class="banner-swipe" :autoplay="4000" :show-indicators="true" indicator-color="#ffffff">
              <van-swipe-item v-for="(item, index) in banners" :key="item.id || index">
                <img :src="resolveImg(item.imageUrl || item.image)" class="banner-img" loading="lazy" decoding="async" @click="onBannerClick(item)" />
              </van-swipe-item>
              <van-swipe-item v-if="banners.length === 0">
                <div class="banner-placeholder">
                  <van-icon name="photo-o" size="48" />
                  <p>暂无轮播图</p>
                </div>
              </van-swipe-item>
            </van-swipe>
          </div>
          <!-- 右侧新闻Tab -->
          <div class="banner-right">
            <div class="news-tabs">
              <div :class="['news-tab', { active: newsTab === 'news' }]" @click="newsTab = 'news'">新闻动态</div>
              <div :class="['news-tab', { active: newsTab === 'events' }]" @click="newsTab = 'events'">重大活动</div>
              <div :class="['news-tab', { active: newsTab === 'announcement' }]" @click="newsTab = 'announcement'">通知公告</div>
              <div class="news-more" @click="goNewsCenter">更多 &gt;&gt;</div>
            </div>
            <div class="news-tab-content">
              <ul v-if="newsTab === 'news'" class="news-sidebar-list">
                <li v-for="item in displayNews" :key="item.id" @click="onNewsClick(item)">
                  <svg class="news-icon" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M3 1.5h7L13 4.5V14.5H3z" stroke="#c41e3a" stroke-width="1.2"/>
                    <path d="M10 1.5V4.5H13" stroke="#c41e3a" stroke-width="1.2"/>
                    <path d="M5 7.5h6M5 9.5h6M5 11.5h4" stroke="#c41e3a" stroke-width="1.2" stroke-linecap="round"/>
                  </svg>
                  <span class="news-text">{{ item.title }}</span>
                  <span class="news-date">{{ formatDate(item.publishTime || item.createTime) }}</span>
                </li>
                <li v-if="newsList.length === 0" class="news-empty">暂无新闻</li>
              </ul>
              <ul v-else-if="newsTab === 'events'" class="news-sidebar-list">
                <li v-for="item in displayEvents" :key="'e'+item.id" @click="onNewsClick(item)">
                  <svg class="news-icon" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M3 1.5h7L13 4.5V14.5H3z" stroke="#c41e3a" stroke-width="1.2"/>
                    <path d="M10 1.5V4.5H13" stroke="#c41e3a" stroke-width="1.2"/>
                    <path d="M5 7.5h6M5 9.5h6M5 11.5h4" stroke="#c41e3a" stroke-width="1.2" stroke-linecap="round"/>
                  </svg>
                  <span class="news-text">{{ item.title }}</span>
                  <span class="news-date">{{ formatDate(item.publishTime || item.createTime) }}</span>
                </li>
                <li v-if="eventsList.length === 0" class="news-empty">暂无活动</li>
              </ul>
              <ul v-else class="news-sidebar-list">
                <li v-for="item in displayAnnouncements" :key="item.id" @click="onAnnouncementClick(item)">
                  <svg class="news-icon" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M3 1.5h7L13 4.5V14.5H3z" stroke="#c41e3a" stroke-width="1.2"/>
                    <path d="M10 1.5V4.5H13" stroke="#c41e3a" stroke-width="1.2"/>
                    <path d="M5 7.5h6M5 9.5h6M5 11.5h4" stroke="#c41e3a" stroke-width="1.2" stroke-linecap="round"/>
                  </svg>
                  <span class="news-text">{{ item.title }}</span>
                  <span class="news-date">{{ formatDate(item.publishTime || item.createTime) }}</span>
                </li>
                <li v-if="announcements.length === 0" class="news-empty">暂无公告</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 横幅图片(轮播图下方) ========== -->
      <div v-if="bannerImages.length > 0" class="homepage-banner-wrap">
        <div
          v-for="banner in bannerImages"
          :key="banner.id"
          class="homepage-banner-item"
          @click="onBannerImageClick(banner)"
        >
          <img :src="resolveImg(banner.imageUrl)" class="homepage-banner-img" loading="lazy" decoding="async" />
          <div v-if="banner.title" class="homepage-banner-title">{{ banner.title }}</div>
        </div>
      </div>

      <div class="container">
        <!-- ========== 推荐课程 ========== -->
        <div class="course-section">
          <div class="section-header">
            <span class="section-title">推荐课程</span>
            <span v-if="courseList.length > displayLimit" class="section-more" @click="loadMore">更多课程 &gt;&gt;</span>
          </div>
          <div class="course-grid">
            <div
              v-for="course in displayCourses"
              :key="course.id"
              class="course-card"
              @click="goCourseDetail(course.id)"
            >
              <div class="course-cover">
                <img :src="resolveImg(course.coverUrl || course.coverImage || course.thumbnail || defaultCover)" :alt="course.name" loading="lazy" decoding="async" />
                <div v-if="course.progress > 0" class="progress-tag">已学{{ course.progress }}%</div>
                <div v-if="!course.purchased && loggedIn" class="locked-tag">
                  <van-icon name="lock" /> 未开通
                </div>
                <div v-if="course.purchased" class="cover-mask">
                  <van-icon name="play-circle-o" size="40" color="#fff" />
                </div>
              </div>
              <div class="course-info">
                <div class="course-name">{{ course.name }}</div>
                <div v-if="course.progress > 0" class="course-progress">
                  <van-progress :percentage="course.progress" :show-pivot="false" color="#c41e3a" stroke-width="3" />
                  <span class="progress-text">已学习 {{ course.progress }}%</span>
                </div>
                <div class="course-stats">
                  <van-icon name="friends-o" size="14" color="#909399" />
                  <span>已有 {{ course.studyCount || 0 }} 人学过</span>
                  <span class="stat-divider">·</span>
                  <span>学时：{{ course.studyHours || 0 }}</span>
                </div>
                <div class="course-footer">
                  <div class="course-price">
                    <span v-if="course.price > 0" class="price-value">¥{{ course.price }}</span>
                    <span v-else class="price-free">免费</span>
                  </div>
                  <van-button size="small" type="danger" round icon="eye-o" @click.stop="goCourseDetail(course.id)">查看详情</van-button>
                </div>
              </div>
            </div>
          </div>
          <van-empty v-if="courseList.length === 0 && !loading" description="暂无课程" />
        </div>

        <!-- ========== 政策法规 + 信息公开（两栏） ========== -->
        <div class="info-section">
          <!-- 政策法规 -->
          <div class="info-panel">
            <div class="panel-header">
              <span class="panel-title">政策法规</span>
              <span class="panel-more" @click="loadMoreNews">更多 &gt;&gt;</span>
            </div>
            <ul class="panel-list">
              <li v-for="item in displayPolicyNews" :key="'p'+item.id" class="panel-item" @click="onSectionClick(item, '政策法规')">
                <span class="item-dot"></span>
                <span class="item-text">{{ item.title }}</span>
                <span class="item-date">{{ formatDate(item.publishTime || item.createTime) }}</span>
              </li>
              <li v-if="displayPolicyNews.length === 0" class="panel-empty">暂无内容</li>
            </ul>
          </div>
          <!-- 信息公开 -->
          <div class="info-panel">
            <div class="panel-header">
              <span class="panel-title">信息公开</span>
              <span class="panel-more" @click="loadMoreAnnouncements">更多 &gt;&gt;</span>
            </div>
            <ul class="panel-list">
              <li v-for="item in displayDisclosureNews" :key="'d'+item.id" class="panel-item" @click="onSectionClick(item, '信息公开')">
                <span class="item-dot"></span>
                <span class="item-text">{{ item.title }}</span>
                <span class="item-date">{{ formatDate(item.publishTime || item.createTime) }}</span>
              </li>
              <li v-if="displayDisclosureNews.length === 0" class="panel-empty">暂无内容</li>
            </ul>
          </div>
        </div>

        <!-- ========== 测评服务平台 ========== -->
        <div v-if="videoImages.length" class="evaluate-section">
          <div class="section-header">
            <span class="section-title">测评服务平台</span>
          </div>
          <div class="evaluate-row">
            <div
              v-for="img in videoImages"
              :key="img.id"
              class="evaluate-item"
              @click="onVideoImageClick(img)"
            >
              <template v-if="img.imageUrl">
                <div class="evaluate-image-wrapper">
                  <img :src="resolveImg(img.imageUrl)" :alt="img.description ? stripHtml(img.description) : ''" class="evaluate-image" loading="lazy" decoding="async" />
                </div>
                <div v-if="img.description" class="evaluate-desc" v-html="processRichContent(img.description)"></div>
              </template>
              <template v-else>
                <div class="evaluate-text-content">
                  <div v-if="img.description" class="evaluate-desc" v-html="processRichContent(img.description)"></div>
                  <div v-else class="evaluate-title">{{ img.title || '' }}</div>
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- ========== 相关链接 ========== -->
        <div v-if="friendlyLinks.length" class="related-section">
          <div class="section-header">
            <span class="section-title">友情链接</span>
          </div>
          <div class="related-grid">
            <a
              v-for="link in friendlyLinks"
              :key="link.id"
              :href="link.linkUrl"
              target="_blank"
              rel="noopener"
              class="related-item"
            >
              <img v-if="link.imageUrl" :src="resolveImg(link.imageUrl)" :alt="link.name" class="related-img" loading="lazy" decoding="async" />
              <div v-else class="related-img related-placeholder">
                <van-icon name="link-o" size="32" color="#999" />
              </div>
            </a>
          </div>
        </div>

        <!-- ========== 政府网站链接 ========== -->
        <div class="gov-links-section">
          <div class="gov-links-header">
            <span class="gov-links-title">友情链接</span>
          </div>
          <div class="gov-links-body">
            <select class="gov-select" @change="onGovLinkChange($event, 1)">
              <option value="">国家各部委机关网站</option>
              <option value="http://www.mohrss.gov.cn">人力资源和社会保障部</option>
              <option value="http://www.mfa.gov.cn">外交部</option>
              <option value="http://www.ndrc.gov.cn">发展改革委</option>
              <option value="http://www.moe.gov.cn">教育部</option>
              <option value="http://www.most.gov.cn">科技部</option>
              <option value="http://www.mps.gov.cn">公安部</option>
              <option value="http://www.mca.gov.cn">民政部</option>
              <option value="http://www.moj.gov.cn">司法部</option>
              <option value="http://www.mof.gov.cn">财政部</option>
              <option value="http://www.mlr.gov.cn">国土资源部</option>
              <option value="http://www.moc.gov.cn">交通部</option>
              <option value="http://www.mwr.gov.cn">水利部</option>
              <option value="http://www.agri.gov.cn">农业部</option>
              <option value="http://www.mofcom.gov.cn">商务部</option>
              <option value="http://www.moh.gov.cn">卫生部</option>
              <option value="http://www.pbc.gov.cn">人民银行</option>
              <option value="http://www.sasac.gov.cn">国资委</option>
              <option value="http://www.customs.gov.cn">海关总署</option>
              <option value="http://www.chinatax.gov.cn">税务总局</option>
              <option value="http://www.stats.gov.cn">统计局</option>
              <option value="http://www.sda.gov.cn">食品药品监管局</option>
            </select>
            <select class="gov-select" @change="onGovLinkChange($event, 2)">
              <option value="">人力资源和社会保障部部属网站</option>
              <option value="http://www.mohrss.gov.cn">人力资源和社会保障部门户网</option>
              <option value="http://www.newjobs.com.cn">中国国家人才网</option>
              <option value="http://www.chrm.gov.cn">中国人力资源市场网</option>
              <option value="http://www.cpta.com.cn">中国人事考试网</option>
              <option value="http://www.chinahra.org">中国人才交流协会</option>
              <option value="http://www.rky.org.cn">中国人事科学研究网</option>
              <option value="http://www.chinatest.com.cn">中国国家人才测评网</option>
              <option value="http://www.cacee.org.cn">中国继续工程教育协会网</option>
              <option value="http://www.chinatalents.gov.cn">中国留学人才信息网</option>
              <option value="http://www.rensb.com">中国人事报</option>
            </select>
            <select class="gov-select" @change="onGovLinkChange($event, 3)">
              <option value="">各省市人事厅网站</option>
              <option value="http://www.bjp.gov.cn">北京人事人才信息网</option>
              <option value="http://www.tjpnet.gov.cn">天津人事信息网</option>
              <option value="http://www.hebrs.gov.cn">河北人事人才网</option>
              <option value="http://www.sxsrs.gov.cn">山西人事人才网</option>
              <option value="http://www.nmrc.com.cn">内蒙古人事人才网</option>
              <option value="http://www.lnrc.com.cn">辽宁人事人才信息网</option>
              <option value="http://rst.jl.gov.cn">吉林人事厅</option>
              <option value="http://www.21cnhr.gov.cn">上海人事21世纪人才网</option>
              <option value="http://www.jsrsrc.gov.cn">江苏人事人才公共服务网</option>
              <option value="http://www.zjrs.gov.cn">浙江人事编制网</option>
              <option value="http://www.sdrs.gov.cn">山东人事信息网</option>
              <option value="http://www.hnrs.gov.cn">河南省人事厅</option>
              <option value="http://www.hbrs.gov.cn">湖北人事信息网</option>
              <option value="http://www.gdrst.gov.cn">广东省人事厅</option>
              <option value="http://www.scrs.gov.cn">四川人事信息网</option>
            </select>
            <select class="gov-select" @change="onGovLinkChange($event, 4)">
              <option value="">各省市人力资源市场网站</option>
              <option value="http://www.job168.com">南方人才网</option>
              <option value="http://www.bjrc.com">北京人才市场</option>
              <option value="http://www.hr.net.cn">上海人才市场</option>
              <option value="http://www.cqrc.net">重庆人才大市场</option>
              <option value="http://www.zjrc.com">浙江人才市场</option>
              <option value="http://www.sdrc.com.cn">山东人才市场</option>
              <option value="http://www.gdrc.com">广东人才市场</option>
              <option value="http://www.scrc168.com">四川人才市场</option>
            </select>
          </div>
        </div>
      </div>

      <!-- ========== 底部链接 + 分割线 ========== -->
      <div v-if="friendlyLinks.length" class="footer-links">
        <span class="footer-link" @click="openDialog('cooperation')">合作咨询</span>
        <span class="footer-link-sep">|</span>
        <span class="footer-link" @click="openDialog('declaration')">网站声明</span>
        <span class="footer-link-sep">|</span>
        <span class="footer-link" @click="openDialog('complaint')">投诉建议</span>
      </div>
      <div v-else class="footer-links">
        <span class="footer-link" @click="openDialog('cooperation')">合作咨询</span>
        <span class="footer-link-sep">|</span>
        <span class="footer-link" @click="openDialog('declaration')">网站声明</span>
        <span class="footer-link-sep">|</span>
        <span class="footer-link" @click="openDialog('complaint')">投诉建议</span>
      </div>
      <div class="section-divider"></div>

      <!-- 弹窗 -->
      <CooperationDialog v-model="dialogState.cooperation" />
      <DeclarationDialog v-model="dialogState.declaration" />
      <ComplaintDialog v-model="dialogState.complaint" />

      <!-- 底部版权 -->
      <div class="footer">
        <div class="footer-copyright">© 中国人力资源专业技能人才评价中心</div>
        <div class="footer-beian">
          <span>香港政府注册登记号：78503955-000-07-25-4</span>
          <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener">
            冀ICP备2025108945号-2
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import CooperationDialog from '@/components/CooperationDialog.vue'
import DeclarationDialog from '@/components/DeclarationDialog.vue'
import ComplaintDialog from '@/components/ComplaintDialog.vue'
import { getBanners, getBannerImages, getAnnouncements, getNewsList, getEventsList, getFriendlyLinks, getCourseThreeImages, getPublicCourseList, getHomepageSections, getHomepageAggregated, getHomepageSectionDetail } from '@/api/home'
import { resolveImg, processRichContent } from '@/utils/apiBase'
import { Toast, Dialog } from 'vant'

export default {
  name: 'HomeIndex',
  components: { Header, CooperationDialog, DeclarationDialog, ComplaintDialog },
  data() {
    return {
      banners: [],
      bannerImages: [],
      courseList: [],
      announcements: [],
      newsList: [],
      eventsList: [],
      friendlyLinks: [],
      videoImages: [],
      loading: false,
      page: 1,
      displayLimit: 8,
      newsTab: 'news',
      policySections: [],
      disclosureSections: [],
      dialogState: {
        cooperation: false,
        declaration: false,
        complaint: false
      },
      defaultCover: 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="120" height="80"><rect fill="#e8e8e8" width="120" height="80"/><text x="50%" y="50%" text-anchor="middle" dy=".3em" fill="#999" font-size="12">课程封面</text></svg>')
    }
  },
  computed: {
    currentSubjectId() {
      return this.$store.getters.professionId
    },
    loggedIn() {
      return !!this.$store.getters.token
    },
    displayCourses() {
      return (this.courseList || []).slice(0, this.displayLimit)
    },
    displayNews() {
      return (this.newsList || []).slice(0, 7)
    },
    displayEvents() {
      return (this.eventsList || []).slice(0, 7)
    },
    displayAnnouncements() {
      return (this.announcements || []).slice(0, 7)
    },
    displayPolicyNews() {
      return (this.policySections || []).slice(0, 5)
    },
    displayDisclosureNews() {
      return (this.disclosureSections || []).slice(0, 5)
    }
  },
  created() {
    // 证书查询参数跳转已移至路由守卫 beforeEach,此处无需处理
    this.fetchBanners()
    this.fetchBannerImages()
    this.fetchCourses()
    this.fetchHomepageAggregated()
    this.fetchFriendlyLinks()
    this.fetchVideoImages()
  },
  methods: {
    resolveImg,
    processRichContent,
    onSubjectChange() {
      this.fetchCourses()
    },
    formatDate(date) {
      if (!date) return ''
      const d = new Date(date)
      const yyyy = d.getFullYear()
      const mm = String(d.getMonth() + 1).padStart(2, '0')
      const dd = String(d.getDate()).padStart(2, '0')
      return `${yyyy}-${mm}-${dd}`
    },
    async fetchHomepageAggregated() {
      try {
        const res = await getHomepageAggregated()
        const data = res.data || res || {}
        // 新闻动态
        const newsData = data.news || []
        this.newsList = Array.isArray(newsData) ? newsData : (newsData.list || newsData.records || [])
        // 重大活动
        const eventsData = data.events || []
        this.eventsList = Array.isArray(eventsData) ? eventsData : (eventsData.list || eventsData.records || [])
        // 通知公告
        const annoData = data.announcements || []
        this.announcements = Array.isArray(annoData) ? annoData : (annoData.list || annoData.records || [])
        // 政策法规 + 信息公开 (合并后从新闻表读取: type=8政策法规, type=9信息公开)
        const sectionData = data.homepageSections || []
        const sectionList = Array.isArray(sectionData) ? sectionData : (sectionData.list || [])
        this.policySections = sectionList.filter(item => item.type === 8)
        this.disclosureSections = sectionList.filter(item => item.type === 9)
      } catch (error) {
        // 聚合接口失败时回退到独立接口
        this.fetchAnnouncements()
        this.fetchNews()
        this.fetchEvents()
        this.fetchHomepageSections()
      }
    },
    async fetchHomepageSections() {
      try {
        const res = await getHomepageSections()
        const data = res.data || res || []
        const list = Array.isArray(data) ? data : (data.list || [])
        this.policySections = list.filter(item => item.type === 8)
        this.disclosureSections = list.filter(item => item.type === 9)
      } catch (error) {
        this.policySections = []
        this.disclosureSections = []
      }
    },
    async fetchAnnouncements() {
      try {
        const res = await getAnnouncements()
        const data = res.data || res
        this.announcements = Array.isArray(data) ? data : (data.list || data.records || [])
      } catch (error) {
        this.announcements = []
      }
    },
    async fetchNews() {
      try {
        const res = await getNewsList()
        const data = res.data || res
        this.newsList = Array.isArray(data) ? data : (data.list || data.records || [])
      } catch (error) {
        this.newsList = []
      }
    },
    async fetchEvents() {
      try {
        const res = await getEventsList()
        const data = res.data || res
        this.eventsList = Array.isArray(data) ? data : (data.list || data.records || [])
      } catch (error) {
        this.eventsList = []
      }
    },
    onGovLinkChange(event, index) {
      const url = event.target.value
      if (url) {
        window.open(url, '_blank')
      }
      event.target.selectedIndex = 0
    },
    async fetchFriendlyLinks() {
      try {
        const res = await getFriendlyLinks()
        const data = res.data || res || []
        this.friendlyLinks = Array.isArray(data) ? data : (data.list || [])
      } catch (error) {
        this.friendlyLinks = []
      }
    },
    async fetchVideoImages() {
      try {
        const res = await getCourseThreeImages()
        const data = res.data || res || []
        this.videoImages = Array.isArray(data) ? data : []
      } catch (error) {
        this.videoImages = []
      }
    },
    stripHtml(html) {
      if (!html) return ''
      return html.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim()
    },
    onVideoImageClick(img) {
      const t = Number(img.linkType)
      if (t === 1 && img.linkUrl) {
        window.open(img.linkUrl, '_blank')
      } else if (t === 2 && img.linkId) {
        this.$router.push('/exam/intro/' + img.linkId).catch(() => {})
      } else if (t === 3 && img.linkId) {
        this.$router.push('/course/detail/' + img.linkId).catch(() => {})
      }
    },
    async fetchBanners() {
      try {
        const res = await getBanners()
        const data = res.data || res || []
        this.banners = Array.isArray(data) ? data : (data.list || [])
      } catch (error) {
        this.banners = []
      }
    },
    async fetchBannerImages() {
      try {
        const res = await getBannerImages()
        this.bannerImages = res.data || []
      } catch (error) {
        this.bannerImages = []
      }
    },
    onBannerImageClick(banner) {
      if (banner.linkUrl) {
        window.open(banner.linkUrl, '_blank')
      }
    },
    async fetchCourses() {
      this.loading = true
      try {
        const res = await getPublicCourseList({
          professionId: this.currentSubjectId
        })
        const data = res.data || res
        const list = Array.isArray(data) ? data : (data.list || data.records || [])
        this.courseList = list
      } catch (error) {
        console.error('获取课程列表失败:', error)
        this.courseList = []
      } finally {
        this.loading = false
      }
    },
    goCourseDetail(id) {
      if (!this.$store.getters.token) {
        Dialog.confirm({
          title: '需要先登录',
          message: '登录后即可查看课程详情并学习。是否现在登录?',
          confirmButtonText: '去登录',
          cancelButtonText: '取消'
        })
          .then(() => {
            this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
          })
          .catch(() => {})
        return
      }
      this.$router.push(`/course/detail/${id}`).catch(() => {})
    },
    onBannerClick(item) {
      if ((item.linkType === 1 || item.linkType === 2) && !this.$store.getters.token) {
        Dialog.confirm({
          title: '需要先登录',
          message: '登录后即可查看详情。是否现在登录?',
          confirmButtonText: '去登录',
          cancelButtonText: '取消'
        })
          .then(() => {
            this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
          })
          .catch(() => {})
        return
      }
      if (item.linkType === 1 && item.linkId) {
        this.$router.push(`/exam/intro/${item.linkId}`).catch(() => {})
      } else if (item.linkType === 2 && item.linkId) {
        this.goCourseDetail(item.linkId)
      } else if (item.link) {
        this.$router.push(item.link).catch(() => {})
      }
    },
    onNewsClick(item) {
      sessionStorage.setItem('news_detail_data', JSON.stringify(item))
      this.$router.push(`/news/detail/${item.id}?type=news`).catch(() => {})
    },
    onAnnouncementClick(item) {
      sessionStorage.setItem('news_detail_data', JSON.stringify(item))
      this.$router.push(`/news/detail/${item.id}?type=announcement`).catch(() => {})
    },
    async onSectionClick(item, sectionTitle) {
      // 先弹窗显示标题,异步获取content后更新
      const dialogInst = Dialog.alert({
        title: item.title || sectionTitle,
        message: '<div style="text-align:center;padding:40px 0;color:#999;">加载中...</div>',
        confirmButtonText: '关闭',
        className: 'section-detail-dialog'
      }).catch(() => {})
      try {
        const res = await getHomepageSectionDetail(item.id)
        const detail = (res.data || res) || {}
        const html = detail.content || '<p style="text-align:center;color:#999;padding:40px 0;">暂无内容</p>'
        Dialog.alert({
          title: item.title || sectionTitle,
          message: html,
          confirmButtonText: '关闭',
          className: 'section-detail-dialog'
        }).catch(() => {})
      } catch (error) {
        Dialog.alert({
          title: item.title || sectionTitle,
          message: '<p style="text-align:center;color:#999;padding:40px 0;">内容加载失败</p>',
          confirmButtonText: '关闭',
          className: 'section-detail-dialog'
        }).catch(() => {})
      }
    },
    loadMoreNews() {
      this.$router.push('/news/list').catch(() => {})
    },
    loadMoreAnnouncements() {
      this.$router.push('/news/announcements').catch(() => {})
    },
    goNewsCenter() {
      this.$router.push({ path: '/news/center', query: { tab: this.newsTab } }).catch(() => {})
    },
    loadMore() {
      this.$router.push('/course/my').catch(() => {})
    },
    openDialog(type) {
      if (this.dialogState[type] !== undefined) {
        this.dialogState[type] = true
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary-red: #c41e3a;
$primary-red-dark: #a01530;
$primary-red-light: #fff5f5;

.home-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-body {
  padding-top: var(--header-height, 178px);
}

/* ========== Banner + 新闻侧栏 ========== */
.banner-section {
  width: 80%;
  max-width: 1600px;
  margin: 0 auto 20px;
  padding: 0 20px;
  box-sizing: border-box;
}

/* ========== 横幅图片 ========== */
.homepage-banner-wrap {
  width: 80%;
  max-width: 1600px;
  margin: 0 auto 20px;
  padding: 0 20px;
  box-sizing: border-box;
  overflow: hidden;
}
.homepage-banner-item {
  position: relative;
  width: 100%;
  cursor: pointer;
  overflow: hidden;
  border-radius: 4px;
}
.homepage-banner-img {
  width: 100%;
  height: auto;
  max-width: 100%;
  display: block;
  object-fit: cover;
}
.homepage-banner-title {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
  font-size: 24px;
  font-weight: bold;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
  letter-spacing: 2px;
  white-space: nowrap;
}

.banner-wrapper {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

.banner-left {
  flex: 0 0 50%;
  min-width: 0;
  overflow: hidden;
  .banner-swipe {
    width: 100%;
    overflow: hidden;
    height: 340px;
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    ::v-deep .van-swipe {
      width: 100%;
      overflow: hidden;
    }
    ::v-deep .van-swipe__track {
      width: 100% !important;
      display: flex;
    }
    ::v-deep .van-swipe-item {
      width: 100% !important;
      flex: 0 0 100%;
    }
    ::v-deep .van-swipe__indicators { bottom: 16px; }
    ::v-deep .van-swipe__indicator {
      width: 10px;
      height: 10px;
      margin: 0 5px;
      background: rgba(255, 255, 255, 0.5);
      border: 1px solid rgba(255, 255, 255, 0.8);
    }
    ::v-deep .van-swipe__indicator--active {
      background: #ffffff;
      width: 24px;
      border-radius: 5px;
    }
  }
  .banner-img {
    width: 100%;
    height: 340px;
    object-fit: cover;
    display: block;
  }
  .banner-placeholder {
    height: 340px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, $primary-red, #e8556a);
    color: #fff;
    p { margin-top: 12px; font-size: 16px; }
  }
}

.banner-right {
  flex: 0 0 calc(50% - 16px);
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  position: relative;
  z-index: 2;
}

.news-tabs {
  display: flex;
  height: 32px;
  .news-tab {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    font-weight: 600;
    color: #666;
    cursor: pointer;
    border-bottom: 2px solid transparent;
    transition: all 0.2s;
    background: #fafafa;
    &:hover { color: $primary-red; }
    &.active {
      color: #fff;
      background: $primary-red;
      border-bottom-color: $primary-red-dark;
    }
  }
  .news-more {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 12px;
    font-size: 12px;
    color: #999;
    cursor: pointer;
    background: #fafafa;
    white-space: nowrap;
    border-bottom: 2px solid transparent;
    &:hover { color: $primary-red; }
  }
}

.news-tab-content {
  flex: 1;
  padding: 8px 0;
  overflow: hidden;
}

.news-sidebar-list {
  list-style: none;
  margin: 0;
  padding: 0;
  li {
    display: flex;
    align-items: center;
    padding: 10px 16px;
    cursor: pointer;
    border-bottom: 1px dashed #f0f0f0;
    transition: background 0.2s;
    &:last-child { border-bottom: none; }
    &:hover { background: $primary-red-light; }
    .news-icon {
      flex-shrink: 0;
      width: 14px;
      height: 14px;
      margin-right: 8px;
    }
    .news-text {
      flex: 1;
      font-size: 14px;
      color: #333;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-right: 8px;
    }
    .news-date {
      flex-shrink: 0;
      font-size: 12px;
      color: #999;
    }
  }
  .news-empty {
    padding: 40px 16px;
    text-align: center;
    color: #ccc;
    font-size: 14px;
  }
}

/* ========== 通用 section header ========== */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 0;

  .section-title {
    font-size: 18px;
    font-weight: bold;
    color: #333;
    position: relative;
    padding-left: 16px;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 4px;
      height: 18px;
      background: $primary-red;
      border-radius: 2px;
    }
  }

  .section-more {
    font-size: 13px;
    color: #999;
    cursor: pointer;
    &:hover { color: $primary-red; }
  }
}

/* ========== 容器 ========== */
.container {
  width: 80%;
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 20px 24px;
}

/* ========== 政策法规 + 信息公开（两栏） ========== */
.info-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.info-panel {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: $primary-red;

  .panel-title {
    font-size: 15px;
    font-weight: 600;
    color: #fff;
  }

  .panel-more {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.8);
    cursor: pointer;
    &:hover { color: #fff; }
  }
}

.panel-list {
  list-style: none;
  margin: 0;
  padding: 4px 0;
}

.panel-item {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  border-bottom: 1px dashed #f0f0f0;
  transition: background 0.2s;

  &:last-child { border-bottom: none; }
  &:hover { background: $primary-red-light; }

  .item-dot {
    flex-shrink: 0;
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: $primary-red;
    margin-right: 8px;
  }
  .item-text {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-right: 12px;
  }
  .item-date {
    flex-shrink: 0;
    color: #999;
    font-size: 12px;
  }
}

.panel-empty {
  padding: 20px 16px;
  text-align: center;
  color: #ccc;
  font-size: 13px;
}

@media (max-width: 768px) {
  .info-section {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}

/* ========== 推荐课程 ========== */
.course-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.course-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  border: 1px solid #f0f0f0;
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 6px 20px rgba(196, 30, 58, 0.12);
    border-color: $primary-red;
  }
  .course-cover {
    position: relative;
    width: 100%;
    height: 150px;
    background: #f5f7fa;
    overflow: hidden;
    img { width: 100%; height: 100%; object-fit: cover; }
    .progress-tag {
      position: absolute;
      top: 8px; left: 8px;
      background: rgba(196, 30, 58, 0.9);
      color: #fff;
      font-size: 12px;
      padding: 3px 8px;
      border-radius: 3px;
    }
    .locked-tag {
      position: absolute;
      top: 8px; left: 8px;
      background: rgba(245, 108, 108, 0.92);
      color: #fff;
      font-size: 12px;
      padding: 3px 8px;
      border-radius: 3px;
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
    .cover-mask {
      position: absolute;
      inset: 0;
      background: rgba(0, 0, 0, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.2s;
    }
  }
  &:hover .cover-mask { opacity: 1; }
  .course-info {
    padding: 12px 14px 14px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    flex: 1;
    .course-name {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
      line-height: 1.4;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      cursor: pointer;
      &:hover { color: $primary-red; }
    }
    .course-progress .progress-text { font-size: 12px; color: #909399; margin-top: 2px; }
    .course-stats {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 4px;
      font-size: 12px;
      color: #909399;
      .stat-divider { margin: 0 2px; color: #c0c4cc; }
    }
    .course-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: auto;
      padding-top: 4px;
      .course-price {
        .price-value { font-size: 18px; font-weight: bold; color: #ee0a24; }
        .price-free { font-size: 15px; font-weight: 600; color: #07c160; }
      }
    }
  }
}

/* ========== 测评服务平台 ========== */
.evaluate-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.evaluate-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.evaluate-item {
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  background: #fff;
  border: 1px solid #f0f0f0;
  transition: all 0.3s;
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(196, 30, 58, 0.1);
    border-color: $primary-red;
  }
}

.evaluate-image-wrapper {
  position: relative;
  width: 100%;
  height: 180px;
  overflow: hidden;
  img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s; }
}

.evaluate-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-top: 8px;
  padding: 0 12px 12px;
  word-break: break-word;
  max-height: 80px;
  overflow: hidden;
}
.evaluate-desc ::v-deep p { margin: 4px 0; }
.evaluate-desc ::v-deep img { max-width: 100%; border-radius: 4px; height: auto; display: block; margin: 0 auto; }
.evaluate-desc ::v-deep a { color: $primary-red; }

.evaluate-text-content {
  width: 100%;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.evaluate-text-content .evaluate-title { font-size: 16px; font-weight: 500; color: #333; line-height: 1.5; }
.evaluate-text-content .evaluate-desc { margin-top: 0 !important; padding: 0 !important; }

/* ========== 相关链接 ========== */
.related-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.related-item {
  height: 65px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 6px 20px rgba(196, 30, 58, 0.1);
    border-color: $primary-red;
  }
}

.related-img {
  width: 100%;
  height: 65px;
  object-fit: cover;
  background: #fafafa;
}

.related-placeholder {
  width: 100%;
  height: 65px;
  background: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ========== 政府网站链接 ========== */
.gov-links-section {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.gov-links-header {
  background: $primary-red;
  padding: 10px 20px;
  .gov-links-title {
    font-size: 15px;
    font-weight: 600;
    color: #fff;
  }
}

.gov-links-body {
  display: flex;
  justify-content: center;
  flex-wrap: nowrap;
  gap: 20px;
  padding: 16px 20px;
  overflow-x: auto;
  white-space: nowrap;
}

.gov-select {
  width: 220px;
  min-width: 220px;
  height: 30px;
  padding: 0 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 13px;
  color: #333;
  background: #fff;
  cursor: pointer;
  outline: none;
  flex-shrink: 0;
  &:hover { border-color: $primary-red; }
  &:focus { border-color: $primary-red; box-shadow: 0 0 0 2px rgba(196, 30, 58, 0.1); }
}

/* ========== 分割线 + 底部链接 ========== */
.section-divider {
  height: 3px;
  background: $primary-red;
  margin: 0;
}

.footer-links {
  text-align: center;
  padding: 18px 0 14px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 14px;
}
.footer-link {
  color: $primary-red;
  cursor: pointer;
  transition: color 0.2s, opacity 0.2s;
  user-select: none;
  &:hover { opacity: 0.75; text-decoration: underline; }
}
.footer-link-sep { color: #dcdfe6; user-select: none; }

.footer {
  text-align: center;
  color: #909399;
  font-size: 12px;
  padding: 24px 0 32px;
  background: #fff;
}

.footer-copyright { margin-bottom: 8px; }

.footer-beian {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  a {
    color: #909399;
    text-decoration: none;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    &:hover { color: $primary-red; }
  }
  .beian-icon { width: 14px; height: 14px; vertical-align: middle; }
}

/* ========== 响应式 ========== */
@media (min-width: 1600px) {
  .course-grid { grid-template-columns: repeat(5, 1fr); }
}

@media (max-width: 1100px) {
  .course-grid { grid-template-columns: repeat(3, 1fr); }
  .banner-left .banner-swipe, .banner-left .banner-img, .banner-left .banner-placeholder { height: 280px; }
}

@media (max-width: 800px) {
  .banner-wrapper { flex-direction: column; }
  .banner-left { flex: 1; }
  .banner-left .banner-swipe, .banner-left .banner-img, .banner-left .banner-placeholder { height: 160px; }
  .banner-right { flex: 1; }
  .course-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .page-body { padding-top: var(--header-height, 96px); }
  .container, .banner-section, .homepage-banner-wrap { width: 100%; padding: 0 10px; box-sizing: border-box; }
  .homepage-banner-title { font-size: 14px; letter-spacing: 0; }
  .banner-left .banner-swipe, .banner-left .banner-img, .banner-left .banner-placeholder { height: 140px; }
  .homepage-banner-wrap { width: 100%; margin: 0 0 12px; }
  .homepage-banner-img { width: 100%; height: auto; max-height: 120px; object-fit: cover; }
  .section-header .section-title { font-size: 16px; }
  .evaluate-row { grid-template-columns: 1fr; gap: 12px; }
  .evaluate-section { padding: 12px; }
  .evaluate-image-wrapper { height: 120px; }
  .related-grid { grid-template-columns: repeat(3, 1fr); gap: 8px; }
  .related-section { padding: 12px; }
  .related-item, .related-img, .related-placeholder { height: 50px; }
  .gov-links-body { gap: 8px; padding: 12px; }
  .gov-select { width: 160px; min-width: 160px; font-size: 12px; }
  .course-grid { grid-template-columns: repeat(2, 1fr); gap: 8px; }
  .course-section { padding: 12px; }
  .course-card {
    .course-cover { height: 90px; }
    .course-info {
      padding: 8px 10px 10px;
      gap: 4px;
      .course-name { font-size: 13px; }
      .course-footer .course-price .price-value { font-size: 15px; }
    }
  }
  .footer-links { gap: 8px; font-size: 13px; padding: 12px 0 10px; }
  .footer { padding: 16px 0 20px; }
}

@media (max-width: 540px) {
  .course-grid { grid-template-columns: 1fr; }
}
</style>

<style>
/* 政策法规/信息公开详情弹窗: 让富文本HTML正确渲染 */
.section-detail-dialog .van-dialog__message {
  max-height: 60vh;
  overflow-y: auto;
  text-align: left;
  line-height: 1.6;
}
.section-detail-dialog .van-dialog__message p { margin: 8px 0; }
.section-detail-dialog .van-dialog__message img { max-width: 100%; height: auto; border-radius: 4px; }
.section-detail-dialog .van-dialog__message a { color: #c41e3a; }
.section-detail-dialog .van-dialog__message table { width: 100%; border-collapse: collapse; }
.section-detail-dialog .van-dialog__message td,
.section-detail-dialog .van-dialog__message th { border: 1px solid #ddd; padding: 4px 8px; }
.section-detail-dialog .van-dialog__message h1,
.section-detail-dialog .van-dialog__message h2,
.section-detail-dialog .van-dialog__message h3 { margin: 12px 0 8px; }
</style>
