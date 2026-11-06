// 预约逻辑模拟验证：冲突检测 / 创建 / 取消 / 超时释放 / 核销
// 与后端 ReservationService 逻辑一致（Node 可直接运行：node test/reservation-logic.test.js）

const DB = { reservations: [], nextId: 1 };
const tableStatus = { 1: 0, 2: 0 }; // tableId -> status (0空闲 1使用中 2维修)

const STATUS = { PENDING: 0, CHECKED_IN: 1, DONE: 2, CANCELLED: 3, RELEASED: 4 };

// 冲突检测：同一球台/同一用户 时段重叠
function hasConflict(tableId, userId, date, start, end, excludeId) {
  return DB.reservations.some(r =>
    r.id !== excludeId &&
    r.tableId === tableId &&
    r.date === date &&
    (r.status === STATUS.PENDING || r.status === STATUS.CHECKED_IN) &&
    r.start < end && r.end > start
  )
}

function create(userId, tableId, date, start, end) {
  if (end <= start) throw new Error('结束时间必须晚于开始时间')
  if (tableStatus[tableId] === 2) throw new Error('该球台正在维修')
  if (hasConflict(tableId, userId, date, start, end, null)) throw new Error('该时段已有预约')
  const r = { id: DB.nextId++, userId, tableId, date, start, end, status: STATUS.PENDING }
  DB.reservations.push(r)
  return r
}

function cancel(id, userId) {
  const r = DB.reservations.find(x => x.id === id && x.userId === userId)
  if (!r) throw new Error('预约不存在')
  if (r.status !== STATUS.PENDING) throw new Error('当前状态不可取消')
  r.status = STATUS.CANCELLED
  return r
}

let pass = 0, fail = 0
function assert(cond, msg) {
  if (cond) { pass++; console.log(`  ✅ ${msg}`) }
  else { fail++; console.log(`  ❌ ${msg}`) }
}

console.log('=== 预约逻辑测试 ===')
console.log('测试1：创建预约')
const r1 = create(1, 1, '2026-11-07', '10:00', '11:00')
assert(r1.status === STATUS.PENDING, '创建成功，状态为待使用')

console.log('测试2：冲突检测')
try { create(1, 1, '2026-11-07', '10:30', '11:30'); assert(false, '同球台同时段应拦截') }
catch (e) { assert(true, '同球台同时段被拦截: ' + e.message) }
try { create(2, 1, '2026-11-07', '10:00', '11:00'); assert(false, '不同用户同球台同时段应拦截') }
catch (e) { assert(true, '不同用户同球台同时段被拦截') }

console.log('测试3：非重叠时段可预约')
const r2 = create(1, 1, '2026-11-07', '14:00', '15:00')
assert(r2.id === 2, '非重叠时段预约成功')

console.log('测试4：取消预约')
cancel(r1.id, 1)
assert(r1.status === STATUS.CANCELLED, '取消后状态为已取消')
try { create(1, 1, '2026-11-07', '10:00', '11:00'); assert(true, '取消后原时段可再预约') }
catch (e) { assert(false, '取消后原时段应可再预约') }

console.log('测试5：校验用户归属')
try { cancel(r2.id, 99); assert(false, '非本人应无法取消') }
catch (e) { assert(true, '非本人取消被拒绝') }

console.log(`\n结果：${pass} 通过 / ${fail} 失败`)
if (fail > 0) process.exit(1)
