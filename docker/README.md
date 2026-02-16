# Skyway-Vue Docker 部署

## 敏感配置（勿提交）

以下文件含密码等敏感信息，已加入 `.gitignore`，**请勿提交**：

- `docker/volumes/skyway/config/application-prod.yml`
- `docker/.env`
- `skyway-admin/src/main/resources/application-druid.yml`（本地 IDE 运行用）

首次部署或新克隆后：复制对应 `.example` 为上述文件名并填写实际值。本地已有这些文件则无需改动，可照常运行。

## 部署流程（按顺序）

以下在**项目根目录** `Skyway-Vue` 下操作，除非注明在 `docker` 目录。

### 1. 构建前端

```bash
cd skyway-ui && npm install && npm run build:prod && cd ..
cd skyway-c-ui && npm install && npm run build && cd ..
```

将 `skyway-ui/dist/` 和 `skyway-c-ui/dist/` 的**全部内容**分别复制到 `docker/volumes/nginx/admin/` 和 `docker/volumes/nginx/c/`。C 端构建时 `base` 需为 `/`。

### 2. 后端配置

在 **docker** 目录下：

```bash
cd docker
cp volumes/skyway/config/application-prod.yml.example volumes/skyway/config/application-prod.yml
```

编辑 `volumes/skyway/config/application-prod.yml`，确保 Redis、MySQL 与 `.env` 一致。

### 3. 环境变量

在 **docker** 目录下：

```bash
cp .env.example .env
```

按需修改 `.env`（数据库、Redis、端口 8081/8082）。

### 4. 数据库初始化（首次必做）

将项目 `sql/` 下脚本复制到 `docker/volumes/database/mysql/init/`，保证库名与 `.env` 中 `MYSQL_DATABASE` 一致。

### 5. 启动

在 **docker** 目录下：

```bash
docker compose up -d
```

**skyway-admin** 使用阿里云镜像 `crpi-kf13onfj0v8f6jax.cn-shanghai.personal.cr.aliyuncs.com/ming121/skyway-admin:latest`，无需本地构建。首次需先 `docker login` 该仓库（若需拉取权限）。

### 6. 访问

- 管理端：`http://宿主机IP:8081/`
- C 端：`http://宿主机IP:8082/`

---

## 重新构建并推送后端镜像（开发者）

修改后端代码后需重新打镜像并推送，再在服务器上 `docker compose pull skyway-admin && docker compose up -d`。

1. **创建 buildx builder（首次）**  
   `bash docker/buildx-create-builder.sh`

2. **构建并推送到阿里云**  
   复制 `docker/.registry.env.example` 为 `docker/.registry.env`，填入仓库用户名与密码，然后：

   ```bash
   bash docker/buildx-build.sh
   ```

   推送地址：`crpi-kf13onfj0v8f6jax.cn-shanghai.personal.cr.aliyuncs.com/ming121/skyway-admin:latest`（可通过环境变量 `DOCKER_NAMESPACE`、`DOCKER_REGISTRY` 覆盖）。

---

## 端口与说明

- 管理端 8081、C 端 8082，可在 `.env` 中改 `EXPOSE_NGINX_PORT` / `EXPOSE_NGINX_C_PORT`。
- SSL 在宿主机配置，容器仅 HTTP。
