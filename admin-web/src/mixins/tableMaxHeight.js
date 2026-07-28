/**
 * 表格滚动 mixin
 *
 * 关键原理:
 *   el-table 根元素保持 width:100% (= 容器宽度, 如 1880px)
 *   body-wrapper 也是 width:100% (= 容器宽度)
 *   内部 table 设为列宽之和 (如 2850px) → 超出 body-wrapper → 水平滚动条
 *   MutationObserver 防止 Element UI 重置内部 table 宽度
 */
export default {
  data() {
    return {
      tableMaxHeight: 500
    }
  },
  mounted() {
    this._calcHeight()
    this._initScroll()
    window.addEventListener('resize', this._calcHeight)
  },
  activated() {
    this._calcHeight()
    this.$nextTick(() => this._apply())
  },
  updated() {
    if (this._ut) clearTimeout(this._ut)
    this._ut = setTimeout(() => this._apply(), 100)
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

    _initScroll() {
      this.$nextTick(() => {
        const t = this.$el && this.$el.querySelector('.el-table')
        if (!t) return

        const bw = t.querySelector('.el-table__body-wrapper')
        const hw = t.querySelector('.el-table__header-wrapper')
        if (!bw) return

        this._bw = bw
        this._hw = hw

        // body-wrapper: overflow:auto → 它负责水平和垂直滚动
        bw.style.overflow = 'auto'
        bw.style.overflowX = 'auto'
        bw.style.overflowY = 'auto'
        bw.style.maxHeight = this.tableMaxHeight + 'px'
        // 关键: body-wrapper 宽度 = 容器宽度 (不是 totalWidth!)
        bw.style.width = '100%'

        // header-wrapper: 隐藏溢出, 水平滚动由 JS 同步
        if (hw) {
          hw.style.overflow = 'hidden'
          hw.style.width = '100%'
        }

        // MutationObserver: 防止 Element UI JS 重置关键样式
        this._mo = new MutationObserver(() => {
          let changed = false
          if (bw.style.overflowX !== 'auto') {
            bw.style.overflow = 'auto'
            bw.style.overflowX = 'auto'
            bw.style.overflowY = 'auto'
            changed = true
          }
          if (bw.style.width !== '100%') {
            bw.style.width = '100%'
            changed = true
          }
          if (changed) this._apply()
        })
        this._mo.observe(bw, { attributes: true, attributeFilter: ['style'] })

        // 同步 header/body 水平滚动
        this._onBS = () => {
          if (hw) hw.scrollLeft = bw.scrollLeft
        }
        bw.addEventListener('scroll', this._onBS)

        // 延迟应用 (等数据加载)
        setTimeout(() => this._apply(), 300)
        setTimeout(() => this._apply(), 800)
        setTimeout(() => this._apply(), 1500)
      })
    },

    _apply() {
      const t = this.$el && this.$el.querySelector('.el-table')
      if (!t) return

      // ★ 确保 el-table 根元素保持 width:100% (容器宽度)
      // 不设 totalWidth! 这样 body-wrapper 的 100% = 容器宽度
      if (t.style.width && t.style.width !== '100%') {
        t.style.width = '100%'
      }

      const vue = t.__vue__
      const cols = (vue && vue.store && vue.store.states && vue.store.states.columns) || []
      if (!cols.length) return

      // 计算列宽之和
      let tw = 0
      cols.forEach(c => {
        const w = parseInt(c.width || c.minWidth || 0, 10)
        if (w > 0) tw += w
      })
      if (tw <= 0) return

      // ★ 内部 body table = 列宽之和 (超出 body-wrapper → 触发滚动条)
      const bt = t.querySelector('.el-table__body')
      if (bt) {
        bt.style.width = tw + 'px'
        bt.style.minWidth = tw + 'px'
        bt.style.maxWidth = 'none'
        bt.style.tableLayout = 'fixed'
      }

      // ★ 内部 header table = 列宽之和 (溢出 header-wrapper)
      const ht = t.querySelector('.el-table__header')
      if (ht) {
        ht.style.width = tw + 'px'
        ht.style.minWidth = tw + 'px'
        ht.style.maxWidth = 'none'
        ht.style.tableLayout = 'fixed'
      }

      // 设置每个单元格宽度
      if (bt) {
        bt.querySelectorAll('tr').forEach(r => {
          r.querySelectorAll('td').forEach((c, i) => {
            if (i >= cols.length) return
            const w = parseInt(cols[i].width || cols[i].minWidth || 0, 10)
            if (w > 0) c.style.width = w + 'px'
          })
        })
      }
      if (ht) {
        ht.querySelectorAll('tr').forEach(r => {
          r.querySelectorAll('th').forEach((c, i) => {
            if (i >= cols.length) return
            const w = parseInt(cols[i].width || cols[i].minWidth || 0, 10)
            if (w > 0) c.style.width = w + 'px'
          })
        })
      }

      // 确保 body-wrapper 样式正确
      const bw = t.querySelector('.el-table__body-wrapper')
      if (bw) {
        bw.style.overflow = 'auto'
        bw.style.overflowX = 'auto'
        bw.style.overflowY = 'auto'
        bw.style.maxHeight = this.tableMaxHeight + 'px'
        bw.style.width = '100%'
      }
    }
  }
}
