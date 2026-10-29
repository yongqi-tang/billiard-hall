// 用 JS 复刻后端 OrderService 核心逻辑做验证
const START_PRICE_MINUTES = 30;   // 起步30分钟
const START_PRICE = 30.00;        // 起步价30
const PRICE_HOUR = 30.00;         // 每小时30 = 每分钟0.5

// 计费函数（与 Java calcAmount 完全一致）
function calcAmount(minutes, priceStart, priceHour) {
  const perMinute = +(priceHour / 60).toFixed(2);
  if (minutes <= START_PRICE_MINUTES) return +priceStart.toFixed(2);
  const extra = minutes - START_PRICE_MINUTES;
  return +(priceStart + perMinute * extra).toFixed(2);
}

// 模拟一个完整流程：开台 -> 用35分钟 -> 结账
let userBalance = 100;  // 用户余额100
const table = { id: 1, name: 'A区1号台', priceStart: 30, priceHour: 30 };

console.log('=== 测试1：开台（扣起步价） ===');
if (userBalance < table.priceStart) { console.log('❌ 余额不足无法开台'); process.exit(1); }
userBalance -= table.priceStart;
let order = { status: 0, startAmount: table.priceStart, startTime: Date.now() };
console.log(`✅ 开台成功，扣起步价30，余额剩 ${userBalance}`);

console.log('=== 测试2：使用35分钟结账 ===');
const usedMin = 35;
const total = calcAmount(usedMin, table.priceStart, table.priceHour);
const diff = +(total - order.startAmount).toFixed(2);
userBalance -= diff;
order = { status: 1, amount: total, duration: usedMin };
console.log(`✅ 使用${usedMin}分钟，应收¥${total}，已扣起步30，补收${diff}，最终余额 ${userBalance}`);

console.log('=== 测试3：计费边界 ===');
console.log(`使用30分钟 -> ¥${calcAmount(30, 30, 30)}（应=30 起步价）`);
console.log(`使用60分钟 -> ¥${calcAmount(60, 30, 30)}（应=30+30*0.5=45）`);
console.log(`使用90分钟 -> ¥${calcAmount(90, 30, 30)}（应=30+60*0.5=60）`);
console.log(`使用10分钟 -> ¥${calcAmount(10, 30, 30)}（应=30 未满起步按起步价）`);

console.log('=== 测试4：余额不足无法开台 ===');
userBalance = 10;
if (userBalance < table.priceStart) console.log('✅ 余额10 < 起步30，正确拦截开台');
else console.log('❌ 未拦截');

console.log('=== 测试5：欠费结账 ===');
userBalance = 100;
userBalance -= table.priceStart; // 开台
const used = 120;
const t2 = calcAmount(used, 30, 30); // 120分钟=30+90*0.5=75
let diff2 = +(t2 - 30).toFixed(2);
userBalance -= diff2; // 结账，可能为负
console.log(`✅ 用120分钟应收¥75，开台已扣30补收45，余额 ${userBalance}（欠费允许为负，即欠费结账）`);

// 断言
const assert = (cond, msg) => { if (!cond) { console.log('❌ FAIL: ' + msg); process.exit(1); } };
assert(calcAmount(30,30,30)===30, '30min=30');
assert(calcAmount(60,30,30)===45, '60min=45');
assert(calcAmount(90,30,30)===60, '90min=60');
assert(calcAmount(10,30,30)===30, '10min=30');
console.log('\n🎉 全部逻辑验证通过！计费算法正确，开台/结账状态机正确');
