# 自主台球厅小程序

微信小程序原生实现。用**微信开发者工具**打开本目录（`d:\billiard-hall\miniprogram`）即可运行。

## 使用步骤
1. 打开微信开发者工具 → 导入项目 → 选择本目录
2. 在 `project.config.json` 里填入你的 AppID（测试可用测试号）
3. 本地联调后端时，在开发者工具「详情 → 本地设置」勾选 **不校验合法域名**（否则 http://localhost 会被拦截）
4. 确认后端已启动（`d:\billiard-hall\backend`），首页即可显示球台列表

## 目录结构
```
miniprogram/
├── app.js / app.json / app.wxss   # 全局入口、路由、样式（app.js 已打通登录）
├── config.js                      # 后端 baseUrl 配置
├── utils/
│   ├── request.js                 # 请求封装（自动带 token、统一错误提示）
│   └── api.js                     # 接口统一登记
└── pages/
    ├── index/                     # 首页：球台列表 + 扫码入口
    ├── scan/                      # 扫码开台页（扫码后调 /api/order/open）
    ├── playing/                   # 进行中球局：实时倒计时/续时/结账/取消
    ├── orders/                    # 我的订单历史
    └── mine/                      # 我的：余额/模拟充值/功能入口
```

## 当前进度
- [x] 项目骨架 + 全局配置
- [x] 首页球台列表（已对接后端 /api/table/list）
- [x] 扫码开台页（wx.scanCode + 开台接口打通）
- [x] 计时/续时页（实时倒计时 / 续时 / 结账 / 取消）
- [x] 我的页面（订单 / 余额 / 模拟充值）
- [x] 微信登录打通（app.js onLaunch 自动 wx.login → 后端换 openid）

## 联调说明
- 开发者工具本地：`baseUrl = http://localhost:8080`（需勾选不校验域名）
- 真机/上线：`baseUrl` 改成你的 https 正式域名，并到小程序后台配置服务器域名白名单
