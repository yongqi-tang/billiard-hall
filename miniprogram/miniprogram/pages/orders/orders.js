// 我的订单历史
const api = require('../../utils/api')

Page({
  data: {
    orders: []
  },

  onShow() {
    this.loadOrders()
  },

  async loadOrders() {
    const userId = wx.getStorageSync('userId')
    if (!userId) return
    try {
      const orders = await api.getMyOrders(userId)
      this.setData({ orders })
    } catch (e) {
      console.error(e)
    }
  }
})
