/**
 * 表格滚动 mixin (wrapper 方案)
 *
 * 彻底放弃 Element UI 的 max-height 内部滚动机制(它不断把 table width 重置为 100%,
 * 导致水平滚动条永远不出来), 改为在外层包一个 scroll wrapper, 由 wrapper 处理
 * 所有垂直+水平滚动。
 *
 * 用法:
 *   import tableMaxHeight from '@/mixins/tableMaxHeight'
 *   export default { mixins: [tableMaxHeight], ... }
 *   <el-table :fit="false" ...>   ← 不需要 max-height, 不需要 max-height!
 *
 * 原理:
 *   1. 在 el-table 外面包一个 div.table-scroll-wrapper
 *   2. wrapper 设置 height + overflow: auto → 成为滚动容器
 *   3. 表格在 wrapper 内以自然尺寸渲染(fit=false → 列不压缩)
 *   4. 表格超出 wrapper 时, wrapper 显示滚动条
 *   5. 禁用 Element UI 内部的 overflow(由 CSS 处理)
 */
export default {
  data() {
    return {
      tableMaxHeight: 500 // 保留兼容, 但不再用于 max-height prop
    }
  },
  mounted() {
    this._calcHeight()
    this._setupWrapper()
    window.addEventListener('resize', this._onResize)
  },
  activated() {
    this._calcHeight()
    this._setupWrapper()
  },
  beforeDestroy() {
    window.removeEventListener('resize', this._onResize)
  },
  methods: {
    _onResize() {
      this._calcHeight()
    },

    _calcHeight() {
      // wrapper 高度 = 视口高度 - 顶部空间
      // 顶部空间: header(56px) + layout-main padding(32px) + app-container padding(40px)
      //          + 筛选区/工具栏/分页等(~170px) ≈ 300px
      this.tableMaxHeight = Math.max(200, window.innerHeight - 300)
    },

    _setupWrapper() {
      this.$nextTick(() => {
        // 找到本组件的第一个 el-table
        const tableEl = this.$el && this.$el.querySelector('.el-table')
        if (!tableEl) return

        // 如果已经包过了, 只更新高度
        let wrapper = tableEl.parentElement
        if (wrapper && wrapper.classList && wrapper.classList.contains('table-scroll-wrapper')) {
          wrapper.style.height = this.tableMaxHeight + 'px'
          return
        }

        // 创建 wrapper 并包裹表格
        wrapper = document.createElement('div')
        wrapper.className = 'table-scroll-wrapper'
        wrapper.style.height = this.tableMaxHeight + 'px'
        wrapper.style.overflow = 'auto'
        wrapper.style.position = 'relative'

        // 在 el-table 的位置插入 wrapper
        tableEl.parentNode.insertBefore(wrapper, tableEl)
        wrapper.appendChild(tableEl)
      })
    }
  }
}
