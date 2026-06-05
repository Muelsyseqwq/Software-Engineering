# CI/CD 流水线与分支协作规范

## 1. 文档目的

本文档用于规范 NekoCafe 项目的代码提交、分支协作、自动化测试和自动化部署流程。

项目采用基于 Pull Request 的协作方式，避免成员直接向 `main` 分支提交代码。通过 GitHub Actions 搭建 CI/CD 流水线，在代码合并前自动完成测试与构建，在代码合并到主分支后自动部署到阿里云服务器。

## 2. 总体流程

推荐流程如下：

```text
从 main 创建功能分支
        ↓
本地开发与自测
        ↓
push 到远程功能分支
        ↓
提交 Pull Request
        ↓
GitHub Actions 自动执行 CI 检查
        ↓
组长或负责人 Code Review
        ↓
CI 通过后合并到 main
        ↓
main 分支触发 CD 部署
        ↓
自动部署到阿里云服务器
```

## 3. 分支管理规范

### 3.1 主分支

项目主分支为：

```text
main
```

`main` 分支代表当前稳定版本，不允许普通开发过程直接 push 到 `main`。

`main` 分支上的代码应满足以下要求：

- 后端测试通过。
- 前端类型检查通过。
- 前端生产构建通过。
- 经过 Pull Request 审核。
- 可部署到阿里云服务器。

### 3.2 功能分支

每个功能或修复应从 `main` 创建独立分支。

推荐分支命名：

```text
feature/功能名称
fix/问题名称
hotfix/紧急修复名称
docs/文档名称
ci/流水线名称
```

示例：

```text
feature/reservation-page
feature/staff-order-flow
fix/customer-order-status
docs/cicd-workflow
ci/github-actions-deploy
```

### 3.3 创建分支示例

```bash
git checkout main
git pull origin main
git checkout -b fix/customer-order-status
```

开发完成后：

```bash
git add .
git commit -m "fix: correct customer order status after staff completion"
git push origin fix/customer-order-status
```

然后在 GitHub 上创建 Pull Request：

```text
fix/customer-order-status -> main
```

## 4. Pull Request 规范

### 4.1 什么时候提交 PR

以下情况应提交 Pull Request：

- 新增业务功能。
- 修复缺陷。
- 修改数据库迁移脚本。
- 修改部署或 CI/CD 配置。
- 修改核心接口、权限、认证相关代码。
- 修改项目文档。

### 4.2 PR 内容要求

Pull Request 描述应包含：

```text
1. 本次修改内容
2. 修改原因
3. 影响范围
4. 自测结果
5. 是否涉及数据库、部署、权限或配置变更
```

示例：

```markdown
## 修改内容

- 修复顾客历史订单状态显示错误。
- 接入店员完成订单后的后端状态流转。

## 修改原因

店员完成订单后，顾客历史订单仍显示为“待支付”。

## 影响范围

- 顾客订单页面
- 店员订单履约页面
- 后端店员订单接口

## 自测结果

- 后端 `mvn test` 通过
- 前端 `npm run typecheck` 通过
- 前端 `npm run build` 通过

## 特殊说明

不涉及数据库结构变更。
```

## 5. CI 流水线规范

CI，即 Continuous Integration，持续集成。

在本项目中，CI 的作用是：在代码合并前自动检查项目是否可以正常编译、测试和构建。

### 5.1 CI 触发时机

CI 在以下场景触发：

```text
1. 向 main 分支提交 Pull Request
2. push 到 main 分支
```

### 5.2 CI 检查内容

本项目 CI 至少包含以下检查：

#### 后端检查

工作目录：

```text
NekoCafe/backend
```

执行命令：

```bash
mvn test
```

作用：

- 编译 Spring Boot 后端代码。
- 执行后端自动化测试。
- 检查 Java 代码是否存在编译错误。

#### 前端检查

工作目录：

```text
NekoCafe/frontend
```

执行命令：

```bash
npm install
npm run typecheck
npm run build
```

作用：

- 安装前端依赖。
- 执行 Vue/TypeScript 类型检查。
- 构建生产环境前端资源。

### 5.3 CI 通过条件

