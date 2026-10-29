/**
 * 请求封装：统一处理 baseUrl、token、错误提示
 */
const config = require('../config')

function request(options) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: config.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        Authorization: wx.getStorageSync('token') || ''
      },
      success(res) {
        const body = res.data
        if (body && body.code === 0) {
          resolve(body.data)
        } else {
          wx.showToast({ title: (body && body.message) || '请求失败', icon: 'none' })
          reject(body)
        }
      },
      fail(err) {
        wx.showToast({ title: '网络异常，请检查后端是否启动', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = {
  get(url, data) {
    return request({ url, data })
  },
  post(url, data) {
    return request({ url, data, method: 'POST' })
  }
}
