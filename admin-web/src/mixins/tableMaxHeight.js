/**
 * 表格滚动 mixin (简洁方案)
 *
 * 核心思路:
 * 1. 覆盖 el-table 根元素 inline width:100% → 列宽之和 px
 * 2. MutationObserver 强制 body-wrapper overflow:auto (对抗 Element UI JS)
 * 3. 同步 header/body 水平滚动
 * 4. updated 钩子确保数据变化后重新应用宽度
 */
export default {
  data() {
    return {
      tableMaxHeight: 500
    }
  },
  mounted() {
    this._calcHeight()
    this._initTableScroll()
    window.addEventListener('resize', this._calcHeight)
  },
  activated() {
    this._calcHeight()
    this.$nextTick(() => this._applyWidths())
  },
  updated() {
    // 数据变化后 Vue 会重新设置 style="width:100%", 需要再次覆盖
    if (this._ut) clearTimeout(this._ut)
    this._ut = setTimeout(() => this._applyWidths(), 100)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this._calcHeight)
    if (this._mo) this._mo.disconnect()
    if (this._bw) this._bw.removeEventListener('scroll', this._onBS)
    if (this._ut) clearTimeout(this._ut)
  },
  methods: {
    _calcHeight() {
      this.tableMaxHeight = Math.max(200, window.innerHeight - 300)
    },

    _initTableScroll() {
      this.$nextTick(() => {
        const tableEl = this.$el && this.$el.querySelector('.el-table')
        if (!tableEl) return

        const bw = tableEl.querySelector('.el-table__body-wrapper')
        if (bw) {
          this._bw = bw
          bw.style.maxHeight = this.tableMaxHeight + 'px'
          bw.style.overflow = 'auto'
          bw.style.overflowX = 'auto'
          bw.style.overflowY = 'auto'

          // MutationObserver: 对抗 Element UI JS 对 overflow 的重写
          this._mo = new MutationObserver(() => {
            if (bw.style.overflowX !== 'auto') {
              bw.style.overflow = 'auto'
              bw.style.overflowX = 'auto'
              bw.style.overflowY = 'auto'
            }
          })
          this._mo.observe(bw, { attributes: true, attributeFilter: ['style'] })

          // header/body 水平滚动同步
          const hw = tableEl.querySelector('.el-table__header-wrapper')
          this._onBS = () => {
            if (hw) hw.scrollLeft = bw.scrollLeft
          }
          bw.addEventListener('scroll', this._onBS)
        }

        // 延迟应用宽度 (等数据加载)
        setTimeout(() => this._applyWidths(), 300)
        setTimeout(() => this._applyWidths(), 800)
        setTimeout(() => this._applyWidths(), 1500)
      })
    },

    _applyWidths() {
      const tableEl = this.$el && this.$el.querySelector('.el-table')
      if (!tableEl) return

      const vue = tableEl.__vue__
      const columns = (vue && vue.store && vue.store.states && vue.store.states.columns) || []
      if (columns.length === 0) return

      // 计算列宽之和
      let totalWidth = 0
      columns.forEach(col => {
        const w = parseInt(col.width || col.minWidth || 0, 10)
        if (w > 0) totalWidth += w
      })
      if (totalWidth <= 0) return

      // 关键: 覆盖 el-table 根元素的 inline width:100%
      tableEl.style.width = totalWidth + 'px'
      tableEl.style.minWidth = totalWidth + 'px'
      tableEl.style.maxWidth = 'none'

      // 设置 body table
      const bodyTable = tableEl.querySelector('.el-table__body')
      if (bodyTable) {
        bodyTable.style.width = totalWidth + 'px'
        bodyTable.style.minWidth = totalWidth + 'px'
        bodyTable.style.maxWidth = 'none'
        bodyTable.style.tableLayout = 'fixed'
      }

      // 设置 header table
      const headerTable = tableEl.querySelector('.el-table__header')
      if (headerTable) {
        headerTable.style.width = totalWidth + 'px'
        headerTable.style.minWidth = totalWidth + 'px'
        headerTable.style.maxWidth = 'none'
        headerTable.style.tableLayout = 'fixed'
      }

      // 设置每个单元格宽度
      if (bodyTable) {
        bodyTable.querySelectorAll('tr').forEach(row => {
          row.querySelectorAll('td').forEach((cell, i) => {
            if (i >= columns.length) return
            const w = parseInt(columns[i].width || columns[i].minWidth || 0, 10)
            if (w > 0) cell.style.width = w + 'px'
          })
        })
      }
      if (headerTable) {
        headerTable.querySelectorAll('tr').forEach(row => {
          row.querySelectorAll('th').forEach((cell, i) => {
            if (i >= columns.length) return
            const w = parseInt(columns[i].width || columns[i].minWidth || 0, 10)
            if (w > 0) cell.style.width = w + 'px'
          })
        })
      }

      // 确保 body wrapper 仍然是 auto
      const bw = tableEl.querySelector('.el-table__body-wrapper')
      if (bw) {
        bw.style.overflow = 'auto'
        bw.style.overflowX = 'auto'
        bw.style.overflowY = 'auto'
        bw.style.maxHeight = this.tableMaxHeight + 'px'
      }
    }
  }
}
