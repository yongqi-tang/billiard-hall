// 进行中的球局：实时计时 + 续时 + 结账
const api = require('../../utils/api')

Page({
  data: {
    order: null,
    table: null,
    estimatedAmount: '0.00',
    displayTime: '00:00',
    debt: false,
    extendPrice: '30.00',
    _timer: null
  },

  onShow() {
    this.loadCurrent()
    // 每秒刷新计时
    this.setData({ _timer: setInterval(() => this.loadCurrent(), 1000) })
  },

  onHide() {
    if (this.data._timer) clearInterval(this.data._timer)
  },

  onUnload() {
    if (this.data._timer) clearInterval(this.data._timer)
  },

  async loadCurrent() {
    const userId = wx.getStorageSync('userId')
    if (!userId) return
    try {
      const data = await api.getCurrentOrder(userId)
      if (!data) {
        this.setData({ order: null, table: null })
        return
      }
      this.setData({
        order: data.order,
        table: data.table,
        estimatedAmount: data.estimatedAmount.toFixed(2),
        debt: data.debt,
        displayTime: this.formatTime(data.usedMinutes)
      })
    } catch (e) {
      console.error(e)
    }
  },

  formatTime(minutes) {
    const h = Math.floor(minutes / 60)
    const m = minutes % 60
    return (h > 0 ? String(h).padStart(2, '0') + ':' : '') + String(m).padStart(2, '0')
  },

  // 续时
  async onExtend() {
    const userId = wx.getStorageSync('userId')
    try {
      await api.extendOrder({ orderId: this.data.order.id, userId })
      wx.showToast({ title: '续时成功', icon: 'success' })
    } catch (e) {}
  },

  // 结账
  onSettle() {
    wx.showModal({
      title: '确认结账',
      content: `本次消费预估 ¥${this.data.estimatedAmount}，确认结账离场？`,
      success: async (res) => {
        if (!res.confirm) return
        const userId = wx.getStorageSync('userId')
        try {
          await api.settleOrder({ orderId: this.data.order.id, userId })
          wx.showToast({ title: '结账成功', icon: 'success' })
          setTimeout(() => this.loadCurrent(), 800)
        } catch (e) {}
      }
    })
  },

  // 取消
  onCancel() {
    wx.showModal({
      title: '取消订单',
      content: '取消后已扣费用将退回，确认取消？',
      success: async (res) => {
        if (!res.confirm) return
        const userId = wx.getStorageSync('userId')
        try {
          await api.cancelOrder({ orderId: this.data.order.id, userId })
          wx.showToast({ title: '已取消', icon: 'success' })
          setTimeout(() => this.loadCurrent(), 800)
        } catch (e) {}
      }
    })
  },

  goIndex() {
    wx.switchTab({ url: '/pages/index/index' })
  }
})
