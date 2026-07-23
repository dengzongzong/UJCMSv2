import Vue from 'vue'
import VueRouter from 'vue-router'
import Layout from '@/layout/Index.vue'
import store from '@/store'
import { publicCertificateTypes } from '@/api/certificateType'

Vue.use(VueRouter)

// 抑制 Vue Router 的 NavigationDuplicated / redirected 错误，避免控制台报错
// 必须在创建 router 实例之前重写原型方法
const originalPush = VueRouter.prototype.push
const originalReplace = VueRouter.prototype.replace
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => err)
}
VueRouter.prototype.replace = function replace(location) {
  return originalReplace.call(this, location).catch(err => err)
}

export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login/Index.vue'),
    hidden: true
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: '仪表盘', icon: 'el-icon-s-home', affix: true }
      }
    ]
  },
  {
    path: '/admin',
    component: Layout,
    meta: { title: '子管理员管理', icon: 'el-icon-user-solid' },
    children: [
      {
        path: 'list',
        name: 'AdminList',
        component: () => import('@/views/admin/List.vue'),
        meta: { title: '子管理员管理', icon: 'el-icon-user-solid' }
      }
    ]
  },
  {
    path: '/student',
    component: Layout,
    meta: { title: '学生管理', icon: 'el-icon-s-custom' },
    children: [
      {
        path: 'list',
        name: 'StudentList',
        component: () => import('@/views/student/List.vue'),
        meta: { title: '学生管理', icon: 'el-icon-s-custom' }
      },
      {
        path: 'detail/:id',
        name: 'StudentDetail',
        component: () => import('@/views/student/Detail.vue'),
        hidden: true,
        meta: { title: '学生详情', activeMenu: '/student/list' }
      }
    ]
  },
  {
    path: '/video',
    component: Layout,
    meta: { title: '视频管理', icon: 'el-icon-video-camera' },
    children: [
      {
        path: 'list',
        name: 'VideoList',
        component: () => import('@/views/video/List.vue'),
        meta: { title: '视频管理', icon: 'el-icon-video-camera' }
      },
      {
        path: 'edit/:id?',
        name: 'VideoEdit',
        component: () => import('@/views/video/Edit.vue'),
        hidden: true,
        meta: { title: '视频编辑', activeMenu: '/video/list' }
      }
    ]
  },
  {
    path: '/course',
    component: Layout,
    meta: { title: '课程管理', icon: 'el-icon-reading' },
    children: [
      {
        path: 'list',
        name: 'CourseList',
        component: () => import('@/views/course/List.vue'),
        meta: { title: '课程管理', icon: 'el-icon-reading' }
      },
      {
        path: 'edit/:id?',
        name: 'CourseEdit',
        component: () => import('@/views/course/Edit.vue'),
        hidden: true,
        meta: { title: '课程编辑', activeMenu: '/course/list' }
      },
      {
        path: 'record',
        name: 'CourseRecord',
        component: () => import('@/views/course/StudyRecord.vue'),
        meta: { title: '课程学习记录', icon: 'el-icon-time' }
      }
    ]
  },
  {
    path: '/question',
    component: Layout,
    meta: { title: '题库管理', icon: 'el-icon-edit' },
    children: [
      {
        path: 'list',
        name: 'QuestionList',
        component: () => import('@/views/question/List.vue'),
        meta: { title: '题库管理', icon: 'el-icon-edit' }
      },
      {
        path: 'edit/:id?',
        name: 'QuestionEdit',
        component: () => import('@/views/question/Edit.vue'),
        hidden: true,
        meta: { title: '题目编辑', activeMenu: '/question/list' }
      }
    ]
  },
  {
    path: '/paper',
    component: Layout,
    meta: { title: '试卷管理', icon: 'el-icon-document' },
    children: [
      {
        path: 'list',
        name: 'PaperList',
        component: () => import('@/views/paper/List.vue'),
        meta: { title: '试卷管理', icon: 'el-icon-document' }
      },
      {
        path: 'edit',
        name: 'PaperEdit',
        component: () => import('@/views/paper/Edit.vue'),
        hidden: true,
        meta: { title: '编辑试卷', activeMenu: '/paper/list' }
      },
      {
        path: 'edit/:id',
        name: 'PaperEditId',
        component: () => import('@/views/paper/Edit.vue'),
        hidden: true,
        meta: { title: '编辑试卷', activeMenu: '/paper/list' }
      }
    ]
  },
  {
    path: '/exam',
    component: Layout,
    meta: { title: '考试管理', icon: 'el-icon-s-platform' },
    children: [
      {
        path: 'list',
        name: 'ExamList',
        component: () => import('@/views/exam/List.vue'),
        meta: { title: '考试管理', icon: 'el-icon-document' }
      },
      {
        path: 'edit/:id?',
        name: 'ExamEdit',
        component: () => import('@/views/exam/Edit.vue'),
        hidden: true,
        meta: { title: '考试编辑', activeMenu: '/exam/list' }
      },
      {
        path: 'record',
        name: 'ExamRecord',
        component: () => import('@/views/exam/Record.vue'),
        meta: { title: '考试记录', icon: 'el-icon-tickets' }
      }
    ]
  },
  {
    path: '/certificate',
    component: Layout,
    redirect: '/certificate/list',
    meta: { title: '证书管理', icon: 'el-icon-trophy' },
    children: [
      {
        path: 'list',
        name: 'CertificateListAll',
        component: () => import('@/views/certificate/List.vue'),
        meta: { title: '全部证书', icon: 'el-icon-trophy' }
      },
      {
        path: 'cooperation-apply',
        name: 'CooperationApply',
        component: () => import('@/views/cooperationApply/List.vue'),
        meta: { title: '授权培育基地内容', icon: 'el-icon-s-order' }
      },
      {
        path: 'edit/:id?',
        name: 'CertificateEdit',
        component: () => import('@/views/certificate/Edit.vue'),
        meta: { title: '录入证书用户', activeMenu: '/certificate/list' },
        hidden: true
      },
      {
        path: 'detail/:id',
        name: 'CertificateDetail',
        component: () => import('@/views/certificate/Detail.vue'),
        meta: { title: '证书详情', activeMenu: '/certificate/list' },
        hidden: true
      },
      {
        path: 'issue',
        redirect: '/certificate/list',
        meta: { title: '模板绑定', icon: 'el-icon-medal' },
        hidden: true
      },
      {
        path: 'field',
        name: 'CertificateField',
        component: () => import('@/views/certificate/FieldList.vue'),
        meta: { title: '证书字段' }
      },
      {
        path: 'number-config',
        name: 'CertificateNumberConfig',
        component: () => import('@/views/certificate/NumberConfig.vue'),
        meta: { title: '编号配置', icon: 'el-icon-setting' }
      },
      {
        path: 'url-config',
        name: 'CertificateUrlConfig',
        component: () => import('@/views/certificate/UrlConfig.vue'),
        meta: { title: 'URL配置', icon: 'el-icon-link' }
      },
      {
        path: 'template',
        name: 'CertificateTemplateList',
        component: () => import('@/views/certificateTemplate/List.vue'),
        meta: { title: '证书模板' }
      },
      {
        path: 'template/edit/:id?',
        name: 'CertificateTemplateEdit',
        component: () => import('@/views/certificateTemplate/Edit.vue'),
        meta: { title: '编辑证书模板', activeMenu: '/certificate/template' },
        hidden: true
      }
    ]
  },
  {
    path: '/setting',
    component: Layout,
    redirect: '/setting/profession',
    meta: { title: '系统设置', icon: 'el-icon-s-tools' },
    children: [
      {
        path: 'profession',
        name: 'SettingProfession',
        component: () => import('@/views/setting/Profession.vue'),
        meta: { title: '专业设置', icon: 'el-icon-s-management' }
      },
      {
        path: 'banner',
        name: 'SettingBanner',
        component: () => import('@/views/banner/List.vue'),
        meta: { title: '轮播图管理', icon: 'el-icon-picture' }
      },
      {
        path: 'about',
        name: 'SettingAbout',
        component: () => import('@/views/setting/About.vue'),
        meta: { title: '关于我们', icon: 'el-icon-info' }
      },
      {
        path: 'video-category',
        name: 'SettingVideoCategory',
        component: () => import('@/views/setting/VideoCategory.vue'),
        meta: { title: '课程分类', icon: 'el-icon-menu' }
      },
      {
        path: 'question-category',
        name: 'SettingQuestionCategory',
        component: () => import('@/views/setting/QuestionCategory.vue'),
        meta: { title: '题目分类', icon: 'el-icon-collection-tag' }
      },
      {
        path: 'news',
        name: 'NewsList',
        component: () => import('@/views/news/List.vue'),
        meta: { title: '新闻管理', icon: 'el-icon-document-copy' }
      },
      {
        path: 'course-three-image',
        name: 'SettingCourseThreeImage',
        component: () => import('@/views/courseThreeImage/List.vue'),
        meta: { title: '服务平台', icon: 'el-icon-picture-outline-round' }
      },
      {
        path: 'friendly-link',
        name: 'SettingFriendlyLink',
        component: () => import('@/views/friendlyLink/List.vue'),
        meta: { title: '友情链接', icon: 'el-icon-link' }
      },
      {
        path: 'cooperation',
        name: 'SettingCooperation',
        component: () => import('@/views/setting/Cooperation.vue'),
        meta: { title: '合作咨询', icon: 'el-icon-phone-outline' }
      },
      {
        path: 'declaration',
        name: 'SettingDeclaration',
        component: () => import('@/views/setting/Declaration.vue'),
        meta: { title: '网站声明', icon: 'el-icon-document' }
      },
      {
        path: 'complaint',
        name: 'SettingComplaint',
        component: () => import('@/views/setting/Complaint.vue'),
        meta: { title: '投诉建议', icon: 'el-icon-warning-outline' }
      },
      {
        path: 'announcement',
        name: 'AnnouncementList',
        component: () => import('@/views/announcement/List.vue'),
        meta: { title: '系统公告', icon: 'el-icon-bell' }
      },
      {
        path: 'file-clean',
        name: 'FileClean',
        component: () => import('@/views/setting/FileClean.vue'),
        meta: { title: '文件清理', icon: 'el-icon-delete' }
      },
      {
        path: 'homepage-section',
        name: 'HomepageSectionList',
        component: () => import('@/views/homepageSection/List.vue'),
        meta: { title: '首页内容板块', icon: 'el-icon-files' }
      },
      {
        path: 'banner-image',
        name: 'BannerImageList',
        component: () => import('@/views/bannerImage/List.vue'),
        meta: { title: '首页横幅图片', icon: 'el-icon-picture' }
      },
      {
        path: 'certificate-type',
        name: 'CertificateTypeList',
        component: () => import('@/views/certificateType/List.vue'),
        meta: { title: '证书类型', icon: 'el-icon-document' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: { path: '/dashboard' },
    hidden: true
  }
]

const createRouter = () =>
  new VueRouter({
    scrollBehavior: () => ({ y: 0 }),
    routes: constantRoutes
  })

const router = createRouter()

const whiteList = ['/login']

// 动态添加证书类型路由
let certTypeRoutesAdded = false
async function addDynamicCertTypeRoutes() {
  if (certTypeRoutesAdded) return
  try {
    const res = await publicCertificateTypes()
    const types = (res.data || res || [])
    types.forEach((t, idx) => {
      const name = t.name || t
      const routePath = `list/cert-type-${idx + 1}`
      router.addRoute('Certificate', {
        path: routePath,
        name: `CertificateListDyn${idx + 1}`,
        component: () => import('@/views/certificate/List.vue'),
        meta: { title: `${name}内容`, icon: 'el-icon-document', certType: name }
      })
    })
    // 通知菜单更新
    store.commit('app/SET_CERT_TYPES', types)
    certTypeRoutesAdded = true
  } catch (e) {
    // 静默失败,不影响登录
  }
}

router.beforeEach(async (to, from, next) => {
  const hasToken = store.getters.token
  document.title = (to.meta && to.meta.title ? to.meta.title + ' - ' : '') + '人力资源专业技能人才评价网管理后台'
  if (hasToken) {
    if (to.path === '/login') {
      next({ path: '/' })
    } else {
      if (!store.getters.adminInfo) {
        try {
          await store.dispatch('admin/fetchAdminInfo')
        } catch (e) {
          await store.dispatch('admin/resetToken')
          next({ path: '/login', query: { redirect: to.fullPath } })
          return
        }
      }
      // 动态添加证书类型路由
      await addDynamicCertTypeRoutes()
      next()
    }
  } else {
    if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }
})

export default router