只有满足以下条件，Pull Request 才允许合并：

- 后端 `mvn test` 成功。
- 前端 `npm run typecheck` 成功。
- 前端 `npm run build` 成功。
- 代码经过负责人 Review。

## 6. CD 流水线规范

CD，即 Continuous Deployment，持续部署。

在本项目中，CD 的作用是：当代码通过审核并合并到 `main` 后，自动部署到阿里云服务器。

### 6.1 CD 触发时机

CD 只允许在以下场景触发：

```text
push 到 main 分支
```

Pull Request 阶段只执行测试和构建，不执行部署。

GitHub Actions 中应使用类似条件限制部署：

```yaml
if: github.event_name == 'push' && github.ref == 'refs/heads/main'
```

### 6.2 CD 部署内容

部署内容包括：

```text
1. 构建后端 jar 包
2. 构建前端 dist 静态资源
3. 上传 jar 包到阿里云服务器
4. 上传 dist 文件到 Nginx 静态目录
5. 重启 Spring Boot 后端服务
6. 重载或重启 Nginx
```

### 6.3 推荐服务器目录

阿里云服务器推荐目录：

```text
/opt/nekocafe/backend/       后端 jar 存放目录
/var/www/nekocafe/           前端 dist 静态资源目录
/opt/nekocafe/upload/        CI/CD 临时上传目录
```

### 6.4 推荐服务结构

```text
阿里云 ECS
├── Nginx
│   ├── 托管 Vue 前端静态文件
│   └── 将 /api 请求反向代理到后端
│
├── Spring Boot 后端
│   └── 运行在 8080 端口
│
└── MySQL
    └── 使用阿里云 RDS 或服务器本机 MySQL
```

## 7. GitHub Actions 示例配置

建议在仓库中创建：

```text
.github/workflows/ci-cd.yml
```

示例配置如下：

```yaml
name: NekoCafe CI/CD

on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main

jobs:
  backend-test:
    name: Backend Test
    runs-on: ubuntu-latest

    defaults:
      run:
        working-directory: NekoCafe/backend

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
          cache: maven

      - name: Run backend tests
        run: mvn test

  frontend-build:
    name: Frontend Typecheck and Build
    runs-on: ubuntu-latest

    defaults:
      run:
        working-directory: NekoCafe/frontend

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 20

      - name: Install frontend dependencies
        run: npm install

      - name: Run frontend typecheck
        run: npm run typecheck

      - name: Build frontend
        run: npm run build

  deploy:
    name: Deploy to Aliyun ECS
    runs-on: ubuntu-latest
    needs:
      - backend-test
      - frontend-build

    if: github.event_name == 'push' && github.ref == 'refs/heads/main'

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
          cache: maven

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 20

      - name: Build backend jar
        working-directory: NekoCafe/backend
        run: mvn clean package -DskipTests

      - name: Build frontend dist
        working-directory: NekoCafe/frontend
        run: |
          npm install
          npm run build

      - name: Upload backend jar to server
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.ALIYUN_HOST }}
          username: ${{ secrets.ALIYUN_USER }}
          key: ${{ secrets.ALIYUN_SSH_KEY }}
          port: ${{ secrets.ALIYUN_PORT }}
          source: "NekoCafe/backend/target/*.jar"
          target: "/opt/nekocafe/upload"

      - name: Upload frontend dist to server
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.ALIYUN_HOST }}
          username: ${{ secrets.ALIYUN_USER }}
          key: ${{ secrets.ALIYUN_SSH_KEY }}
          port: ${{ secrets.ALIYUN_PORT }}
          source: "NekoCafe/frontend/dist/*"
          target: "/opt/nekocafe/upload/frontend"

      - name: Restart services on server
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.ALIYUN_HOST }}
          username: ${{ secrets.ALIYUN_USER }}
          key: ${{ secrets.ALIYUN_SSH_KEY }}
          port: ${{ secrets.ALIYUN_PORT }}
          script: |
            set -e

            sudo mkdir -p /opt/nekocafe/backend
            sudo mkdir -p /var/www/nekocafe

            sudo cp /opt/nekocafe/upload/NekoCafe/backend/target/*.jar /opt/nekocafe/backend/nekocafe-backend.jar

            sudo rm -rf /var/www/nekocafe/*
            sudo cp -r /opt/nekocafe/upload/frontend/NekoCafe/frontend/dist/* /var/www/nekocafe/

            sudo systemctl restart nekocafe-backend
            sudo systemctl reload nginx

            echo "NekoCafe deployed successfully."
```

