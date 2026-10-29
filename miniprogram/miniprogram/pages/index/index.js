// 首页：球台列表 + 扫码入口
const api = require('../../utils/api')

Page({
  data: {
    tables: [],
    loading: false
  },

  onShow() {
    this.loadTables()
  },

  // 拉取球台列表（后端 /api/table/list）
  async loadTables() {
    this.setData({ loading: true })
    try {
      const tables = await api.getTableList()
      this.setData({ tables })
    } catch (e) {
      console.error('加载球台失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  // 扫码开台
  goScan() {
    wx.navigateTo({ url: '/pages/scan/scan' })
  }
})
