# VLESS-WS-TLS 托管节点

每个节点有独立的子域名、UUID、WebSocket 路径和本机监听端口。同一台 VPS 上的节点可以共享公网 TCP 443。新节点的 Cloudflare 记录默认使用「仅 DNS」。

## 安装和参数

1. 已有数据库先备份，再执行一次 `sql/2026-11-20-vless-ws-tls.sql`，随后更新管理端及前端。新装 SQL 和 Docker 初始化 SQL 已包含这些表和参数。
2. 进入「系统管理 → 参数设置」，确认 `skyway.cloudflare.domain` 为 `skyway666.sbs`，录入 `skyway.cloudflare.zoneId` 和 `skyway.cloudflare.apiToken`。令牌需要本区域的 `Zone → Zone → Read` 和 `Zone → DNS → Edit`。轮换时在同一项填写新令牌。系统用 AES-GCM 加密存储，页面和导出只显示掩码。[Cloudflare API Token 权限](https://developers.cloudflare.com/fundamentals/api/reference/permissions/)
3. Docker 镜像内置 lego v4.26.0，Compose 将 `docker/volumes/skyway/private` 挂载为 `/app/private`。IDE 和独立 JAR 运行时优先使用管理进程 PATH 中的 lego。缺少时，首次创建或更换域名会自动下载对应平台的官方 v4.26.0 发行包，校验固定的 SHA-256，用 `tar` 解包并缓存到私有目录的 `tools/`。离线部署可以通过 `skyway.ws.lego` 指定已安装工具的绝对路径。`skyway.ws.state-dir` 默认是运行用户的 `~/.skyway/private`。将这个私有目录和数据库一起备份，它包含令牌加密密钥和 ACME 账户。多实例部署需共享这个持久目录。[lego v4.26.0 官方发行包](https://github.com/go-acme/lego/releases/tag/v4.26.0)
4. VPS 使用 Skyway 已支持的 233boy sing-box 环境，并提供 OpenSSL、curl、iproute2 和 iptables 或 nftables。管理端经 SSH 在源站生成私钥和 CSR，并用 Cloudflare DNS-01 申请证书。源站私钥保存在 `/etc/caddy/skyway-certs/`。默认在提交 TXT 后等待 30 秒，由 Let's Encrypt 从公网验证并签发公信证书。通过 `skyway.ws.acme-propagation-wait-seconds` 可以调整等待时间，设为 `0` 时由 lego 在管理机上先检查传播。[233boy 的配置生成逻辑](https://github.com/233boy/sing-box/blob/main/src/core.sh) · [Let's Encrypt DNS-01](https://letsencrypt.org/docs/challenge-types/#dns-01-challenge)
5. 公网开放选定的 TCP 端口。源站已有 Caddy 时，Skyway 向 Caddyfile 追加 `/etc/caddy/skyway/*.conf` 导入，每个域名使用自己的配置文件。新 VPS 会安装 Caddy。橙云使用 Cloudflare 支持的 HTTPS 端口 `443`、`2053`、`2083`、`2087`、`2096` 或 `8443`，Cloudflare 的 SSL/TLS 模式应允许 HTTPS 回源，推荐 `Full (strict)`。[Cloudflare 代理端口](https://developers.cloudflare.com/fundamentals/reference/network-ports/) · [Full (strict)](https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/full-strict/)

## 管理与分享

- 在 VPS 详情、客户详情或节点列表选择 `VLESS-WS-TLS`。子域名前缀留空会自动分配，也可以在单条或批量表单逐行填写。提交后会显示 DNS、证书和握手进度。自填重名时可换名重试，已成功的批次行可以继续使用。
- 在节点编辑页切换「Cloudflare 代理」或修改子域名和公网端口。开启橙云后管理端仍会续期源站证书。换域名时，系统验证新证书和 WebSocket，再更新分享配置和清理旧记录。DNS 缓存按上游 TTL 自行过期。
- 管理员和客户都可以查看 WS Host、路径和 TLS 参数，复制原始 VLESS 链接、扫描二维码、下载 YAML 或复制固定 Clash/Mihomo 订阅。刷新原订阅地址即可获得新域名和端口。直接导入 `vless://` 的客户端需要重新导入链接。独立部署的前端网关须将 `/share/subscription/` 原路径转给 Skyway 后端。仓库自带的 Nginx 和 Vite 已配置转发。
- 访问策略、SOCKS5 中转、访问日志、累计流量、实时速率与限速均按节点分别管理。停用或到期时保留 DNS，恢复后沿用原地址。节点转交其他客户会换发订阅令牌。
- 普通删除回收源站入站、Caddy 站点、证书、计数规则以及托管 DNS。强制删除支持 VPS 离线，清理平台数据和托管 DNS，VPS 上残留的文件由服务器管理员处理。系统按区域、记录 ID 和归属标记确认删除，遇到 Cloudflare 暂时失败时保留记录和「待清理」状态，后台重试，重启后也会继续。

## 复验

`bin/test-vless-ws-local.sh` 首先在空白 Alpine 容器里从 Caddy 官方发行页下载安装包，核对 SHA-512，并通过实际 HTTPS 请求验证 OpenRC 服务。随后在隔离的 DNS 和 CA 环境中，使用真实 sing-box、Caddy、Mihomo 和 lego 测试双节点共享端口、DNS-01、续期、限速和订阅热更新。常规 Java 回归运行 `mvn -pl skyway-admin -am test`。

公网测试入口是 `WsPublicIntegrationTest`。准备权限为 `0600` 的 JSON 文件，字段为 `host`、`port`、`username`、`password`、`zone` 和 `token`。`SKYWAY_WS_LIVE_CONFIG` 指向此文件，`SKYWAY_WS_LIVE_LEGO` 和 `SKYWAY_WS_LIVE_MIHOMO` 指向本机的可执行文件。受限网络可另设 `SKYWAY_WS_ACME_PROPAGATION_WAIT_SECONDS=30`。

```sh
mvn -pl skyway-admin -am test -Dtest=WsPublicIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

该测试在指定 VPS 上使用新生成的 `qa-` 子域名和独立测试数据库，访问真实 Cloudflare 和 Let's Encrypt，结束时按创建时的记录 ID 核验删除结果。

同机复验旧协议可运行 `Hysteria2VpsLiveIntegrationTest#legacyProtocolsShareEditAndTrafficWithWsDeployment`。将 `SKYWAY_WS_LEGACY_CONFIG` 指向同一份测试凭据文件，将 `SKYWAY_WS_LEGACY_MIHOMO_GZ` 指向与 VPS 架构匹配并已校验的官方 Linux Mihomo gzip 文件。测试通过已有控制器创建 Hysteria2 和 VLESS-REALITY，修改端口、读取分享配置、双向传输数据，最后清理测试节点并核对原入站的文件摘要。

## 2026 年 11 月 20 至 21 日验收记录

| 场景 | 实测结果 |
| --- | --- |
| 真实公网直连 | 指定 IPv4 VPS 上两个节点共用 443，各自通过证书校验和 WebSocket 升级。两个 Mihomo 客户端同时各上传、下载 1 MiB，内核计数和访问日志分别命中各自的节点。 |
| 橙云与续期 | Cloudflare API 确认 `proxied=true`，客户端经带有 `CF-Ray` 的边缘节点收发数据。管理端执行续期并部署指纹已变化的公信证书，客户端继续连接。 |
| 换域与改端口 | 新子域名使用 8443。管理端验证公网握手后才切换分享配置。节点 ID、UUID 和订阅地址保留，运行中的 Mihomo 刷新原订阅后完成上传和下载。 |
| DNS 清理 | 两轮公网验收共 6 条 A 记录按各自的 Cloudflare ID 查询均返回 404。对本次尝试涉及的主机名及 DNS-01 名称再次查询，剩余记录为 0。 |
| 同机旧协议 | Hysteria2 和 VLESS-REALITY 的创建、分享、端口修改、双向流量与删除通过真实 Mihomo 验证。测试后核对了原入站配置的 SHA-256。 |
| 隔离与故障回归 | 真实 Linux 网络栈验证了节点独立实时速率、两个方向的限速及恢复。自动化测试覆盖 IPv6 地址同步、重名、Cloudflare 超时、证书失败、后台重启恢复、离线强删和批量部分失败。 |
| 安装和升级 | MySQL 5.7 分别执行存量升级、全新安装和 Docker 初始化，核对了字段、索引、表及参数。 |

最终检查时，测试 VPS 的 sing-box 和 Caddy 均为 `active`，原有 16 个节点配置仍在。临时 Caddy 站点、节点证书目录、限速文件和测试辅助进程均已清理。管理端 JAR 成功打包，前端 88 项测试全部通过，生产构建成功。

11 月 21 日又通过真实管理端、MySQL、Redis 和 Chrome 进行了全量复验，覆盖公网订阅、故障恢复和旧协议回归。修复项、最终测试数和清理核对见[本轮验收报告](vless-ws-tls-regression-2026-11-21.md)。
