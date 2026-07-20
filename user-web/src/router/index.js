import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '@/store'

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

const routes = [
  {
      path: '/',
      name: 'Home',
      component: () => import('@/views/home/Index.vue'),
      // 公开:未登录也能看
      meta: { requiresAuth: false, requiresSubject: false, title: '首页' }
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/Login.vue'),
      meta: { requiresAuth: false, title: '登录' }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/Register.vue'),
      meta: { requiresAuth: false, title: '注册' }
    },
    {
      path: '/forgot-password',
      redirect: '/home'
    },
    {
      path: '/choose-subject',
      name: 'ChooseSubject',
      component: () => import('@/views/auth/ChooseSubject.vue'),
      meta: { requiresAuth: true, requiresSubject: false, title: '选择专业' }
    },
    {
      path: '/home',
      redirect: '/'  // 保留 /home 作为别名,兼容已有代码中的跳转
    },
    {
      path: '/about',
      name: 'About',
      component: () => import('@/views/profile/About.vue'),
      // 公开:未登录也能看"关于我们"
      meta: { requiresAuth: false, requiresSubject: false, title: '关于我们' }
    },
    {
      path: '/course/detail/:id',
      name: 'CourseDetail',
      component: () => import('@/views/course/Detail.vue'),
      // 公开:未登录也能浏览课程详情
      meta: { requiresAuth: false, requiresSubject: false, title: '课程详情' }
    },
    {
      path: '/course/my',
      name: 'CourseCenter',
      component: () => import('@/views/course/MyCourses.vue'),
      // 公开:未登录可浏览所有课程,点击具体课程时再校验是否已开通/已登录
      meta: { requiresAuth: false, requiresSubject: false, title: '课程中心' }
    },
    {
      path: '/course/my-opened',
      name: 'MyOpenedCourses',
      component: () => import('@/views/course/MyOpenedCourses.vue'),
      meta: { requiresAuth: true, requiresSubject: false, title: '我的课程' }
    },
    {
      path: '/exam',
      name: 'ExamCenter',
      component: () => import('@/views/exam/List.vue'),
      // 公开:未登录可浏览所有考试,点击具体考试时再校验
      meta: { requiresAuth: false, requiresSubject: false, title: '考试中心' }
    },
    {
      path: '/exam/my-opened',
      name: 'MyExams',
      component: () => import('@/views/exam/MyExams.vue'),
      meta: { requiresAuth: true, requiresSubject: false, title: '我的考试' }
    },
    {
      path: '/exam/intro/:id',
      name: 'ExamIntro',
      component: () => import('@/views/exam/Intro.vue'),
      meta: { requiresAuth: true, requiresSubject: true, title: '考试介绍' }
    },
    {
      path: '/exam/take/:id',
      name: 'ExamTake',
      component: () => import('@/views/exam/Exam.vue'),
      meta: { requiresAuth: true, requiresSubject: true, title: '考试' }
    },
    {
      path: '/exam/result/:id',
      name: 'ExamResult',
      component: () => import('@/views/exam/Result.vue'),
      meta: { requiresAuth: true, requiresSubject: true, title: '考试结果' }
    },
    {
      path: '/exam/records',
      name: 'ExamRecords',
      component: () => import('@/views/exam/Records.vue'),
      meta: { requiresAuth: true, requiresSubject: true, title: '考试记录' }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/profile/Index.vue'),
      // 需登录:点击"个人中心"会跳登录
      meta: { requiresAuth: true, requiresSubject: true, title: '个人中心' }
    },
    {
      path: '/profile/wrong-questions',
      name: 'WrongQuestions',
      component: () => import('@/views/profile/WrongQuestions.vue'),
      meta: { requiresAuth: true, requiresSubject: true, title: '错题本' }
    },
    {
      path: '/profile/about',
      redirect: '/about'
    },
    {
      path: '/news/detail/:id',
      name: 'NewsDetail',
      component: () => import('@/views/news/Detail.vue'),
      // 公开:未登录可看新闻详情
      meta: { requiresAuth: false, requiresSubject: false, title: '新闻详情' }
    },
    {
      path: '/news/list',
      name: 'NewsList',
      component: () => import('@/views/news/List.vue'),
      meta: { requiresAuth: false, requiresSubject: false, title: '新闻动态' }
    },
    {
      path: '/news/center',
      name: 'NewsCenter',
      component: () => import('@/views/news/Center.vue'),
      meta: { requiresAuth: false, requiresSubject: false, title: '中心动态' }
    },
    {
      path: '/news/announcements',
      name: 'AnnouncementList',
      component: () => import('@/views/news/Announcements.vue'),
      meta: { requiresAuth: false, requiresSubject: false, title: '系统公告' }
    },
    {
      path: '/certificate',
      name: 'CertificatePortal',
      component: () => import('@/views/certificate/Index.vue'),
      // 公开:未登录也能查证书
      meta: { requiresAuth: false, requiresSubject: false, title: '证书查询' }
    },
    {
      path: '/cooperation',
      name: 'Cooperation',
      component: () => import('@/views/cooperation/Index.vue'),
      meta: { requiresAuth: false, requiresSubject: false, title: '合作单位' }
    },
    {
      path: '*',
      redirect: '/'
    }
]

const router = new VueRouter({
  mode: 'history',
  routes,
  scrollBehavior(to, from, savedPosition) {
    return { x: 0, y: 0 }
  }
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title
  }

  const token = store.getters.token

  // 已登录访问登录/注册页，跳转首页（忘记密码入口已关闭）
  if ((to.name === 'Login' || to.name === 'Register') && token) {
    next('/home')
    return
  }

  // 需要登录但未登录 —— 排除已在登录页的情况，防止重定向死循环
  if (to.meta.requiresAuth && !token) {
    // 如果目标本身就是登录相关页面，直接放行
    if (to.name === 'Login' || to.name === 'Register') {
      next()
      return
    }
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  // 需要选择专业科目但未选择 —— 排除 ChooseSubject 自身，防止循环
  if (to.meta.requiresSubject && token && !store.getters.currentSubject && to.name !== 'ChooseSubject') {
    next({ name: 'ChooseSubject', query: to.fullPath !== '/' ? { redirect: to.fullPath } : {} })
    return
  }

  next()
})

export default router
