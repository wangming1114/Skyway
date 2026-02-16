<p align="center">
  <img src="skyway-ui/public/favicon.svg" alt="Skyway" width="48" style="vertical-align: middle;">
  <span style="font-size: 1.85em; font-weight: bold; margin-left: 10px; vertical-align: middle; letter-spacing: -0.02em;">Skyway</span>
</p>
<p align="center">生产级代理节点管控与一键部署平台</p>

<p align="center">
  <img src="https://img.shields.io/badge/Skyway-v3.9.1-brightgreen.svg" alt="Skyway">
  <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License">
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker" alt="Docker">
  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=springboot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3-4FC08D?logo=vue.js" alt="Vue">
</p>

---

## 简介

Skyway 深度整合 [233boy/sing-box](https://github.com/233boy/sing-box) 的一键安装与管理能力，提供**全自动节点管理、用户体系与节点流量监控**的管控平台，面向自建代理与节点运维场景。从一键部署到日常运维，用最少操作完成节点与用户的统一管控。

## 快速开始

> 环境要求：已安装 [Docker](https://docs.docker.com/get-docker/) 与 [Docker Compose](https://docs.docker.com/compose/install/)。建议配置：CPU ≥ 2 核，内存 ≥ 4 GiB。

在项目根目录下执行：

```bash
cd docker
cp .env.example .env
# 按需编辑 .env（数据库、Redis、端口等）
docker compose up -d
```

启动后访问：

- **管理端**：`http://宿主机IP:8081`
- **C 端**：`http://宿主机IP:8082`

首次部署需完成前端构建、数据库初始化等步骤，详见 **[docker/README.md](docker/README.md)**。

## 核心特性

**1. sing-box 一键整合**  
深度整合 [233boy/sing-box](https://github.com/233boy/sing-box)，支持 VLESS-REALITY、TUIC、Trojan、Hysteria2、Shadowsocks 2022、VMess 等主流协议；一键安装与配置、自动化 TLS，极简运维，零学习成本。

**2. 全自动节点管理**  
节点生命周期管理、批量部署、状态监控与自动修复；多节点、多协议统一管控，一处配置，处处生效。

**3. 用户与权限体系**  
用户、角色与细粒度权限；多终端认证与动态菜单，适配团队协作与多租户场景。

**4. 节点流量监控与管理**  
实时流量统计、用量排行、限额与告警，为计费与运营提供完整数据基础。

**5. 持续演进**  
智能调度与负载均衡、自动化证书与域名管理、开放 API 与 Webhook、审计与合规报表、多区域与多机房容灾等能力将持续增强，打造可扩展的节点管控底座。

## 使用与部署

- **自托管**：通过 Docker Compose 一键部署（见上方「快速开始」）。进阶配置、自定义构建与镜像推送见 [docker/README.md](docker/README.md)。
- **技术栈**：Spring Boot、Vue3、Element Plus、Redis、MySQL；管理端与 C 端分离部署，便于扩展与定制。

## 文档与许可

- **部署文档**：[docker/README.md](docker/README.md)
- **反馈与讨论**：欢迎通过 [GitHub Issues](https://github.com/skyway/Skyway-Vue/issues) 提交问题与建议

本项目采用 [MIT](LICENSE) 许可证。
