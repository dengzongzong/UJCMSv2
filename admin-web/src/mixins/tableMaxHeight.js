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
 *   Element UI 默认 fit=true 用 table-layout:fixed 把列压缩到容器宽度,
 *   导致 scrollWidth===clientWidth, 水平滚动条永远不会出现。
 *   设置 fit=false 后, Element UI 不再自动给单元格设置宽度,
 *   所以 JS 手动将每列的 minWidth 设到对应的 td/th 上,
 *   确保列保持最小宽度, 表格自然撑开, 水平滚动条出现。
 *   最后将表头和主体表格设为同一个显式宽度, 保证列对齐。
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
    // 多次延迟触发,确保在数据加载和 Element UI 布局完成后修正表格宽度
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
      // 减去: header(56) + main padding(32) + app-container padding(40)
      //        + 筛选区/工具栏/分页等(~170) ≈ 300
      this.tableMaxHeight = Math.max(200, window.innerHeight - 300)
      this.$nextTick(() => this._fixAllTables())
    },

    /**
     * 遍历所有 ElTable 子组件,修正宽度和布局
     */
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

    /**
     * 修正单个 ElTable:
     * 1. 从 Element UI 列定义中读取 minWidth/width
     * 2. 手动设到每个 td/th 上(因为 fit=false 时 Element UI 不会设)
     * 3. 测量第一行总宽度
     * 4. 如果超出容器, 将 body 和 header 表格设为同一个显式宽度
     * 5. 调用 doLayout() 同步固定列
     */
    _fixOneTable(tableVm) {
      try {
        const el = tableVm.$el
        if (!el) return

        const bodyTable = el.querySelector('.el-table__body-wrapper .el-table__body')
        const headerTable = el.querySelector('.el-table__header-wrapper .el-table__header')
        const wrapper = el.querySelector('.el-table__body-wrapper')
        if (!bodyTable || !wrapper) return

        // ---- Step 1: 从列定义读取宽度, 手动设到单元格上 ----
        const columns = (tableVm.columns || tableVm.store.states.columns || [])
        const bodyRows = bodyTable.querySelectorAll('tr')
        const headerRows = headerTable ? headerTable.querySelectorAll('tr') : []

        columns.forEach((col, idx) => {
          // 取列的最小宽度或固定宽度
          const w = col.minWidth || col.width
          if (!w) return

          // 设到 body 的每个 td 上
          bodyRows.forEach(row => {
            const cell = row.querySelectorAll('td')[idx]
            if (cell) cell.style.width = w + 'px'
          })

          // 设到 header 的每个 th 上(保持表头和数据列对齐)
          headerRows.forEach(row => {
            const cell = row.querySelectorAll('th')[idx]
            if (cell) cell.style.width = w + 'px'
          })
        })

        // ---- Step 2: 测量第一行的总宽度 ----
        const firstRow = bodyTable.querySelector('tr')
        if (!firstRow) return
        const cells = firstRow.querySelectorAll('td')
        let totalWidth = 0
        cells.forEach(cell => {
          totalWidth += cell.getBoundingClientRect().width
        })

        const containerWidth = wrapper.clientWidth
        if (totalWidth > containerWidth + 1) {
          // 列宽之和超出容器 → 设同一个显式宽度, 保证表头和主体对齐
          const w = Math.ceil(totalWidth) + 'px'
          bodyTable.style.width = w
          if (headerTable) headerTable.style.width = w
        }

        // ---- Step 3: 调用 doLayout 同步固定列和滚动状态 ----
        if (typeof tableVm.doLayout === 'function') {
          tableVm.doLayout()
        }

        // doLayout 可能重置宽度, 再设一次
        if (totalWidth > containerWidth + 1) {
          const w = Math.ceil(totalWidth) + 'px'
          bodyTable.style.width = w
          if (headerTable) headerTable.style.width = w
        }
      } catch (e) {
        // ignore
      }
    }
  }
}
