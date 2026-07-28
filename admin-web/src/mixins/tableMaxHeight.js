/**
 * 表格滚动 mixin (wrapper + fixed-layout 方案)
 *
 * 用外层 wrapper 处理滚动, 同时用 table-layout: fixed + JS 设置列宽
 * 确保表格宽度 = 列宽之和, 超出容器时水平滚动条出现。
 *
 * 用法:
 *   import tableMaxHeight from '@/mixins/tableMaxHeight'
 *   export default { mixins: [tableMaxHeight], ... }
 *   <el-table :fit="false" ...>   ← 不需要 max-height
 */
export default {
  data() {
    return {
      tableMaxHeight: 500
    }
  },
  mounted() {
    this._calcHeight()
    this._setupWrapper()
    window.addEventListener('resize', this._onResize)
    // 多次延迟触发, 确保数据加载后也能修正
    setTimeout(() => this._fixTableWidths(), 150)
    setTimeout(() => this._fixTableWidths(), 400)
    setTimeout(() => this._fixTableWidths(), 800)
  },
  activated() {
    this._calcHeight()
    this._setupWrapper()
    setTimeout(() => this._fixTableWidths(), 150)
  },
  updated() {
    if (this._t) clearTimeout(this._t)
    this._t = setTimeout(() => this._fixTableWidths(), 100)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this._onResize)
    if (this._t) clearTimeout(this._t)
  },
  methods: {
    _onResize() {
      this._calcHeight()
    },

    _calcHeight() {
      this.tableMaxHeight = Math.max(200, window.innerHeight - 300)
    },

    _setupWrapper() {
      this.$nextTick(() => {
        const tableEl = this.$el && this.$el.querySelector('.el-table')
        if (!tableEl) return

        let wrapper = tableEl.parentElement
        if (wrapper && wrapper.classList && wrapper.classList.contains('table-scroll-wrapper')) {
          wrapper.style.height = this.tableMaxHeight + 'px'
          return
        }

        wrapper = document.createElement('div')
        wrapper.className = 'table-scroll-wrapper'
        wrapper.style.height = this.tableMaxHeight + 'px'
        wrapper.style.overflow = 'auto'
        wrapper.style.position = 'relative'

        tableEl.parentNode.insertBefore(wrapper, tableEl)
        wrapper.appendChild(tableEl)
      })
    },

    /**
     * 核心: 设置 table-layout: fixed + 列宽 + 表格宽度
     * 确保表格宽度 = 列宽之和, 超出容器时水平滚动出现
     */
    _fixTableWidths() {
      this.$nextTick(() => {
        const tableEl = this.$el && this.$el.querySelector('.el-table')
        if (!tableEl) return

        const bodyTable = tableEl.querySelector('.el-table__body-wrapper .el-table__body')
        const headerTable = tableEl.querySelector('.el-table__header-wrapper .el-table__header')
        const wrapper = tableEl.querySelector('.el-table__body-wrapper')
        if (!bodyTable || !wrapper) return

        // ---- Step 1: table-layout: fixed (列宽严格遵循设定值) ----
        bodyTable.style.tableLayout = 'fixed'
        if (headerTable) headerTable.style.tableLayout = 'fixed'

        // ---- Step 2: 从 Element UI store 读取列宽, 设到每个 td/th ----
        const columns = (tableEl.__vue__ && tableEl.__vue__.store && tableEl.__vue__.store.states.columns) || []
        const containerWidth = wrapper.clientWidth

        // 设 body 单元格宽度
        const bodyRows = bodyTable.querySelectorAll('tbody > tr')
        bodyRows.forEach(row => {
          const cells = row.querySelectorAll('td')
          let colIdx = 0
          cells.forEach((cell, i) => {
            if (colIdx >= columns.length) return
            const col = columns[colIdx]
            const w = parseInt(col.width || col.minWidth || 0, 10)
            if (w > 0) cell.style.width = w + 'px'
            colIdx++
          })
        })

        // 设 header 单元格宽度(保持对齐)
        if (headerTable) {
          const headerRows = headerTable.querySelectorAll('thead > tr')
          headerRows.forEach(row => {
            const cells = row.querySelectorAll('th')
            let colIdx = 0
            cells.forEach((cell, i) => {
              if (colIdx >= columns.length) return
              const col = columns[colIdx]
              const w = parseInt(col.width || col.minWidth || 0, 10)
              if (w > 0) cell.style.width = w + 'px'
              colIdx++
            })
          })
        }

        // ---- Step 3: 计算列宽之和, 设表格宽度 ----
        let totalWidth = 0
        columns.forEach(col => {
          const w = parseInt(col.width || col.minWidth || 0, 10)
          if (w > 0) totalWidth += w
        })

        if (totalWidth > 0 && totalWidth > containerWidth) {
          bodyTable.style.width = totalWidth + 'px'
          if (headerTable) headerTable.style.width = totalWidth + 'px'
        }
      })
    }
  }
}
