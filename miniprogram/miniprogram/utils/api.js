/**
 * 接口统一管理：所有后端接口都在这登记
 */
const { get, post } = require('./request')

module.exports = {
  // 用户模块
  login: (data) => post('/api/user/login', data),
  recharge: (data) => post('/api/user/recharge', data),
  getUserInfo: (userId) => get(`/api/user/info?userId=${userId}`),

  // 球台模块
  getTableList: () => get('/api/table/list'),

  // 订单模块
  openTable: (data) => post('/api/order/open', data),
  extendOrder: (data) => post('/api/order/extend', data),
  settleOrder: (data) => post('/api/order/settle', data),
  cancelOrder: (data) => post('/api/order/cancel', data),
  getCurrentOrder: (userId) => get(`/api/order/current?userId=${userId}`),
  getMyOrders: (userId) => get(`/api/order/list?userId=${userId}`)
}
