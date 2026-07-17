import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import App from './App.vue'
import router from './router'
import store from './store'
import './styles/index.scss'

Vue.use(ElementUI, { size: 'small' })

Vue.config.productionTip = false

// ===== 全局错误兜底,避免后端异常时弹出 "Uncaught runtime errors" 遮罩 =====
// 1) 捕获未处理的 Promise rejection(如后端返回 500/业务错误后,组件未 catch 的场景)
//    axios 拦截器已用 Message 提示用户,这里只需 preventDefault 阻止 dev-server 遮罩
window.addEventListener('unhandledrejection', (event) => {
  // eslint-disable-next-line no-console
  console.error('[unhandledrejection]', event.reason && event.reason.message || event.reason)
  event.preventDefault()
})
// 2) 捕获 Vue 组件内未处理的同步错误(渲染/生命周期/事件回调)
Vue.config.errorHandler = (err, vm, info) => {
  // eslint-disable-next-line no-console
  console.error('[Vue errorHandler]', err && err.message, info)
}

new Vue({
  router,
  store,
  render: (h) => h(App)
}).$mount('#app')
