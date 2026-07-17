// Jest 全局 setup: 把 window.location/localStorage 全配齐
// 模拟浏览器全局对象

// localStorage 已有(由 jsdom 提供),这里做安全补丁
const _localStorage = window.localStorage

// 给 Vue Router 用的 history API
Object.defineProperty(window, 'localStorage', {
  value: _localStorage,
  writable: true
})

// matchMedia(Element UI 用到)
window.matchMedia = window.matchMedia || function() {
  return {
    matches: false,
    addListener: () => {},
    removeListener: () => {}
  }
}
