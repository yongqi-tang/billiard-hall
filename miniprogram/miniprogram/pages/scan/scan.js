// 扫码开台页：扫码 -> 拿到球台ID -> 调后端开台接口
const api = require('../../utils/api')

Page({
  data: {
    scanning: false,
    hasReservation: false
  },

  // 调起摄像头扫码
  scanCode() {
    if (this.data.scanning) return
    wx.scanCode({
      success: (res) => {
        this.handleScanResult(res.result)
      },
      fail: () => {
        wx.showToast({ title: '已取消扫码', icon: 'none' })
      }
    })
  },

  // 解析二维码内容 -> 开台
  async handleScanResult(result) {
    // 约定二维码内容格式：table://球台ID  或直接是数字ID
    let tableId = result
    if (result && result.indexOf('table://') === 0) {
      tableId = result.replace('table://', '')
    }
    if (!tableId || isNaN(Number(tableId))) {
      wx.showToast({ title: '二维码格式不正确', icon: 'none' })
      return
    }

    const userId = wx.getStorageSync('userId')
    if (!userId) {
      wx.showToast({ title: '登录中，请稍后再试', icon: 'none' })
      return
    }

    this.setData({ scanning: true })
    wx.showLoading({ title: '开台中...' })
    try {
      await api.openTable({ userId: Number(userId), tableId: Number(tableId) })
      wx.hideLoading()
      wx.showToast({ title: '开台成功，灯光已亮', icon: 'success' })
      // 跳转到计时页
      setTimeout(() => {
        wx.navigateTo({ url: '/pages/playing/playing' })
      }, 800)
    } catch (e) {
      wx.hideLoading()
      // 错误提示已由 request 统一处理
    } finally {
      this.setData({ scanning: false })
    }
  }
})
