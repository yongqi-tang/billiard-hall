# 自主台球厅系统（基于微信小程序）

一个完整的无人自助台球厅系统：**用户扫码开台 → 自动亮灯 → 自助计时 → 自助结账 → 自动灭灯**，含微信小程序用户端、Spring Boot 后端、管理后台。

## 三端结构

```
billiard-hall/
├── backend/       # Spring Boot 2.7 后端（Java 17 + MyBatis-Plus + MySQL）
├── miniprogram/   # 微信小程序用户端（原生，5 个页面）
├── admin-web/     # 管理后台（单 HTML + 本地 Vue3，可嵌入后端静态资源）
└── test/          # 逻辑测试 + 端到端测试 + 真实后端集成测试（Node 可跑）
```

## 已实现功能

### 用户端（小程序）
- 微信登录（code2session，开发模式自动 mock openid）
- 首页球台状态看板（空闲/使用中/维修/预约）
- 扫码开台（wx.scanCode 扫描球台二维码 `table://ID`）
- 进行中球局：**实时倒计时**、预估金额、余额不足告警
- 续时、结账（自动结算差额）、取消（退回费用）
- 我的页面：余额展示、模拟充值（50/100/200）、订单历史

### 后端（Spring Boot）
- 统一返回 `Result<T>`、全局异常、CORS、MyBatis-Plus 分页
- 登录/充值/球台/订单/设备/管理后台全套 RESTful 接口
- **计费规则可配置**（application.yml：起步价/起步分钟/每分钟单价/欠费时限）
- 开台亮灯、结账灭灯（设备抽象为 `DeviceService` 接口，mock 实现可换真实硬件）
- 支付抽象（`PayService` 接口：充值/扣款/退款，mock 实现可换微信支付）
- 定时任务：欠费超时 30 分钟自动强制结账灭灯
- 内存计时兜底（Redis 未装也能跑，接口透明）

### 管理后台（admin-web/index.html，已内嵌后端 /admin/index.html）
- 营收统计（累计/今日/订单数/进行中）
- 球台管理（增删）
- 订单管理、用户管理（后台充值）
- Vue3 已下载到本地（vue.global.prod.js），内网无 CDN 也能用

## 测试（test/）
```bash
node test/billing-logic.test.js     # 计费算法边界验证
node test/e2e-flow.test.js          # 模拟后端端到端流程验证
node test/api-integration.test.js   # 真实后端集成测试（需先启动后端，16 项断言）
```
前两条为逻辑验证；`api-integration.test.js` 是真实验证——**后端必须已启动**（见下方运行步骤）。

## 运行步骤（本机已实测跑通 ✅）

本机环境：JDK 17 + Maven 3.9.16（放 `C:\Users\10360435\tools`，走内网代理下载依赖）+ MySQL 8（root 密码 0513）+ 后端 jar 已打包。

1. **数据库**：MySQL 执行 `backend/sql/schema.sql`（先 `CREATE DATABASE billiard`，含测试球台/设备数据）
2. **后端**（二选一）：
   - 直接跑已打包 jar：`java -jar backend/target/billiard-hall-backend-1.0.0.jar`（端口 8080）
   - 或改代码后重新打包：`mvn -f backend/pom.xml package -DskipTests` 再 `java -jar`
   - 启动后访问 `http://localhost:8080/api/table/list` 验证
3. **管理后台**：浏览器打开 `http://localhost:8080/admin/index.html`（账号 admin/admin123）——已内嵌在后端，无需单独起服务
4. **小程序**：微信开发者工具导入 `miniprogram/`，填 AppID，勾选"不校验合法域名"，后端启动后首页即显示球台

> 💡 本机无外网，Maven 依赖通过内网代理 `proxyxa.zte.com.cn:80` 下载（配置在 `~/.m2/settings.xml`）。

## 上线说明
- 真实微信支付需企业主体，毕设用"模拟支付/余额制"（`/api/user/recharge`）
- 真机需 https 域名并到小程序后台配置服务器域名白名单
- 硬件联动：`DeviceService` 换成涂鸦/机智云/ESP8266 实现即可，业务代码零改动
