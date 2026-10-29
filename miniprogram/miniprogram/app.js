// 自主台球厅小程序 - 全局入口
const api = require('./utils/api')

App({
  globalData: {
    userInfo: null,
    userId: '',
    token: ''
  },

  onLaunch() {
    // 微信登录：wx.login 拿 code -> 后端换 openid
    wx.login({
      success: async (res) => {
        try {
          const data = await api.login({ code: res.code })
          const user = data.user
          this.globalData.userInfo = user
          this.globalData.userId = user.id
          this.globalData.token = data.token
          wx.setStorageSync('userId', user.id)
          wx.setStorageSync('token', data.token)
          console.log('登录成功 userId=', user.id)
        } catch (e) {
          console.error('登录失败', e)
        }
      }
    })
  }
})
