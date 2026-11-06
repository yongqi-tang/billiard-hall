// 在线预约页：选球台 -> 选日期 -> 选时间段 -> 提交
const api = require('../../utils/api')

// 可预约时段（半小时粒度，营业时间 10:00-22:30）
const ALL_SLOTS = (() => {
  const slots = []
  for (let h = 10; h <= 21; h++) {
    for (const m of [0, 30]) {
      const start = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`
      const endH = m === 30 ? h + 1 : h
      const endM = m === 30 ? 0 : 30
      if (endH > 22) break
      slots.push({ start, end: `${String(endH).padStart(2, '0')}:${String(endM).padStart(2, '0')}:00`, label: `${start.slice(0, 5)} - ${`${String(endH).padStart(2, '0')}:${String(endM).padStart(2, '0')}`}` })
    }
  }
  return slots
})()

Page({
  data: {
    tables: [],
    loading: true,
    selectedTableId: null,
    dates: [],
    selectedDate: '',
    slots: ALL_SLOTS,
    selectedSlotIndex: -1,
    remark: ''
  },

  onLoad(options) {
    // 从首页点进来可带 tableId
    const tableId = options.tableId ? Number(options.tableId) : null
    this.setData({ selectedTableId: tableId })
    this.buildDates()
    this.loadTables()
  },

  // 生成未来 7 天日期
  buildDates() {
    const week = ['日', '一', '二', '三', '四', '五', '六']
    const dates = []
    const today = new Date()
    for (let i = 0; i < 7; i++) {
      const d = new Date(today)
      d.setDate(today.getDate() + i)
      const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      dates.push({
        date: dateStr,
        label: i === 0 ? '今天' : i === 1 ? '明天' : `${d.getMonth() + 1}月${d.getDate()}日`,
        week: `周${week[d.getDay()]}`
      })
    }
    this.setData({ dates, selectedDate: dates[0].date })
  },

  async loadTables() {
    this.setData({ loading: true })
    try {
      const tables = await api.getTableList()
      this.setData({ tables })
      if (!this.data.selectedTableId && tables.length) {
        this.setData({ selectedTableId: tables[0].id })
      }
    } catch (e) {
      console.error(e)
    } finally {
      this.setData({ loading: false })
    }
  },

  selectTable(e) {
    this.setData({ selectedTableId: e.currentTarget.dataset.id })
  },

  selectDate(e) {
    this.setData({ selectedDate: e.currentTarget.dataset.date, selectedSlotIndex: -1 })
  },

  selectSlot(e) {
    this.setData({ selectedSlotIndex: e.currentTarget.dataset.index })
  },

  onRemark(e) {
    this.setData({ remark: e.detail.value })
  },

  // 提交预约
  async onSubmit() {
    const { selectedTableId, selectedDate, selectedSlotIndex, remark } = this.data
    const userId = wx.getStorageSync('userId')
    if (!userId) {
      wx.showToast({ title: '登录中，请稍后再试', icon: 'none' })
      return
    }
    if (!selectedTableId) {
      wx.showToast({ title: '请选择球台', icon: 'none' })
      return
    }
    if (selectedSlotIndex < 0) {
      wx.showToast({ title: '请选择时间段', icon: 'none' })
      return
    }
    const slot = ALL_SLOTS[selectedSlotIndex]

    wx.showLoading({ title: '预约中...' })
    try {
      await api.createReservation({
        userId: Number(userId),
        tableId: selectedTableId,
        reserveDate: selectedDate,
        startTime: slot.start,
        endTime: slot.end,
        remark: remark || ''
      })
      wx.hideLoading()
      wx.showToast({ title: '预约成功', icon: 'success' })
      setTimeout(() => {
        wx.navigateBack()
      }, 1200)
    } catch (e) {
      wx.hideLoading()
    }
  }
})
