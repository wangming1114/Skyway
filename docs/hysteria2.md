# Hysteria2 节点

VPS 详情、客户详情和代理节点列表支持 Hysteria2，单个、批量创建均沿用现有节点操作流程。

## 创建和管理

- 协议类型为 `Hysteria2`，通过 `sb add hy2 <port>` 创建，基线为 sb v1.18 / sing-box 1.13.14。
- 密码与自签证书由 sb 管理，使用 TLS 和 `h3`，无需 SNI。平台不写入 `server_name`，也不修改共享证书。
- 输入配置可以仅包含 `inbounds`。创建时补充仅匹配该节点入站的直连回退，避免合并配置继承其他节点的 SOCKS5 默认出站；中转和域名策略按需叠加。
- 支持中转变更和关闭、域名黑白名单、修改端口、有效期、客户归属、启停及删除。修改失败会恢复配置或清理未完成的创建。
- 分享链接完整包含 `alpn`、`insecure`、`allowInsecure` 和脚本返回的 `pinSHA256`；节点详情可以查看密码及证书指纹。
- 流量计数使用 UDP 规则，实时速度使用字节计数差值。旧版实时采集脚本会在采集器重新建立 SSH 连接时升级。
- 限速沿用现有按端口的 tc 规则及到期解除机制，修改端口时迁移对应规则。
- 修改端口时再次检查远端 socket、sing-box 配置和 Docker 映射，避免与未纳管端口冲突。

## 分享与订阅

管理端和客户分享页支持 Hysteria2 原始链接、二维码、二维码下载和 Clash/Mihomo 订阅。

Hysteria2 转换服务为 `https://sub-server.v2rayssr.com/sub`，使用
[Custom_Clash.ini](https://raw.githubusercontent.com/Aethersailor/Custom_OpenClash_Rules/main/cfg/Custom_Clash.ini)。
原始链接和模板地址分别编码；固定参数为：

```text
target=clash
insert=false
emoji=true
tfo=false
scv=true
fdn=false
expand=false
sort=false
append_type=false
new_name=true
```

VLESS 的订阅默认方式保持原行为。无需数据库迁移。

## 验证

自动化测试覆盖输出解析、密码编码、IPv6、无 SNI 表单提交、HTTP/WebSocket 创建、失败清理、最小配置、中转与域名策略组合、端口和路由迁移、UDP 计数及日志解析。

```sh
mvn -pl skyway-admin -am test
cd skyway-ui
node --test src/utils/*.test.mjs src/views/resource/vps/*.test.mjs src/views/share/customer/*.test.mjs
npm run build:prod
```

另外使用官方 sing-box 1.13.14 与 Mihomo 1.19.33，在无外网的临时 Linux 容器中完成实际连接验证：

- 仅有入站的初始配置。
- SOCKS5 中转。
- 白名单放行与阻断。
- 黑名单放行与阻断。
- SOCKS5 中转与白名单组合。
- 关闭中转后保持域名策略。
- 修改端口后保持中转、TLS 与域名策略。

使用虚构节点联网核对转换服务返回的密码、ALPN、证书指纹和跳过证书校验字段。iptables/nft 两个速度采集分支均在隔离 Linux 环境验证了计数增长、重置与非负速率。验证过程未修改线上 VPS。

后续在用户提供的服务器完成了实际验证与依赖补齐，见 [2026-10-26 真机测试报告](hysteria2-live-test-2026-10-26.md)。
