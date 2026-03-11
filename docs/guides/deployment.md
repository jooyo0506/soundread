# SoundRead 部署运维手册

> 服务器 IP：101.32.128.2 | 后端端口：9090 | Jenkins 端口：8080

---

## 架构概览

```
用户浏览器
    │
    ▼
Cloudflare CDN (www.joyoai.xyz → Cloudflare Pages 静态前端)
             (joyoai.xyz → 服务器 Nginx)
    │
    ▼
Nginx (端口 80) ← server_name: joyoai.xyz
    │
    ├── /api/* → Spring Boot (端口 9090)
    └── /ws/*  → Spring Boot WebSocket (端口 9090)
    
Jenkins (端口 8080) → 自动构建 → 部署 /opt/sounds-tts/app.jar
```

---

## 服务器目录结构

| 路径 | 说明 |
|------|------|
| `/opt/sounds-tts/app.jar` | 后端可执行 JAR |
| `/opt/sounds-tts/application-prod.yml` | 生产配置文件（含密钥，不入 Git）|
| `/opt/sounds-tts/app.log` | 后端运行日志 |
| `/opt/sounds-tts/app.pid` | 后端进程 PID |
| `/opt/sounds-tts/logs/` | Logback 日志目录 |
| `/etc/nginx/conf.d/soundread.conf` | Nginx 配置 |

---

## Jenkins Pipeline 配置

> Jenkins Job → 配置 → Pipeline Script（在 Jenkins UI 中配置，不在代码库）

```groovy
pipeline {
    agent any

    environment {
        APP_DIR  = '/opt/sounds-tts'
        APP_PORT = '9090'
    }

    stages {
        stage('拉取代码') {
            steps {
                git branch: 'main',
                    credentialsId: 'github-token',
                    url: 'https://github.com/jooyo0506/soundread.git'
            }
        }

        stage('打包后端') {
            steps {
                dir('server') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('部署') {
            steps {
                sh '''
                    # ── Step1: 先复制新 JAR（避免杀进程时 JAR 被占用）──
                    mkdir -p $APP_DIR
                    cp server/target/*.jar $APP_DIR/app-new.jar

                    # ── Step2: 优雅关闭旧进程 ──
                    if [ -f $APP_DIR/app.pid ]; then
                        OLD_PID=$(cat $APP_DIR/app.pid)
                        if kill -0 $OLD_PID 2>/dev/null; then
                            echo "正在停止旧进程 PID=$OLD_PID ..."
                            kill $OLD_PID || true
                        fi
                    fi
                    # 兜底：确保没有残留 app.jar 进程
                    pkill -f "app.jar" || true

                    # ── Step3: 等端口 9090 完全释放（最多 30 秒）──
                    echo "等待端口 $APP_PORT 释放..."
                    for i in $(seq 1 30); do
                        if ! ss -ltn | grep -q ":$APP_PORT "; then
                            echo "✅ 端口已释放（${i}s）"
                            break
                        fi
                        if [ $i -eq 30 ]; then
                            echo "⚠️ 端口 30s 未释放，强制 fuser kill..."
                            fuser -k ${APP_PORT}/tcp || true
                            sleep 2
                        fi
                        sleep 1
                    done

                    # ── Step4: 替换 JAR ──
                    mv $APP_DIR/app-new.jar $APP_DIR/app.jar
                '''

                sh '''
                    # ── Step5: 启动新进程 ──
                    export JENKINS_NODE_COOKIE=dontKillMe
                    cd $APP_DIR
                    nohup java -Xmx768m -Xms256m -jar app.jar \
                        --spring.profiles.active=prod \
                        > app.log 2>&1 &
                    echo $! > app.pid
                    echo "新进程 PID: $(cat app.pid)"

                    # ── Step6: 健康检查（最多等 60 秒，扫描启动关键词）──
                    echo "等待服务启动..."
                    for i in $(seq 1 60); do
                        if grep -q "Started SoundReadApplication" $APP_DIR/app.log 2>/dev/null; then
                            echo "✅ 服务已就绪（${i}s）"
                            exit 0
                        fi
                        # 如果进程已经挂了直接报错
                        if ! kill -0 $(cat $APP_DIR/app.pid) 2>/dev/null; then
                            echo "❌ 进程已退出，查看日志："
                            tail -30 $APP_DIR/app.log
                            exit 1
                        fi
                        sleep 1
                    done
                    echo "⚠️ 60s 内服务未响应，查看最新日志："
                    tail -30 $APP_DIR/app.log
                '''
            }
        }
    }

    post {
        success { echo '✅ 部署成功!' }
        failure {
            sh 'tail -50 /opt/sounds-tts/app.log || true'
            echo '❌ 部署失败，已打印最新日志'
        }
    }
}
```


---

## Nginx 配置

文件路径：`/etc/nginx/conf.d/soundread.conf`

```nginx
server {
    listen 80;
    server_name joyoai.xyz;

    location /api/ {
        proxy_pass http://127.0.0.1:9090/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header Origin $http_origin;
    }

    location /ws/ {
        proxy_pass http://127.0.0.1:9090/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header Origin $http_origin;
    }
}
```

重载 Nginx：`nginx -t && nginx -s reload`

---

