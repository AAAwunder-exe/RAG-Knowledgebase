# RAG-Knowledgebase

一个基于**微服务架构**的 AI 知识库管理平台，核心是「知识库管理 + RAG 智能问答」，并附完整的 RBAC 用户权限体系。主要代码位于 [demo0727/](file:///d:/AIfourOeight/demo0727)，根目录另有 [Atest/](file:///d:/AIfourOeight/Atest)（Python RAG 引擎源码）。

### 技术栈
- **前端**：Vue 3 + TypeScript + Vite + Pinia + Element Plus + ECharts（[package.json](file:///d:/AIFourOeight/demo0727/frontend/package.json)）
- **后端**：Spring Boot 3.4 / Spring Cloud 2024 + Spring Cloud Alibaba + Nacos，Java 17，Maven 多模块（[pom.xml](file:///d:/AIFourOeight/demo0727/pom.xml)）
- **基础设施**：MySQL 8（双 schema）、Redis 7、Nginx，全部经 Docker Compose 编排
- **RAG 引擎**：Python + SentenceTransformer / bge 系列 embedding + reranker，对接 LLM（Moonshot Kimi 等，OpenAI 兼容 API）

### 架构链路
`Vue3 → Nginx:80 → Gateway:8080 → auth-service:8081 / kb-service:8082 → rag-service:8001`，配套 Nacos + Redis + MySQL。

### 后端模块（[modules/](file:///d:/AIFourOeight/demo0727/modules)）
- **common** — 公共代码：JWT 工具、统一返回结构、操作日志注解、常量
- **gateway** — 网关：统一 JWT 校验 + `lb://` 负载均衡路由
- **auth-service** — 认证服务：登录/注册/验证码、用户、角色、权限(RBAC)、菜单、系统配置、Dashboard 聚合
- **kb-service** — 知识服务：知识库、文档上传与解析、全文搜索、AI 问答、调用 RAG 引擎

### 关键设计
1. **安全**：网关统一签发/校验 JWT；下游服务基础「信任模型」——不验 JWT，只信任网关注入的 header，并清洗 `X-User-Permissions` 防伪造；方法级 `hasAuthority` 权限控制。
2. **RAG 能力**：支持文本/扫描件 PDF 解析（OCR）、混合检索融合 + rerank、Redis 问答缓存、sessionStorage 持久化对话历史。
3. **部署**：Docker Compose 单机编排 8 个服务，数据卷持久化；LLM / 数据库 / Nacos 密钥全部经 `.env` 注入，不写死。
