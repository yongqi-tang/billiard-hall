// 我的预约：查看/取消预约
const api = require('../../utils/api')

const STATUS_TEXT = { 0: '待使用', 1: '已开台', 2: '已完成', 3: '已取消', 4: '已释放' }
const STATUS_CLASS = { 0: 's0', 1: 's1', 2: 's2', 3: 's3', 4: 's4' }

Page({
  data: {
    reservations: [],
    loading: false
  },

  onShow() {
    this.loadReservations()
  },

  async loadReservations() {
    const userId = wx.getStorageSync('userId')
    if (!userId) return
    this.setData({ loading: true })
    try {
      const list = await api.getMyReservations(userId)
      const reservations = list.map(r => ({
        ...r,
        statusText: STATUS_TEXT[r.status] || '未知',
        statusClass: STATUS_CLASS[r.status] || 's0',
        dateLabel: `${r.reserveDate}  ${r.startTime.slice(0, 5)}-${r.endTime.slice(0, 5)}`
      }))
      this.setData({ reservations })
    } catch (e) {
      console.error(e)
    } finally {
      this.setData({ loading: false })
    }
  },

  // 取消预约
  onCancel(e) {
    const { id } = e.currentTarget.dataset
    const userId = wx.getStorageSync('userId')
    wx.showModal({
      title: '取消预约',
      content: '确认取消这条预约吗？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.cancelReservation({ reservationId: id, userId })
          wx.showToast({ title: '已取消', icon: 'success' })
          this.loadReservations()
        } catch (err) {}
      }
    })
  },

  // 去预约
  goReserve() {
    wx.navigateTo({ url: '/pages/reserve/reserve' })
  }
})
