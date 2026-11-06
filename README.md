# 台球厅预约管理系统

> 基于微信小程序的无人自助台球厅解决方案：**扫码开台 → 自动亮灯 → 自助计时 → 自助结账 → 自动灭灯**，三端一体（小程序用户端 + Spring Boot 后端 + 运营管理后台）。

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-brightgreen)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5-blue)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1)
![微信小程序](https://img.shields.io/badge/微信小程序-原生-07C160)
![Vue](https://img.shields.io/badge/Vue-3-4FC08D)

---

## ✨ 项目亮点

- **全栈闭环**：小程序用户端 + 后端 + 管理后台三端一体，覆盖"开台 - 计时 - 结账 - 运营"完整业务链路
- **扫码即玩**：扫描球台二维码自动开台、亮灯、计时，无人值守
- **智能计费**：起步价 + 按分钟计费，单价 / 起步时长 / 欠费时限全部可配置
- **无人值守**：定时任务对欠费超时订单自动强制结账灭灯
- **硬件 / 支付解耦**：设备控制与支付均为接口抽象，mock 实现可无缝替换真实硬件与微信支付

## 🛠 技术栈

| 端 | 技术 |
|----|------|
| 后端 | Spring Boot 2.7 · MyBatis-Plus · MySQL 8 · Redis（可选）· Java 17 |
| 用户端 | 微信小程序（原生） |
| 管理后台 | Vue 3（本地引入，离线可用） |
| 测试 | Node.js（计费算法 / 端到端 / 接口集成） |

## 📱 功能特性

### 用户端（小程序）
- 微信登录（开发模式自动 mock openid）
- 首页球台状态看板：空闲 / 使用中 / 维修 / 已预约
- **在线预约球台**：选球台 → 选日期（未来 7 天）→ 选时间段（半小时粒度），同球台同时段冲突自动拦截
- 我的预约：待使用 / 已开台 / 已完成 / 已取消 / 已释放，待使用可取消
- 扫码开台（`wx.scanCode` 扫描球台二维码，预约用户自动核销）
- 进行中球局：实时倒计时、预估金额、余额不足告警
- 续时 / 结账（自动结算差额）/ 取消（退回费用）
- 余额充值（50 / 100 / 200）、订单历史

### 管理后台
- 营收统计：累计营收、今日营收、订单数、进行中订单、待使用预约
- 球台管理（增删改查，展示待预约场次）
- **预约管理**：查看全部预约，可取消待使用预约
- 订单管理、用户管理（后台充值）

### 后端能力
- 统一返回 `Result<T>`、全局异常处理、CORS、MyBatis-Plus 分页
- **预约服务**：时段冲突检测、预约核销（开台自动衔接）、超时/过期自动释放（定时任务）
- 计费规则可配置：起步价 / 起步分钟 / 每分钟单价 / 欠费时限
- 设备抽象 `DeviceService`：mock 实现，可替换为涂鸦 / 机智云 / ESP8266 灯控
- 支付抽象 `PayService`：充值 / 扣款 / 退款，mock 实现可替换微信支付
- 定时任务：欠费超时自动强制结账灭灯、预约超时释放
- 内存计时兜底：未装 Redis 也能运行

## 📂 项目结构

```
billiard-hall/
├── backend/            # Spring Boot 后端
│   ├── src/main/java/com/billiard/hall/
│   │   ├── controller/ # REST 接口层
│   │   ├── service/    # 业务逻辑（开台/结账/计费）
│   │   ├── mapper/     # 数据访问层
│   │   ├── entity/     # 实体
│   │   ├── config/     # MyBatis-Plus / CORS / 计费与微信配置
│   │   ├── device/     # 灯光设备抽象（mock + 可扩展）
│   │   ├── pay/        # 支付抽象（mock + 可扩展）
│   │   └── task/       # 定时任务（欠费结算、预约释放）
│   ├── sql/schema.sql  # 建表脚本（用户/球台/订单/预约/流水/设备）+ 测试数据
│   └── pom.xml
├── miniprogram/        # 微信小程序用户端（7 个页面 + 桌台二维码）
├── admin-web/          # 管理后台（单 HTML + 本地 Vue3）
├── test/               # 自动化测试（Node.js）
└── docs/               # 开题报告、答辩 PPT 等文档
```

## 🚀 快速开始

**环境要求**：JDK 17 · Maven 3.6+ · MySQL 8

```bash
# 1. 克隆
git clone https://github.com/yongqi-tang/billiard-hall.git
cd billiard-hall

# 2. 建库建表（先 CREATE DATABASE billiard，再执行脚本）
mysql -uroot -p < backend/sql/schema.sql

# 3. 修改 backend/src/main/resources/application.yml 中的数据库账号密码

# 4. 打包并启动后端（端口 8080）
mvn -f backend/pom.xml package -DskipTests
java -jar backend/target/billiard-hall-backend-1.0.0.jar
```

- 后端接口验证：`http://localhost:8080/api/table/list`
- 管理后台：`http://localhost:8080/admin/index.html`（账号 `admin` / `admin123`）
- 小程序：用微信开发者工具导入 `miniprogram/`，填写 AppID 并勾选「不校验合法域名」

## ✅ 自动化测试

```bash
node test/billing-logic.test.js         # 计费算法边界验证
node test/e2e-flow.test.js              # 端到端流程验证
node test/reservation-logic.test.js     # 预约逻辑（冲突/取消/核销）验证
node test/api-integration.test.js       # 后端接口集成测试（需先启动后端）
```

## 🗺 后续规划

- [x] 扫码开台 · 自助计时 · 自动结账
- [x] 运营管理后台 · 营收统计
- [x] 计费规则可配置 · 欠费自动结算
- [x] 在线预约球台（时间段预订 · 冲突检测 · 超时释放）
- [x] 前端 UI 美化（渐变 · 动效 · 卡片化）
- [ ] 真实微信支付（需企业主体商户号）
- [ ] 真实硬件联动（ESP8266 / 涂鸦灯控）
- [ ] 小程序真机部署与 HTTPS 域名接入
