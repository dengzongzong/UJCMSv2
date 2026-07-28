/**
 * 表格最大高度 mixin
 * 自动计算 el-table 的 max-height,使表格在可视区域内滚动
 * 同时确保水平+垂直滚动条都正常显示
 *
 * 用法:
 *   import tableMaxHeight from '@/mixins/tableMaxHeight'
 *   export default { mixins: [tableMaxHeight], ... }
 *   <el-table :max-height="tableMaxHeight" :fit="false" ...>
 *
 * 原理:
 *   Element UI 默认 fit=true 用 table-layout:fixed 把列压缩到容器宽度,
 *   导致 scrollWidth===clientWidth, 水平滚动条永远不会出现。
 *   设置 fit=false 后,列保持原始宽度,再通过 JS 显式设置表格宽度为列宽之和,
 *   确保表格内容超出容器时水平滚动条正常显示。
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
     * 1. 读取每列实际渲染宽度,求和得到表格自然宽度
     * 2. 如果自然宽度 > 容器宽度,显式设置表格宽度使水平滚动出现
     * 3. 调用 doLayout() 让 Element UI 同步表头和固定列
     */
    _fixOneTable(tableVm) {
      try {
        const el = tableVm.$el
        if (!el) return

        const bodyTable = el.querySelector('.el-table__body-wrapper .el-table__body')
        const headerTable = el.querySelector('.el-table__header-wrapper .el-table__header')
        const wrapper = el.querySelector('.el-table__body-wrapper')
        if (!bodyTable || !wrapper) return

        // 读取所有列的实际渲染宽度(取第一行)
        const firstRow = bodyTable.querySelector('tr')
        if (!firstRow) return
        const cells = firstRow.querySelectorAll('td')
        let totalWidth = 0
        cells.forEach(cell => {
          totalWidth += cell.getBoundingClientRect().width
        })

        const containerWidth = wrapper.clientWidth
        if (totalWidth > containerWidth + 1) {
          // 列宽之和超出容器 → 显式设置表格宽度,让水平滚动出现
          const w = Math.ceil(totalWidth) + 'px'
          bodyTable.style.width = w
          if (headerTable) headerTable.style.width = w
        }

        // 调用 doLayout 让 Element UI 同步固定列和滚动状态
        if (typeof tableVm.doLayout === 'function') {
          tableVm.doLayout()
        }

        // doLayout 可能重置宽度,再设一次
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
