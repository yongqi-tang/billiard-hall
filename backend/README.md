# 自主台球厅系统 - 后端

Spring Boot 2.7 + MyBatis-Plus + MySQL + Redis

## 技术栈
- JDK 17
- Spring Boot 2.7.18
- MyBatis-Plus 3.5.5
- MySQL 8（业务数据）
- Redis（球台计时状态）
- Hutool（工具集）

## 运行步骤
1. 安装 JDK 17、MySQL 8（本机 Maven 3.9.16 已装好，依赖走内网代理下载）
2. 建库建表：执行 `sql/schema.sql`（先 `CREATE DATABASE billiard;`）
3. 修改 `src/main/resources/application.yml` 里的 MySQL 账号密码（**密码含前导零时务必加引号，如 `password: "0513"`，否则 YAML 会解析成数字导致连接被拒**）
4. 打包运行：
   ```
   mvn package -DskipTests
   java -jar target/billiard-hall-backend-1.0.0.jar
   ```
5. 本地访问 `http://localhost:8080/api/table/list` 验证；管理后台在 `http://localhost:8080/admin/index.html`

> 本机已实测：打包成功、连库成功、16 项真实接口集成测试全部通过。

## 目录结构
```
com.billiard.hall
├── common      # 统一返回 Result、全局异常
├── config      # MyBatis-Plus 分页、CORS 跨域、计费/微信配置
├── controller  # 接口层（User/Table/Order/Device/Admin）
├── service     # 业务层（开台/结账/计费核心逻辑）
├── mapper      # 数据访问层
├── entity      # 实体类
├── device      # 灯光设备抽象（mock 实现 + 可扩展涂鸦/ESP8266）
├── pay         # 支付抽象（mock 实现 + 可扩展微信支付）
└── task        # 定时任务（欠费扫描/超时结账）
```

## 当前进度
- [x] 项目骨架（可启动、可连库，已实测）
- [x] 数据库建表脚本 + 测试数据
- [x] 统一返回 / 全局异常 / CORS
- [x] 灯光设备抽象（mock 实现，开台/结账同步 device 表状态）
- [x] 微信登录 code2session（含开发 mock）
- [x] 开台 / 续时 / 结账 / 取消核心业务 + 计费
- [x] 定时任务（欠费超时强制结账）
- [x] 管理后台接口（登录/球台/订单/用户/统计）
- [x] 支付抽象（`PayService` 接口 + mock 实现，接真实微信支付只需新增实现类）
- [x] 管理后台内嵌（static/admin/index.html，本地 Vue3）
- [ ] 真实微信支付（需企业主体商户号，毕设用余额模拟支付即可）
