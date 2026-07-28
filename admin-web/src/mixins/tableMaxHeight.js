/**
 * 表格最大高度 mixin
 * 自动计算 el-table 的 max-height,使表格在可视区域内滚动
 * 同时自动调用 doLayout() 确保 Element UI 正确检测水平+垂直滚动
 * 用法:
 *   import tableMaxHeight from '@/mixins/tableMaxHeight'
 *   export default { mixins: [tableMaxHeight], ... }
 *   <el-table :max-height="tableMaxHeight" ...>
 */
export default {
  data() {
    return {
      tableMaxHeight: 500
    }
  },
  mounted() {
    this.calcTableMaxHeight()
    window.addEventListener('resize', this.calcTableMaxHeight)
    // 强制表格重新计算布局,确保水平滚动条正确显示
    this.$nextTick(() => this._forceTableLayout())
    // 二次确保:200ms 后再触发一次(等待数据加载完成)
    setTimeout(() => this._forceTableLayout(), 200)
  },
  activated() {
    // keep-alive 重新激活时也需要重新计算
    this.$nextTick(() => this._forceTableLayout())
  },
  updated() {
    // 数据更新后重新计算表格布局(防抖,避免频繁调用)
    if (this._tableLayoutTimer) clearTimeout(this._tableLayoutTimer)
    this._tableLayoutTimer = setTimeout(() => this._forceTableLayout(), 50)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.calcTableMaxHeight)
    if (this._tableLayoutTimer) clearTimeout(this._tableLayoutTimer)
  },
  methods: {
    calcTableMaxHeight() {
      // 减去: header(56) + main padding(32) + app-container padding(40)
      //        + 筛选区/工具栏/分页等(~170) ≈ 300
      this.tableMaxHeight = Math.max(200, window.innerHeight - 300)
      this.$nextTick(() => this._forceTableLayout())
    },
    /**
     * 递归查找所有 ElTable 子组件并调用 doLayout()
     * 这会强制 Element UI 重新计算列宽和滚动状态(包括水平滚动检测)
     */
    _forceTableLayout() {
      const layout = (vm) => {
        if (!vm || !vm.$children) return
        vm.$children.forEach(child => {
          if (child.$options && child.$options.name === 'ElTable') {
            if (typeof child.doLayout === 'function') {
              child.doLayout()
            }
          }
          layout(child)
        })
      }
      layout(this)
    }
  }
}
