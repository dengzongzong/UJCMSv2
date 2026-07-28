/**
 * 表格最大高度 mixin
 * 自动计算 el-table 的 max-height,使表格在可视区域内滚动
 * 水平+垂直滚动条都在表格内部显示,无需滑到页面底部
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
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.calcTableMaxHeight)
  },
  methods: {
    calcTableMaxHeight() {
      // 减去: header(56) + main padding(32) + app-container padding(40)
      //        + 筛选区/工具栏/分页等(~170) ≈ 300
      this.tableMaxHeight = Math.max(200, window.innerHeight - 300)
    }
  }
}