## 前端环境变量（Cloudflare Pages）

> Workers 和 Pages → soundread → 设置 → 环境变量

| 变量 | 值 |
|------|----|
| `VITE_API_BASE_URL` | `https://joyoai.xyz/api` |
| `VITE_WS_BASE_URL` | `wss://joyoai.xyz/ws` |

> ⚠️ 修改环境变量后必须触发**新构建**（不是重试部署），否则 Vite 不会重新编译注入变量。
> 触发方式：`git commit --allow-empty -m "chore: trigger rebuild" && git push origin HEAD`

---

## Cloudflare 配置

| 配置项 | 值 |
|--------|----|
| SSL/TLS 加密模式 | **灵活（Flexible）** |
| `joyoai.xyz` DNS | A 记录 → `101.32.128.2`，已代理（橙云）|
| `api.joyoai.xyz` DNS | A 记录 → `101.32.128.2`，已代理（橙云）|
| `www.joyoai.xyz` DNS | CNAME → `soundread.pages.dev`，已代理 |

> ⚠️ 不要设置为「完全」或「完全（严格）」模式，服务器没有安装 SSL 证书。

---

## 常见问题排查

### 1. 后端启动失败：`Permission denied`（配置文件权限）

```bash
# 症状
java.io.FileNotFoundException: application-prod.yml (Permission denied)

# 修复
chmod 644 /opt/sounds-tts/application-prod.yml
chown jenkins:jenkins /opt/sounds-tts/application-prod.yml
```

### 2. 后端启动失败：日志目录权限

```bash
# 症状
FileNotFoundException: logs/soundread-server-error.log (Permission denied)

# 修复
chown -R jenkins:jenkins /opt/sounds-tts/logs
chmod -R 755 /opt/sounds-tts/logs
```

### 3. Jenkins 构建成功但后端进程不存在

**原因**：Jenkins 构建完成时会杀掉所有子进程，`nohup` 启动的进程也被杀死。

**修复**：在 Pipeline 的 `sh` 块中加 `export JENKINS_NODE_COOKIE=dontKillMe`（见上方 Pipeline 配置）。

### 4. 前端 CORS 错误（405 / No Access-Control-Allow-Origin）

**常见原因及解决**：

| 原因 | 解决方案 |
|------|---------|
| 前端请求地址仍是 `www.joyoai.xyz/api` | 检查 Cloudflare Pages 环境变量，确认 `VITE_API_BASE_URL=https://joyoai.xyz/api` |
| Cloudflare SSL 模式为「完全」触发 521 | 改为「灵活」模式 |
| Nginx 和 Spring Boot 同时返回 CORS 头导致重复 | Nginx 不加 `add_header Access-Control-*`，由 Spring Boot 统一管理 |
| Sa-Token 拦截 OPTIONS 预检请求返回 401 | `SaTokenConfig.java` 中检测到 OPTIONS 方法时直接 `return` |

### 5. 服务器内存爆满卡死（CPU 飙升 / IO 135MB/s）

**原因**：服务器 3.6G 内存不足，Java 无限制使用内存 + MySQL + Jenkins 同时运行触发 swap。

**修复**：
```bash
# 强制杀掉 Java 进程
pkill -9 -f java
sleep 5

# 带内存限制重启
export JENKINS_NODE_COOKIE=dontKillMe
nohup java -Xmx768m -Xms256m -jar /opt/sounds-tts/app.jar \
  --spring.profiles.active=prod > /opt/sounds-tts/app.log 2>&1 &
```

> Pipeline 中也要加 `-Xmx768m -Xms256m` 参数！

### 6. 服务器重启后手动恢复所有服务

```bash
# 1. 启动 MySQL（Docker）
docker start $(docker ps -a -q --filter "ancestor=mysql:8.0")

# 2. 启动 Nginx
systemctl start nginx

# 3. 启动 Jenkins
systemctl start jenkins

# 4. 启动后端（等 Jenkins 启动后也可通过 Jenkins 构建）
export JENKINS_NODE_COOKIE=dontKillMe
nohup java -Xmx768m -Xms256m -jar /opt/sounds-tts/app.jar \
  --spring.profiles.active=prod > /opt/sounds-tts/app.log 2>&1 &
```

### 7. 验证后端是否成功启动

```bash
# 查看进程
ps -ef | grep app.jar | grep -v grep

# 查看日志（找 "Started SoundReadApplication"）
tail -20 /opt/sounds-tts/app.log

# 测试 API
curl http://localhost:9090/api/tts/voices
```

---

## 日常运维命令速查

```bash
# 查看后端日志（实时）
tail -f /opt/sounds-tts/app.log

# 重启后端
pkill -f "app.jar" || true
sleep 3
export JENKINS_NODE_COOKIE=dontKillMe
nohup java -Xmx768m -Xms256m -jar /opt/sounds-tts/app.jar \
  --spring.profiles.active=prod > /opt/sounds-tts/app.log 2>&1 &

# 查看服务器内存
free -h

# 查看各进程内存占用
ps aux --sort=-%mem | head -10

# 重载 Nginx 配置
nginx -t && nginx -s reload

# 查看 Nginx 错误日志
tail -20 /var/log/nginx/error.log
```
