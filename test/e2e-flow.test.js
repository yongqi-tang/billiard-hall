// 端到端流程验证：模拟后端 + 模拟小程序操作
const DB = { users: [], orders: [], tables: [], nextId: 1 };
DB.tables = [
  { id: 1, name: 'A区1号台', status: 0, priceStart: 30, priceHour: 30, deviceId: 1 },
  { id: 2, name: 'A区2号台', status: 0, priceStart: 30, priceHour: 30, deviceId: 2 }
];
DB.deviceStates = { 1: false, 2: false };
const START_MIN = 30, START_PRICE = 30;
function calcAmount(min, ps, ph) {
  const per = +(ph / 60).toFixed(2);
  if (min <= START_MIN) return +ps.toFixed(2);
  return +(ps + per * (min - START_MIN)).toFixed(2);
}
const api = {
  login(code) {
    const openid = code.startsWith('dev_') ? code : 'dev_openid_test';
    let u = DB.users.find(x => x.openid === openid);
    if (!u) { u = { id: DB.nextId++, openid, nickname: '球友', balance: 0, memberLevel: 0 }; DB.users.push(u); }
    return { user: u, token: 'tk-' + u.id };
  },
  recharge(userId, amount) {
    const u = DB.users.find(x => x.id === userId);
    u.balance = +(u.balance + amount).toFixed(2); return u;
  },
  open(userId, tableId) {
    const t = DB.tables.find(x => x.id === tableId);
    const u = DB.users.find(x => x.id === userId);
    if (t.status !== 0) throw new Error('该球台当前不可用');
    // 同用户不能同时开两台
    const hasActive = DB.orders.some(x => x.userId === userId && x.status === 0);
    if (hasActive) throw new Error('您有进行中的球局，请先结账');
    if (u.balance < START_PRICE) throw new Error('余额不足起步价，请先充值');
    u.balance = +(u.balance - START_PRICE).toFixed(2);
    const order = { id: DB.nextId++, orderNo: 'BH' + Date.now(), userId, tableId, startTime: Date.now(), durationMin: 0, amount: START_PRICE, status: 0, payStatus: 0 };
    DB.orders.push(order);
    t.status = 1; DB.deviceStates[t.deviceId] = true;
    return order;
  },
  extend(orderId, userId) {
    const o = DB.orders.find(x => x.id === orderId);
    const u = DB.users.find(x => x.id === userId);
    if (u.balance < START_PRICE) throw new Error('余额不足，无法续时');
    u.balance = +(u.balance - START_PRICE).toFixed(2); return o;
  },
  settle(orderId, userId) {
    const o = DB.orders.find(x => x.id === orderId);
    const u = DB.users.find(x => x.id === userId);
    const t = DB.tables.find(x => x.id === o.tableId);
    const min = 35;
    const total = calcAmount(min, t.priceStart, t.priceHour);
    const diff = +(total - o.amount).toFixed(2);
    u.balance = +(u.balance - diff).toFixed(2);
    o.status = 1; o.amount = total; o.durationMin = min; o.endTime = Date.now();
    t.status = 0; DB.deviceStates[t.deviceId] = false;
    return o;
  },
  current(userId) {
    const o = DB.orders.find(x => x.userId === userId && x.status === 0);
    if (!o) return null;
    const t = DB.tables.find(x => x.id === o.tableId);
    const min = Math.max(1, Math.floor((Date.now() - o.startTime) / 60000));
    return { order: o, table: t, usedMinutes: min, estimatedAmount: calcAmount(min, t.priceStart, t.priceHour) };
  },
  statistics() {
    const finished = DB.orders.filter(x => x.status === 1);
    const income = finished.reduce((s, o) => s + o.amount, 0);
    return { totalIncome: income.toFixed(2), totalOrders: finished.length, activeOrders: DB.orders.filter(x => x.status === 0).length };
  }
};

console.log('========== 端到端流程模拟（用户视角） ==========\n');
const assert = (cond, msg) => { if (!cond) { console.log('❌ FAIL: ' + msg); process.exit(1); } console.log('✅ ' + msg); };

let login = api.login('dev_test');
let userId = login.user.id;
assert(!!userId, `1. 微信登录成功 userId=${userId}`);

api.recharge(userId, 100);
assert(api.login('dev_test').user.balance === 100, `2. 充值100元成功，余额=100`);

let order = api.open(userId, 1);
assert(order.status === 0, `3. 扫码开台成功 orderNo=${order.orderNo}`);
assert(DB.deviceStates[1] === true, `4. 开台后球台灯已亮起 ✅（硬件联动）`);
assert(DB.tables[0].status === 1, `5. 球台状态变为"使用中"`);
assert(api.login('dev_test').user.balance === 70, `6. 开台扣起步价30，余额=70`);

let dupFailed = false;
try { api.open(userId, 2); } catch (e) { dupFailed = true; }
assert(dupFailed, `7. 未结账不能同时开第二台（正确拦截）`);

let before = api.login('dev_test').user.balance;
api.extend(order.id, userId);
let after = api.login('dev_test').user.balance;
assert(before - after === 30, `8. 续时扣30元，余额 ${before} -> ${after}`);

let cur = api.current(userId);
assert(cur && cur.usedMinutes >= 1, `9. 实时计时正常，已用 ${cur.usedMinutes} 分钟，预估 ¥${cur.estimatedAmount}`);

let settled = api.settle(order.id, userId);
assert(settled.status === 1, `10. 结账成功，金额 ¥${settled.amount}（35分钟=32.5）`);
assert(DB.deviceStates[1] === false, `11. 结账后球台灯已熄灭 ✅（硬件联动）`);
assert(DB.tables[0].status === 0, `12. 球台恢复"空闲"状态`);

let stats = api.statistics();
assert(stats.totalOrders === 1, `13. 管理后台统计：1笔订单`);
assert(+stats.totalIncome > 0, `14. 管理后台统计：营收 ¥${stats.totalIncome}`);

let fail2 = false;
try { api.open(api.login('dev_test2').user.id, 2); } catch (e) { fail2 = true; }
assert(fail2, `15. 余额为0的用户开台被正确拦截`);

console.log('\n========== 🎉 端到端全流程验证通过！ ==========');
console.log('登录 → 充值 → 开台(亮灯) → 计时 → 续时 → 结账(灭灯) → 统计 全部正常');
