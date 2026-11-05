# SproutMemo 局域网服务发现技术指南（单端口 mDNS + Avahi 组播反射规范）

本指南详细记录了 SproutMemo（知拾）项目在局域网零配置自动服务发现方面的演进历程、架构拓扑、服务端实现、宿主机 Docker 组播反射配置、Android 客户端标准化对接规范以及核心排障经验，作为全栈协同开发的标准化参考基线。

---

## 1. 架构演进与方案选型

### 1.1 历史方案演进

| 演进阶段 | 发现机制 | 网络拓扑与端口 | 优势 | 缺陷与痛点 |
| :--- | :--- | :--- | :--- | :--- |
| **阶段一（已淘汰）** | **私有 UDP Socket 广播** | Web 业务端口 `8080` + UDP 发现端口 `8090` | 实现直观，无特殊依赖 | 1. 占用双端口，暴露额外攻击面；<br>2. Android 端需申请 `MulticastLock` 与 `CHANGE_WIFI_MULTICAST_STATE` 权限，易被系统省电策略杀死；<br>3. Docker 容器必须显式开放 `8090:8090/udp`；<br>4. 私有报文协议，非通用标准。 |
| **阶段二（现行标准）** | **标准 mDNS (DNS-SD) + Avahi 组播反射** | **单端口**：仅对外暴露 Web 业务端口（如 `8080`），无额外 UDP 端口 | 1. 符合 RFC 6762 / 6763 行业标准；<br>2. Android 原生 `NsdManager` 零权限平滑集成；<br>3. 宿主机 Avahi 跨网桥反射，容器与物理局域网无缝打通；<br>4. 统一单端口部署与管理，彻底根除硬编码。 | - |

---

## 2. 整体架构与网络拓扑

在 Docker 容器化部署场景中，mDNS 组播包（`224.0.0.251:5353`）默认无法跨越 Docker 默认网桥与物理网络。本项目通过**固定网桥名 + Avahi Reflector 组播反射器**实现无缝打通：

```
┌────────────────────────────────────────────────────────────────────────┐
│                         Android 客户端 (App)                            │
│  • 使用 Android 原生 android.net.nsd.NsdManager (零特殊权限)              │
│  • 发现服务类型: "_sproutmemo._tcp"                                    │
│  • 解析 TXT 记录与 A 记录，获取宿主机通告 IP (如 192.168.1.100) 与端口 (8080) │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                  mDNS 组播查询 / 响应 (224.0.0.251:5353)
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 宿主机 (Linux / Debian / Ubuntu / CentOS)                              │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ 宿主机服务: avahi-daemon (开启 enable-reflector=yes)               │  │
│  │ allow-interfaces = <物理上行网卡(如 eth0)>, br-sproutmemo        │  │
│  │ 作用: 自动在物理局域网与 Docker 容器网桥间双向反射 mDNS 报文          │  │
│  │ (禁止在 /etc/avahi/services/ 下编写任何硬编码静态 XML)              │  │
│  └────────────────────────────────▲─────────────────────────────────┘  │
│                                   │                                    │
│  ┌────────────────────────────────▼─────────────────────────────────┐  │
│  │ Docker 专属网桥: br-sproutmemo (sproutmemo_mdns 网络)             │  │
│  └────────────────────────────────▲─────────────────────────────────┘  │
│                                   │                                    │
│  ┌────────────────────────────────┴─────────────────────────────────┐  │
│  │ Docker 业务容器: sproutmemo-backend / sproutmemo-web             │  │
│  │  • Python zeroconf 广播 ServiceInfo ("_sproutmemo._tcp.local.")   │  │
│  │  • 仅暴露单一 Web HTTP 端口: 0.0.0.0:8080 -> 8080                │  │
│  │  • 携带 TXT 属性: host, mappedPort, url, version, path           │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 服务端实现规范

### 3.1 广播器核心代码结构

服务端通过 `app/services/mdns_discovery.py` 中的 `ServiceDiscoveryBroadcaster` 统一管理生命周期：

- **服务类型（Service Type）**：`_sproutmemo._tcp.local.`
- **服务实例名（Instance Name）**：`SproutMemo-Server._sproutmemo._tcp.local.`
- **TXT 扩展属性（Properties）**：
  ```python
  properties = {
      b"host": self.active_host.encode("utf-8"),
      b"mappedPort": str(self.active_port).encode("utf-8"),
      b"url": f"http://{self.active_host}:{self.active_port}".encode("utf-8"),
      b"version": b"1.0",
      b"path": b"/api/v1",
  }
  ```
- **主机别名（Server Domain）**：`sproutmemo-server.local.`

### 3.2 动态重新通告与智能 IP 探测

- `_detect_local_ip()` 函数探测宿主机物理 IP，自动过滤 `127.x` 环回口与 `172.x` Docker 内部私有网段；
- 广播器支持热重载，当系统配置发生变更时可安全重新广播：
  ```python
  broadcaster.start(new_host, new_port)
  ```

---

## 4. 宿主机部署与 Avahi 配置规范

### 4.1 Docker Compose 网络与端口定义

在 `docker-compose.yml` 中，业务容器仅需映射 Web 端口，并挂载到专用 mDNS 网桥网络：

```yaml
version: '3.8'

