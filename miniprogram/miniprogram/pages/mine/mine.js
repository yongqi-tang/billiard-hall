// 我的页面：余额、充值、功能入口
const api = require('../../utils/api')

Page({
  data: {
    userInfo: {},
    balance: '0.00',
    memberText: '普通会员'
  },

  onShow() {
    this.loadUser()
  },

  async loadUser() {
    const userId = wx.getStorageSync('userId')
    if (!userId) return
    try {
      const user = await api.getUserInfo(userId)
      const memberText = user.memberLevel === 0 ? '普通会员' : user.memberLevel === 1 ? '白银会员' : '黄金会员'
      this.setData({
        userInfo: user,
        balance: user.balance.toFixed(2),
        memberText
      })
    } catch (e) {
      console.error(e)
    }
  },

  // 模拟充值
  onRecharge(e) {
    const amount = e.currentTarget.dataset.amount
    const userId = wx.getStorageSync('userId')
    if (!userId) {
      wx.showToast({ title: '登录中，请稍后再试', icon: 'none' })
      return
    }
    wx.showModal({
      title: '确认充值',
      content: `模拟支付 ¥${amount}（开发阶段）`,
      success: async (res) => {
        if (!res.confirm) return
        try {
          const user = await api.recharge({ userId: Number(userId), amount })
          this.setData({ balance: user.balance.toFixed(2) })
          wx.showToast({ title: '充值成功', icon: 'success' })
        } catch (e) {}
      }
    })
  },

  goPlaying() {
    wx.navigateTo({ url: '/pages/playing/playing' })
  },

  goOrders() {
    wx.navigateTo({ url: '/pages/orders/orders' })
  },

  goReservations() {
    wx.switchTab({ url: '/pages/reservations/reservations' })
  },

  goReserve() {
    wx.navigateTo({ url: '/pages/reserve/reserve' })
  },

  goScan() {
    wx.navigateTo({ url: '/pages/scan/scan' })
  }
})
