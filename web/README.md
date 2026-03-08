# 声读前端 (SoundRead Frontend)

> 声读 - AI 语音合成与内容创作平台前端应用

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4+ | 框架 |
| Vite | 5.4+ | 构建工具 |
| Vue Router | 4.6+ | 路由 |
| Pinia | 2.3+ | 状态管理 |
| Axios | 1.13+ | HTTP 客户端 |
| Tailwind CSS | 4.0+ | 样式 |

## 项目结构

```
frontend/
├── public/               # 静态资源
├── src/
│   ├── api/             # API 接口封装
│   ├── assets/          # 静态资源
│   ├── components/      # 公共组件
│   ├── composables/     # 组合式 API
│   ├── router/          # 路由配置
│   ├── stores/          # Pinia 状态管理
│   ├── views/           # 页面视图
│   ├── App.vue          # 根组件
│   ├── main.js          # 入口文件
│   └── style.css        # 全局样式
├── index.html           # HTML 模板
├── vite.config.js       # Vite 配置
└── package.json         # 依赖配置
```

## 快速开始

### 安装依赖

```bash
cd frontend
pnpm install
```

### 开发模式

```bash
pnpm dev
```

前端启动后访问 http://localhost:5173

### 构建生产版本

```bash
pnpm build
```

构建产物输出到 `dist/` 目录

### 预览生产版本

```bash
pnpm preview
```

## 页面路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | Home | 首页 |
| `/login` | Login | 登录/注册 |
| `/discover` | Discover | 发现页 |
| `/podcast` | Podcast | AI 播客 |
| `/emotion` | Emotion | 情感合成 |
| `/clone` | Clone | 声音克隆 |
| `/vip` | VIP | 会员中心 |
| `/ai-director` | AiDirector | AI 导演 |

## API 代理

开发环境通过 Vite 代理连接后端:

```javascript
// vite.config.js
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
})
```

## 开发指南

### 新增页面

1. 在 `src/views/` 创建页面组件
2. 在 `src/router/index.js` 添加路由
3. 在 `src/api/` 添加对应的 API 接口

### 新增组件

1. 在 `src/components/` 创建组件
2. 在页面中导入使用

### 状态管理

使用 Pinia 管理全局状态:

```javascript
// stores/user.js
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null,
  }),
  actions: {
    setToken(token) {
      this.token = token
    },
  },
})
```

## 相关文档

- [环境搭建](../docs/setup.md)
- [贡献指南](../docs/contribution.md)
- [部署文档](../docs/deployment.md)
- [后端 API](../backend/README.md)