services:
  backend:
    image: ${BACKEND_IMAGE}
    networks:
      - sproutmemo_net
      - sproutmemo_mdns

  web:
    image: ${WEB_IMAGE}
    ports:
      - "${HOST_PORT}:80" # 仅暴露单一业务端口
    networks:
      - sproutmemo_net

networks:
  sproutmemo_net:
    name: sproutmemo_net
    driver: bridge
  sproutmemo_mdns:
    name: sproutmemo_mdns
    driver: bridge
    driver_opts:
      com.docker.network.bridge.name: br-sproutmemo # 固定网桥名称为 br-sproutmemo
```

### 4.2 宿主机自动化配置脚本

项目提供自动化配置脚本 `scripts/setup_avahi_sproutmemo.sh`。在宿主机上以 `sudo` 权限执行：

```bash
sudo bash scripts/setup_avahi_sproutmemo.sh
```

**该脚本的核心动作**：
1. 安装 `avahi-daemon` 与 `avahi-utils`；
2. 预创建 Docker 网络 `sproutmemo_mdns` 并绑定物理网桥 `br-sproutmemo`；
3. 自动探测宿主机的默认上行物理网卡（如 `eth0`、`enp3s0`）；
4. 配置 `/etc/avahi/avahi-daemon.conf`：
   ```ini
   [server]
   use-ipv4=yes
   use-ipv6=no
   allow-interfaces=eth0,br-sproutmemo

   [reflector]
   enable-reflector=yes
   ```
5. 清理历史遗留的 `/etc/avahi/services/` 静态 XML 文件；
6. 重启并启用 `avahi-daemon` 守护进程。

---

## 5. Android 客户端标准化对接规范

Android 客户端无需申请任何底层 Socket 权限，使用 Android 官方提供的 `android.net.nsd.NsdManager` 进行零配置服务发现。

### 5.1 基础配置与权限

在 `AndroidManifest.xml` 中仅需声明基本网络权限：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

### 5.2 核心解析与防并发逻辑 (`NsdHelper.kt`)

1. **零权限与无锁**：完全不需要申请 `WifiManager.MulticastLock`；
2. **防并发 Resolve 冲突 (`FAILURE_ALREADY_ACTIVE`)**：
   - Android 12 之前 `NsdManager.resolveService()` 为单任务状态机；
   - 通过 `AtomicBoolean` 串行化解析控制，一旦解析成功首个有效服务，立即停止发现并交付 Base URL；
3. **TXT 记录优先读取**：
   - 优先提取 TXT 中的 `host` 与 `mappedPort`，彻底解决容器内 IP 与宿主机外网 IP 映射不一致的问题；
   - 过滤 IPv6 Scope ID（如 `192.168.1.100%eth0` 去除 `%` 后缀）。

---

## 6. 核心排障与避坑指南

### 避坑 1：mDNS 服务类型后缀格式不合规导致无法发现
- **现象**：服务端启动了广播，但 Android 端从不触发 `onServiceFound`。
- **原因**：mDNS / DNS-SD 规范要求 FQDN 必须以点 `.` 结尾（如 `_sproutmemo._tcp.local.`）。未补齐 `.local.` 时会被自动截断。
- **解决方案**：在注册 `ServiceInfo` 前，严格校验并格式化 `service_type`。

### 避坑 2：Docker 容器默认网桥过滤组播报文
- **现象**：宿主机能搜到服务，手机客户端搜不到。
- **原因**：Docker 默认的 `bridge` 网络与物理网卡隔离，不会自动路由组播地址 `224.0.0.251`。
- **解决方案**：使用专用网桥 `br-sproutmemo` 并由宿主机 `avahi-daemon` 开启 `enable-reflector=yes` 双向反射。

### 避坑 3：禁止在宿主机写入静态 Service XML
- **现象**：宿主机 IP 变动后，客户端依然解析出旧 IP。
- **原因**：在 `/etc/avahi/services/` 下写入了硬编码 IP 的静态 XML 文件。
- **解决方案**：完全由容器内 Python Zeroconf 动态广播，宿主机 Avahi 仅作为纯粹的反射器。

---

## 7. 联调测试与验证工具链

### 7.1 Linux 宿主机测试命令

```bash
# 1. 查看当前网络上所有 mDNS 服务通告（需安装 avahi-utils）
avahi-browse -atr | grep -i sproutmemo

# 2. 解析特定 SproutMemo 服务的详细 TXT 记录与 IP:端口
avahi-browse -r _sproutmemo._tcp

# 3. 抓取网桥上的 mDNS 组播包，验证容器是否发出了通告
sudo tcpdump -i br-sproutmemo port 5353 -vv

# 4. 抓取物理网卡上的 mDNS 组播包，验证 Avahi 是否成功反射到了外网
sudo tcpdump -i eth0 port 5353 -vv
```

### 7.2 Android 客户端辅助调试工具

1. **Bonjour Search / Service Browser**：在手机上安装 mDNS 浏览工具，验证局域网内是否存在 `_sproutmemo._tcp` 服务。
2. **Logcat 过滤标签**：
   ```bash
   adb logcat -s NsdHelper NsdManager
   ```
