/**
 * 表格最大高度 mixin
 * 自动计算 el-table 的 max-height,使表格在可视区域内滚动
 * 同时确保水平+垂直滚动条都正常显示, 表头和数据列对齐
 *
 * 用法:
 *   import tableMaxHeight from '@/mixins/tableMaxHeight'
 *   export default { mixins: [tableMaxHeight], ... }
 *   <el-table :max-height="tableMaxHeight" :fit="false" ...>
 *
 * 原理:
 *   Element UI 的 doLayout() 在 fit=false 时仍然会:
 *   1. 调用 updateColumnsWidth() → 给每个 td/th 设置列宽(来自 column.width/minWidth)
 *   2. 设置 table style.width = '100%' → 表格被压缩到容器宽度
 *   所以 scrollWidth === clientWidth, 水平滚动条不出现。
 *
 *   修复: 在 doLayout() 完成后, 从列定义 store 读取每列的 width/minWidth,
 *   求和得到自然宽度, 显式设置 body/header 表格的 width 为自然宽度(像素值),
 *   覆盖 100%, 使表格超出容器, 水平滚动条出现。
 *   表头和主体设同一个值, 保证列对齐。
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
    this.$nextTick(() => this._fixAllTables())
    setTimeout(() => this._fixAllTables(), 100)
    setTimeout(() => this._fixAllTables(), 300)
    setTimeout(() => this._fixAllTables(), 600)
  },
  activated() {
    this.$nextTick(() => this._fixAllTables())
    setTimeout(() => this._fixAllTables(), 100)
  },
  updated() {
    if (this._t) clearTimeout(this._t)
    this._t = setTimeout(() => this._fixAllTables(), 80)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.calcTableMaxHeight)
    if (this._t) clearTimeout(this._t)
  },
  methods: {
    calcTableMaxHeight() {
      this.tableMaxHeight = Math.max(200, window.innerHeight - 300)
      this.$nextTick(() => this._fixAllTables())
    },

    _fixAllTables() {
      const visit = (vm) => {
        if (!vm || !vm.$children) return
        vm.$children.forEach(child => {
          if (child.$options && child.$options.name === 'ElTable') {
            this._fixOneTable(child)
          }
          visit(child)
        })
      }
      visit(this)
    },

    _fixOneTable(tableVm) {
      try {
        const el = tableVm.$el
        if (!el) return

        const bodyTable = el.querySelector('.el-table__body-wrapper .el-table__body')
        const headerTable = el.querySelector('.el-table__header-wrapper .el-table__header')
        const wrapper = el.querySelector('.el-table__body-wrapper')
        if (!bodyTable || !wrapper) return

        // ---- Step 1: 确保 table-layout: fixed(让列宽严格遵循设定值) ----
        bodyTable.style.tableLayout = 'fixed'
        if (headerTable) headerTable.style.tableLayout = 'fixed'

        // ---- Step 2: 从 Element UI 列定义 store 读取每列宽度 ----
        const allColumns = (tableVm.store && tableVm.store.states && tableVm.store.states.columns) || []
        // 过滤掉隐藏列
        const columns = allColumns.filter(c => !c.filtered)

        let totalNaturalWidth = 0
        columns.forEach(col => {
          // column.width: 有显式 width 或 Element UI 默认值(如 selection=48)
          // column.minWidth: 有 min-width 时
          const w = parseInt(col.width || col.minWidth || 0, 10)
          if (w > 0) totalNaturalWidth += w
        })

        if (totalNaturalWidth <= 0) return

        const containerWidth = wrapper.clientWidth
        if (totalNaturalWidth > containerWidth) {
          // ---- Step 3: 设同一个显式宽度, 覆盖 100%, 保证对齐 + 水平滚动 ----
          const w = totalNaturalWidth + 'px'
          bodyTable.style.width = w
          if (headerTable) headerTable.style.width = w
        }

        // ---- Step 4: doLayout 同步固定列和滚动状态 ----
        if (typeof tableVm.doLayout === 'function') {
          tableVm.doLayout()
        }

        // ---- Step 5: doLayout 可能重置 width, 再设一次 ----
        if (totalNaturalWidth > containerWidth) {
          const w = totalNaturalWidth + 'px'
          bodyTable.style.width = w
          if (headerTable) headerTable.style.width = w
        }
      } catch (e) {
        // ignore
      }
    }
  }
}