## 8. GitHub Secrets 配置规范

服务器地址、SSH 私钥、数据库密码、JWT 密钥等敏感信息不得写入代码仓库。

应在 GitHub 仓库中配置 Secrets：

```text
Settings
  -> Secrets and variables
  -> Actions
  -> New repository secret
```

建议配置：

```text
ALIYUN_HOST       阿里云服务器公网 IP
ALIYUN_USER       SSH 用户名
ALIYUN_PORT       SSH 端口，默认 22
ALIYUN_SSH_KEY    SSH 私钥内容
```

注意事项：

- 不要把 SSH 私钥提交到 Git 仓库。
- 不要把数据库密码写到 README、代码注释或日志中。
- 不要在 Actions 日志中打印密钥或 token。
- 生产环境的 `JWT_SECRET`、数据库密码应通过服务器环境变量或安全配置方式管理。

## 9. main 分支保护建议

为避免成员直接 push 到 `main`，建议在 GitHub 中设置分支保护规则：

```text
Settings
  -> Branches
  -> Branch protection rules
  -> Add rule
```

规则名称：

```text
main
```

建议开启：

- Require a pull request before merging
- Require status checks to pass before merging
- Require branches to be up to date before merging
- Require approvals
- Restrict who can push to matching branches
- Do not allow bypassing the above settings

课程项目中，至少应开启：

```text
1. 必须通过 Pull Request 合并
2. 必须通过 CI 检查
3. 不允许直接 push 到 main
```

## 10. 组内角色建议

### 组长

- 负责管理 `main` 分支。
- 负责审核 Pull Request。
- 负责检查 CI/CD 流水线是否通过。
- 负责管理 GitHub Secrets 和阿里云服务器权限。

### 开发成员

- 从 `main` 创建功能分支。
- 在本地完成开发和自测。
- 提交 Pull Request。
- 根据 Review 意见修改代码。
- 不直接向 `main` 分支提交代码。

### 测试负责人

- 关注 CI 结果。
- 维护测试用例。
- 记录流水线失败原因和修复结果。

## 11. 提交信息规范

推荐使用以下提交类型：

```text
feat: 新功能
fix: 修复问题
docs: 文档修改
style: 代码格式调整
refactor: 重构
test: 测试相关
chore: 构建、依赖、配置等杂项
ci: CI/CD 流水线相关
```

示例：

```bash
git commit -m "feat: add staff order fulfillment workflow"
git commit -m "fix: correct customer order history status"
git commit -m "docs: add ci cd collaboration workflow"
git commit -m "ci: add github actions deployment pipeline"
```

## 12. 课程答辩说明示例

答辩时可以这样说明：

> 本项目采用基于 Pull Request 的分支协作流程。开发成员从 `main` 分支创建功能分支，完成开发后提交 Pull Request。GitHub Actions 会在 PR 阶段自动执行后端 Maven 测试、前端 TypeScript 类型检查和前端构建。只有当 CI 检查通过并经过负责人审核后，代码才允许合并到 `main` 分支。
>
> 当代码合并到 `main` 后，CD 流水线会自动构建后端 jar 包和前端 dist 静态资源，并通过 SSH 上传到阿里云服务器，随后自动重启 Spring Boot 后端服务并重载 Nginx，实现自动化部署。

## 13. 注意事项

- Pull Request 阶段只测试和构建，不部署。
- 只有 `main` 分支更新后才触发部署。
- 不得将服务器私钥、数据库密码、JWT 密钥提交到仓库。
- 部署前应确保阿里云安全组开放必要端口，例如 80、443、后端服务端口或 SSH 端口。
- 若流水线失败，应先修复失败原因，再重新提交，不应绕过 CI 直接合并。
