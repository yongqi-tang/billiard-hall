// 首页：球台列表 + 扫码 + 预约入口
const api = require('../../utils/api')

const STATUS_INFO = {
  0: { text: '空闲', cls: 'free', icon: '🟢' },
  1: { text: '使用中', cls: 'busy', icon: '🔴' },
  2: { text: '维修', cls: 'repair', icon: '🛠️' }
}

Page({
  data: {
    tables: [],
    loading: false,
    hasActiveOrder: false
  },

  onShow() {
    this.loadTables()
    this.checkActive()
  },

  // 拉取球台列表
  async loadTables() {
    this.setData({ loading: true })
    try {
      const tables = await api.getTableList()
      const mapped = tables.map(t => {
        const info = STATUS_INFO[t.status] || { text: '已预约', cls: 'reserved', icon: '📅' }
        const free = t.status === 0
        return {
          ...t,
          statusText: t.status === 3 ? '有预约' : info.text,
          statusCls: t.status === 3 ? 'reserved' : info.cls,
          // 空闲且有预约 -> 可预约；空闲无预约 -> 可扫码/预约
          reservedTag: t.reserved && free ? '已有预约' : '',
          free
        }
      })
      this.setData({ tables: mapped })
    } catch (e) {
      console.error('加载球台失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  // 是否有进行中订单（用于首页提示）
  async checkActive() {
    const userId = wx.getStorageSync('userId')
    if (!userId) return
    try {
      const data = await api.getCurrentOrder(userId)
      this.setData({ hasActiveOrder: !!data })
    } catch (e) {}
  },

  // 扫码开台
  goScan() {
    wx.navigateTo({ url: '/pages/scan/scan' })
  },

  // 在线预约入口
  goReserve() {
    wx.navigateTo({ url: '/pages/reserve/reserve' })
  },

  // 点击球台 -> 进入预约（空闲球台）或提示
  onTableTap(e) {
    const id = e.currentTarget.dataset.id
    const table = this.data.tables.find(t => t.id === id)
    if (!table) return
    if (table.status === 0) {
      wx.navigateTo({ url: `/pages/reserve/reserve?tableId=${id}` })
    } else if (table.status === 1) {
      wx.showToast({ title: '该球台正在使用中', icon: 'none' })
    } else if (table.status === 2) {
      wx.showToast({ title: '该球台维修中', icon: 'none' })
    } else {
      wx.showToast({ title: '该球台已被预约', icon: 'none' })
    }
  }
})
