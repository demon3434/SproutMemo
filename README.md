# SproutMemo (知拾)

<p align="center">
  <img src="icon.png" width="96" height="96" alt="SproutMemo Logo" />
</p>

<p align="center">
  <strong>专为中小学学习设计的私有化自托管错题管理与智能艾宾浩斯复习系统</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker" alt="Docker Compose" />
  <img src="https://img.shields.io/badge/FastAPI-Python_3.11-009688?logo=fastapi" alt="FastAPI" />
  <img src="https://img.shields.io/badge/React-Vite-61DAFB?logo=react" alt="React" />
  <img src="https://img.shields.io/badge/Android-Jetpack_Compose-3DDC84?logo=android" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/PostgreSQL-16_Alpine-336791?logo=postgresql" alt="PostgreSQL" />
</p>

---

## 🌟 核心特性

- 📱 **多端全平台**：Web 管理端（React + Vite）+ Android 客户端（Kotlin + Jetpack Compose 原生支持手机/平板横竖屏与侧滑返回手势）。
- 🧠 **科学复习算法**：基于 SuperMemo-2 / 艾宾浩斯记忆遗忘曲线，每天根据掌握程度智能生成复习题单。
- 📚 **多维度结构化归档**：支持小初高阶段、学期、学科、习题册、章节单元等层级体系，错题条理清晰。
- ✍️ **丰富多媒体记录**：支持错题原图裁剪批注、白板手写答题、语音讲解录制与视频关联。
- 🔍 **局域网 mDNS 秒级自动发现**：Android 客户端与服务端处于同一局域网（Wi-Fi）下无需手动输入 IP，自动发现秒连。
- 💡 **极度轻量，专为低功耗小主机优化**：特别调优 PostgreSQL 内存参数与 Nginx 动静分离，即使在 **1GB 内存** 的老旧低配设备（如玩客云、树莓派、轻量 NAS、入门云服务器）上也能稳定 7×24 小时运行。

---

## 🚀 极速部署指南（Docker 一键运行）

只需一台安装了 Docker 和 Docker Compose 的设备（Windows / macOS / Linux 均可）。

### 第一步：克隆代码仓库

```bash
git clone https://github.com/<你的用户名>/SproutMemo.git
cd SproutMemo
```

### 第二步：生成配置文件

复制模板生成 `.env` 配置文件：

```bash
cp .env.example .env
```

> 💡 **提示**：系统默认对外暴露 **`8080`** 端口，持久化数据存放在当前目录的 `./data` 与 `./pgdata` 中。  
> 若默认配置无需调整，**可直接跳过修改，直接执行下一步**！  
> 如需修改端口或指定挂载磁盘，直接用文本编辑器修改 `.env` 即可。

### 第三步：一键构建并启动

```bash
docker compose up -d --build
```

等待 1~2 分钟容器构建与数据库初始化完成，执行以下命令查看容器状态：

```bash
docker compose ps
```

看到 `sproutmemo-db`、`sproutmemo-backend`、`sproutmemo-web` 状态均为 `running` (或 `healthy`) 即表示启动成功！

### 第四步：访问系统

打开浏览器访问：
👉 **`http://<服务器局域网IP或127.0.0.1>:8080`**

---

## 🔑 默认管理员账号与密码

系统在全新部署拉起后，会自动初始化创建超级管理员账号：

| 配置项 | 默认值 | 说明 |
| :--- | :--- | :--- |
| **登录用户名** | `admin` | 超级管理员账号 |
| **初始密码** | `admin123` | 首次登录初始密码 |
| **预设角色** | `ADMIN` (系统管理员) | 拥有用户管理、学科大纲配置、全库备份与恢复全部权限 |

> ⚠️ **安全提示**：
> 首次成功登录后，请务必及时前往 **「系统设置」→「用户管理」** 修改管理员默认密码。

---

## 📱 Android 客户端配合使用

1. 打开 Android 客户端，确保手机/平板与部署 SproutMemo 的服务器处于**同一个局域网（同一 Wi-Fi）**下。
2. 客户端内置局域网 mDNS 服务发现功能，打开即会自动搜索附近的 SproutMemo 服务；
3. 点击搜索到的服务器节点即可自动完成连接配置；如果网络不支持 mDNS 广播，也可直接在设置中手动输入 `http://<服务器局域网IP>:8080` 连接。

---

## 🛠️ 常用运维管理命令

在项目根目录下执行：

```bash
# 查看所有服务实时日志
docker compose logs -f

# 查看后端接口日志
docker compose logs -f backend

# 重启所有服务
docker compose restart

# 停止并移除容器（数据保留在宿主机，不会丢失）
docker compose down

# 修改代码或更新后重新构建并拉起
docker compose up -d --build
```

---

## 📂 项目模块结构

```text
SproutMemo/
├── sproutmemo-api/          # 后端 API 服务 (Python 3.11 + FastAPI + SQLAlchemy 2.0)
├── sproutmemo-web/          # 前端管理端 (React + Vite + Nginx 动静分离)
├── sproutmemo-android/      # 原生移动端应用源码 (Kotlin + Jetpack Compose)
├── docs/                    # 系统设计规范与技术文档
├── docker-compose.yml       # Docker 编排配置
├── nginx.conf               # Nginx 反向代理配置
├── .env.example             # 环境变量配置模板
└── README.md                # 项目快速入门说明
```

---

## 📖 相关技术文档

- [系统详细部署与运维指南](docs/部署指南.md)
- [局域网 mDNS 自动发现指南](docs/mdns_lan_discovery_guide.md)
- [习题与单元设计规范](docs/习题与单元设计规范.md)
- [图片附件上传设计规范](docs/图片附件上传设计规范.md)
- [每日抽题复习设计与开发规范](docs/每日抽题复习设计与开发规范.md)

---

## 📄 开源许可证

本项目遵循 MIT 协议开源。
