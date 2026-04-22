# 🌿 CarbonPulse - 个人低碳生活追踪平台

> 记录日常低碳行为，追踪碳足迹变化，与好友一起践行绿色生活。

## 📸 项目预览

<!-- 上线后替换为真实截图 -->
| 首页 | 社区 | 消息 |
|:---:|:---:|:---:|
| *待添加* | *待添加* | *待添加* |

## 🛠 技术栈

### 后端
- **Java 17** + **Spring Boot 3**
- **MyBatis-Plus** — ORM & 分页
- **Spring WebSocket + STOMP** — 实时消息推送
- **Redis + Redisson** — 缓存 & 分布式锁
- **JWT** — 无状态认证
- **MySQL 8** — 持久化存储

### 前端
- **Vue 3** (Composition API + `<script setup>`)
- **Vite** — 构建工具
- **Element Plus** — UI 组件库
- **Pinia** — 状态管理
- **Vue Router** — 路由
- **ECharts** — 数据可视化
- **STOMP.js** — WebSocket 客户端

## ✨ 核心功能

| 模块 | 功能 | 亮点 |
|------|------|------|
| 🔐 用户系统 | 注册/登录/JWT鉴权 | Token 自动刷新 |
| 🌱 碳足迹追踪 | 行为记录、每日统计、周报 | 多维度碳排放计算 |
| 👥 社交 | 好友系统、帖子、评论、点赞 | 好友动态流 |
| 💬 私信 | 实时聊天、在线状态、已读回执 | WebSocket + STOMP 双向通信 |
| 🏆 排行榜 | 周/月/好友排行 | 实时排名更新 |
| 🎯 挑战 | 环保挑战参与与打卡 | 进度追踪 |
| 🔔 通知 | 系统通知推送 | WebSocket 实时通知 |

## 📁 项目结构

```
CarbonPulse/                    # 后端 (Spring Boot)
├── src/main/java/com/carbonpulse/
│   ├── controller/             # REST & WebSocket 控制器
│   ├── service/                # 业务逻辑层
│   │   └── Impl/               # 实现类
│   ├── mapper/                 # MyBatis-Plus Mapper
│   ├── entity/                 # 数据实体
│   ├── config/                 # 配置类 (WebSocket, Redis, Security)
│   └── CarbonPulseApplication.java
├── src/main/resources/
│   ├── application.yml         # 应用配置 (需自行创建)
│   ├── application-example.yml # 配置模板
│   └── mappers/                # MyBatis XML
└── sql/
    └── carbonpulse.sql         # 数据库建表脚本

carbonpulse-frontend/           # 前端 (Vue 3)
├── src/
│   ├── views/                  # 页面组件
│   ├── components/             # 通用组件
│   ├── services/               # API & WebSocket 服务
│   ├── stores/                 # Pinia 状态仓库
│   ├── router/                 # 路由配置
│   └── layout/                 # 布局组件
├── package.json
└── vite.config.js
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+

### 后端启动

```bash
# 1. 克隆项目
git clone https://github.com/你的用户名/CarbonPulse.git

# 2. 创建数据库
mysql -u root -p < sql/carbonpulse.sql

# 3. 配置应用
cp src/main/resources/application-example.yml src/main/resources/application.yml
# 编辑 application.yml，填入你的数据库和 Redis 连接信息

# 4. 启动
./mvnw spring-boot:run
```

### 前端启动

```bash
# 1. 进入前端目录
cd carbonpulse-frontend

# 2. 安装依赖
npm install

# 3. 启动开发服务器
npm run dev
```

## 📡 API 概览

| 模块 | 接口 | 方法 |
|------|------|------|
| 用户 | `/api/user/register` | POST |
| 用户 | `/api/user/login` | POST |
| 碳足迹 | `/api/behavior/record` | POST |
| 碳足迹 | `/api/behavior/dashboard` | GET |
| 社区 | `/api/post/publish` | POST |
| 社区 | `/api/post/list` | GET |
| 好友 | `/api/friend/add` | POST |
| 好友 | `/api/friend/list` | GET |
| 消息 | `/api/message/conversations` | GET |
| 消息 | `/api/message/history/{userId}` | GET |
| 排行 | `/api/rank/global/week` | GET |
| WebSocket | `/ws` | STOMP |

> 完整 API 文档：启动后端后访问 Swagger UI

## 📄 License

MIT
