// 真实后端集成测试：登录 -> 充值 -> 开台(亮灯) -> 计时 -> 续时 -> 结账(灭灯) -> 统计
const BASE = 'http://localhost:8080';
let passed = 0, failed = 0;
const assert = (cond, msg) => { if (cond) { passed++; console.log('  ✅ ' + msg); } else { failed++; console.log('  ❌ ' + msg); } };

async function api(method, path, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined
  });
  const data = await res.json();
  if (data.code !== 0) throw new Error(data.message || '请求失败');
  return data.data;
}

(async () => {
  console.log('===== 真实后端端到端测试 =====');

  // 1. 登录
  const login = await api('POST', '/api/user/login', { code: 'dev_apitest' });
  const uid = login.user.id;
  assert(!!uid, `1. 微信登录成功 userId=${uid}，openid=${login.user.openid}`);

  // 2. 充值
  const u1 = await api('POST', '/api/user/recharge', { userId: uid, amount: 100 });
  assert(u1.balance === 100, `2. 充值100元成功，余额=${u1.balance}`);

  // 3. 开台（亮灯）
  const order = await api('POST', '/api/order/open', { userId: uid, tableId: 1 });
  assert(order.status === 0, `3. 扫码开台成功 orderNo=${order.orderNo}`);
  const dev = await api('GET', '/api/device/list');
  assert(dev.find(d => d.bindTableId === 1).status === 1, `4. 开台后 1号台灯已亮起（硬件联动）`);
  const tables = await api('GET', '/api/table/list');
  assert(tables.find(t => t.id === 1).status === 1, `5. 球台1状态变为"使用中"`);
  const u2 = await api('GET', `/api/user/info?userId=${uid}`);
  assert(u2.balance === 70, `6. 开台扣起步价30，余额=${u2.balance}`);

  // 7. 未结账不能开第二台
  let dupBlocked = false;
  try { await api('POST', '/api/order/open', { userId: uid, tableId: 2 }); } catch (e) { dupBlocked = true; }
  assert(dupBlocked, `7. 未结账不能同时开第二台（正确拦截）`);

  // 8. 续时
  const u3 = await api('GET', `/api/user/info?userId=${uid}`);
  await api('POST', '/api/order/extend', { orderId: order.id, userId: uid });
  const u4 = await api('GET', `/api/user/info?userId=${uid}`);
  assert(u3.balance - u4.balance === 30, `8. 续时扣30元，余额 ${u3.balance} -> ${u4.balance}`);

  // 9. 当前订单/计时
  const cur = await api('GET', `/api/order/current?userId=${uid}`);
  assert(cur && cur.usedMinutes >= 1 && cur.estimatedAmount >= 30, `9. 实时计时正常，已用${cur.usedMinutes}分钟，预估¥${cur.estimatedAmount}`);

  // 10. 结账（灭灯）
  const settled = await api('POST', '/api/order/settle', { orderId: order.id, userId: uid });
  assert(settled.status === 1, `10. 结账成功 金额¥${settled.amount} 时长${settled.durationMin}分钟`);
  const dev2 = await api('GET', '/api/device/list');
  assert(dev2.find(d => d.bindTableId === 1).status === 0, `11. 结账后 1号台灯已熄灭（硬件联动）`);
  const tables2 = await api('GET', '/api/table/list');
  assert(tables2.find(t => t.id === 1).status === 0, `12. 球台1恢复"空闲"`);

  // 13. 订单历史
  const orders = await api('GET', `/api/order/list?userId=${uid}`);
  assert(orders.length === 1, `13. 我的订单历史：${orders.length} 笔`);

  // 14. 管理后台统计
  const stats = await api('GET', '/api/admin/statistics');
  assert(stats.totalOrders >= 1 && +stats.totalIncome > 0, `14. 管理后台统计：营收¥${stats.totalIncome} 订单${stats.totalOrders}`);

  // 15. 余额不足用户开台被拦截
  const login2 = await api('POST', '/api/user/login', { code: 'dev_poor' });
  let poorBlocked = false;
  try { await api('POST', '/api/order/open', { userId: login2.user.id, tableId: 2 }); } catch (e) { poorBlocked = true; }
  assert(poorBlocked, `15. 余额为0的用户开台被正确拦截`);

  // 16. 管理员登录
  const admin = await api('POST', '/api/admin/login', { username: 'admin', password: 'admin123' });
  assert(admin.token === 'admin-token', `16. 管理后台登录成功`);

  console.log(`\n===== 结果：${passed} 通过 / ${failed} 失败 =====`);
  process.exit(failed === 0 ? 0 : 1);
})().catch(e => { console.error('测试异常：' + e.message); process.exit(1); });
